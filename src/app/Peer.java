package app;

import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.security.MessageDigest;
import java.rmi.registry.Registry;
import protocol.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.BufferedInputStream;
import java.io.IOException;

public class Peer implements BackupService
{
    private static DatagramSocket controlSocket;
    private static DatagramSocket backupSocket;
    private static DatagramSocket restoreSocket;
    private static DatagramSocket deleteSocket;
    private static int mcPort, mdbPort, mdrPort;
    private static String mcAddr, mdbAddr, mdrAddr;
    private static String id;
    private static String accessPoint;
    private static String version;

    public static void main(String args[])
    {
        System.setProperty("java.net.preferIPv4Stack", "true");
        if(args.length >= 3)
        {
            mcAddr = "224.0.0.1";
            mdbAddr = "224.0.0.1";
            mdrAddr = "224.0.0.1";

            mcPort = 5001;
            mdbPort = 5002;
            mdrPort = 5003;

            version = args[0];
            id = args[1];
            accessPoint = args[2];

            createDirectory();
        }
        else
        {
            System.out.println("Wrong number of arguments");
            System.out.println("Usage: Peer <ProtocolVersion> <ServerID> <AccessPoint> [<mcAddr> <mcPort> <mdbAddr> <mdbPort> <mdrAddr> <mdrPort>]");
            return;
        }

        if (args.length == 9)
        {
            mcAddr = args[3];
            mcPort = Integer.parseInt(args[4]);

            mdbAddr = args[5];
            mdbPort = Integer.parseInt(args[6]);

            mdrAddr = args[7];
            mdrPort = Integer.parseInt(args[8]);
        }

        setUpClientInterface();

        try
        {
            controlSocket = new DatagramSocket();
            backupSocket = new DatagramSocket();
            restoreSocket = new DatagramSocket();
            deleteSocket = new DatagramSocket();
        }
        catch(SocketException e)
        {
            System.out.println("Couldn't open communication sockets in peer.");
            return;
        }

        try
        {
            Control control = new Control(mcPort, InetAddress.getByName(mcAddr));
            Backup backup = new Backup(id, version, mcPort, InetAddress.getByName(mcAddr), mdbPort, InetAddress.getByName(mdbAddr));
            Restore restore = new Restore(mcPort, InetAddress.getByName(mcAddr), mdrPort, InetAddress.getByName(mdrAddr));
            Delete delete = new Delete(id, mcPort, InetAddress.getByName(mcAddr), mdbPort, InetAddress.getByName(mdbAddr));

            control.start();
            backup.start();
            restore.start();
            delete.start();

            System.out.println("Started threads");
        }
        catch(Exception e)
        {
            System.out.println("Could't create threads");
        }
    }

    public static void createDirectory()
    {
        new File(id + "/backup").mkdirs();
        new File(id + "/restored").mkdirs();
    }

    public String generateFileId(File file)
    {
        try
        {
            //Get metadata attributes
            BasicFileAttributes attributes = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
            String hashInput = file.getName() + attributes.lastModifiedTime() + attributes.size(); //Create the value to hash
            MessageDigest digest = MessageDigest.getInstance("SHA-256"); //Create hash instance
            byte[] hash = digest.digest(hashInput.getBytes(StandardCharsets.UTF_8)); //Hash
            char[] hexChars = new char[hash.length * 2], hexArray = "0123456789ABCDEF".toCharArray(); //Convert to format

            for(int j = 0; j < hash.length; j++ )
            {
                int v = hash[j] & 0xFF;
                hexChars[j * 2] = hexArray[v >>> 4];
                hexChars[j * 2 + 1] = hexArray[v & 0x0F];
            }

            return new String(hexChars);

        }
        catch(Exception e)
        {
            System.out.println("Couldn't generate file id");
            return null;
        }
    }

    public static void setUpClientInterface()
    {
        try
        {
            Peer obj = new Peer();
            BackupService stub = (BackupService) UnicastRemoteObject.exportObject(obj, 0);

            Registry registry = LocateRegistry.getRegistry();
            registry.rebind(accessPoint, stub);

            System.out.println("Client-Server Interface Set Up");
        }
        catch(Exception e)
        {
            System.out.println("Client-Server exception: " + e.toString());
        }
    }

    public boolean sendPutChunck(String fileId, byte[] chunck, int chuckNo, int replication)
    {
        String msg = "PUTCHUNCK " + version + " " + id + " " + fileId + " " + chuckNo + " " + replication
            + " \r\n\r\n";

        byte[] header = msg.getBytes();
        byte[] putchunck = new byte[header.length + chunck.length];

        System.arraycopy(header, 0, putchunck, 0, header.length);
        System.arraycopy(chunck, 0, putchunck, header.length, chunck.length);

        try
        {
            DatagramPacket packet = new DatagramPacket(putchunck, putchunck.length, InetAddress.getByName(mdbAddr), mdbPort);

            backupSocket.send(packet);
        }
        catch(Exception e)
        {
            System.out.println("Couldn't send putchunck");
        }

        return true;
    }

    public boolean receivePut(int timeout, int replication, String fileId, int chunckNo)
    {
        byte[] buffer = new byte[64100];
        DatagramPacket receivedPacket = new DatagramPacket(buffer, buffer.length);
        int replicationCounter = 0;
        MulticastSocket mcSocket;

        try
        {
            mcSocket = new MulticastSocket(mcPort);

            mcSocket.joinGroup(InetAddress.getByName(mcAddr));
            mcSocket.setTimeToLive(1);
            mcSocket.setSoTimeout(timeout);
        }
        catch(IOException e)
        {
            System.out.println("Couldn't open multicast socket to receive STORED messages");
            return false;
        }

        try
        {
            while(replicationCounter < replication)
            {
                mcSocket.receive(receivedPacket);

                byte[] actualData = new byte[receivedPacket.getLength()];

                System.arraycopy(receivedPacket.getData(), 0, actualData, 0, actualData.length);

                String msg = new String(actualData).trim();
                String[] msgParams = msg.split("\\s+");

                if(msgParams.length == 0)
                {
                    System.out.println("Corrupt message @ peer.receivePut");
                    continue;
                }

                for(int i = 0; i < msgParams.length; i++)
                    msgParams[i] = msgParams[i].trim();

                if(msgParams[0].equals("STORED"))
                {
                    if(msgParams.length < 5)
                    {
                        System.out.println("Invalid STORED message");
                        continue;
                    }

                    String version = msgParams[1], fileIdReceived = msgParams[3], chunckNoReceived = msgParams[4];

                    if(!version.equals(Peer.version) || !fileIdReceived.equals(fileId) 
                        || Integer.parseInt(chunckNoReceived) != chunckNo)
                        continue;
                    else
                        replicationCounter++;
                }
            }
        }
        catch(Exception e)
        {
            if(e instanceof SocketTimeoutException)
                System.out.println("Didn't received required PUT answers to meet replication demands");
            else
                System.out.println("Couldn't received PUT");

            mcSocket.close();
            return false;
        }

        mcSocket.close();
        return true;
    }

    public void backupFile(String path, int replication)
    {
        File file = new File(path);

        if(!file.exists())
        {
            System.out.println("Couldn't find file to backup: " + path);
            return;
        }

        if(replication <= 0)
        {
            System.out.println("Invalid replication degree:" + replication);
            return;
        }

        String fileId = generateFileId(file);
        int fileSize = (int) file.length();
        int partCounter,  nChuncks = (int) Math.ceil((double) fileSize / 64000);
        int responseWaitingTime = 1 * 1000;
        int attemptNo = 1;

        if(fileSize % 64000 == 0)
            nChuncks += 1;

        try(FileInputStream fis = new FileInputStream(file); BufferedInputStream bis = new BufferedInputStream(fis);)
        {
            int bytesRead = 0;

            for(partCounter = 0; partCounter < nChuncks; partCounter++)
            {
                int aux = (int) file.length() - bytesRead, bufferSize;

                if (aux > 64000)
                    bufferSize = 64000; //Maximum chunck size
                else
                    bufferSize = aux;

                byte[] buffer = new byte[bufferSize];

                bytesRead = bis.read(buffer);

                if(!sendPutChunck(fileId, buffer, partCounter, replication))
                    return;

                while(attemptNo <= 5)
                {
                    if(receivePut(responseWaitingTime, replication, fileId, partCounter))
                        break;
                    else
                    {
                        responseWaitingTime *= 2;
                        attemptNo++;
                    }
                }
                
                if(attemptNo > 5)
                    System.out.println("Max attempts to send PUTCHUNCK reached\nChunck not stored with required replication");
            }
        }
        catch(Exception e)
        {
            System.out.println("Couldn't separate file into chuncks");
        }
    }

    public void restoreFile(String path)
    {
      File file = new File(path);

      if(!file.exists())
      {
          System.out.println("Couldn't find file to restore: " + path);
          return;
      }
    }

    public void deleteFile(String path)
    {
      File file = new File(path);

      if(!file.exists())
      {
          System.out.println("Couldn't find file to delete: " + path);
          return;
      }
      else{
        System.out.println("File Deleted");
      }

      String fileId = generateFileId(file);
      int fileSize = (int) file.length();
      int partCounter,  nChuncks = (int) Math.ceil((double) fileSize / 64000);
      int responseWaitingTime = 1; //seconds
      int attemptNo = 1;
      MulticastSocket mcSocket;

      if(fileSize % 64000 == 0)
          nChuncks += 1;

      try
      {
          mcSocket = new MulticastSocket(mcPort);

          mcSocket.joinGroup(InetAddress.getByName(mcAddr));
          mcSocket.setTimeToLive(1);
      }
      catch(IOException e)
      {
          System.out.println("Couldn't open multicast socket to receive STORED messages");
          return;
      }

      try(FileInputStream fis = new FileInputStream(file); BufferedInputStream bis = new BufferedInputStream(fis);)
      {
          int bytesRead = 0;

          for(partCounter = 0; partCounter < nChuncks; partCounter++)
          {
              int aux = (int) file.length() - bytesRead, bufferSize;

              if (aux > 64000)
                  bufferSize = 64000; //Maximum chunck size
              else
                  bufferSize = aux;

              byte[] buffer = new byte[bufferSize];

              bytesRead = bis.read(buffer);

              int replication = -1;

              if(!sendPutChunck(fileId, buffer, partCounter, replication))
                  return;

              /*
              while(attemptNo <= 5)
              {
                  mcSocket.setSoTimeout(responseWaitingTime * 1000);
                  receivePut(mcSocket, replication);
              } */


              //Send PUTCHUNCK on multicast then wait <time> for response on mc channel
          }
      }
      catch(Exception e)
      {
          System.out.println("Couldn't separate file into chuncks");
      }

      mcSocket.close();
    }

    public void manageStorage()
    {
        //TODO Manage storage
    }

    public void retrieveInfo()
    {
        //TODO Get info
    }
}
