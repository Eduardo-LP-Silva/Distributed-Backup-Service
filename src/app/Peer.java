package app;

import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.security.MessageDigest;
import java.rmi.registry.Registry;
import protocol.*;
import protocol.initiator.Backup;
import protocol.listener.MDB;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Set;

public class Peer extends Thread implements BackupService 
{
    protected static DatagramSocket controlSocket;
    protected static DatagramSocket backupSocket;
    protected static DatagramSocket restoreSocket;
    protected static DatagramSocket deleteSocket;
    protected static int mcPort, mdbPort, mdrPort;
    protected static InetAddress mcAddr, mdbAddr, mdrAddr;
    protected static int id;
    protected static String accessPoint;
    protected static String version;
    protected static Hashtable<String, int[]> backedUpChuncks; //fileID-ChunckNo -> {replication_expected, size}
    protected static Hashtable<String, ArrayList<Integer>> chuncksStorage; //fileID-ChunckNo -> {1, 2, ...}

    public static void main(String args[])
    {
        System.setProperty("java.net.preferIPv4Stack", "true");
        if(args.length >= 3)
        {
            try
            {
                mcAddr = InetAddress.getByName("224.0.0.1");
                mdbAddr = InetAddress.getByName("224.0.0.1");
                mdrAddr = InetAddress.getByName("224.0.0.1");
            }
            catch(UnknownHostException e)
            {
                System.out.println("Unknown Host");
                return;
            }
            

            mcPort = 5001;
            mdbPort = 5002;
            mdrPort = 5003;

            version = args[0];
            id = Integer.parseInt(args[1]);
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
            try
            {
                mcAddr = InetAddress.getByName(args[3]);
                mcPort = Integer.parseInt(args[4]);
    
                mdbAddr = InetAddress.getByName(args[5]);
                mdbPort = Integer.parseInt(args[6]);
    
                mdrAddr = InetAddress.getByName(args[7]);
                mdrPort = Integer.parseInt(args[8]);
            }
            catch(UnknownHostException e)
            {
                System.out.println("Unknown Host");
                return;
            }
            
        }

        generateDataBase();
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
            MDB mdbListener = new MDB();
            Control control = new Control(mcPort, mcAddr);
            Restore restore = new Restore(mcPort, mcAddr, mdrPort, mdrAddr);
            Delete delete = new Delete(id, mcPort, mcAddr, mdbPort, mdbAddr);

            mdbListener.start();
            control.start();
            restore.start();
            //delete.start();

            System.out.println("Started threads");
        }
        catch(Exception e)
        {
            System.out.println("Could't create threads");
        }
    }

    public static void saveTableToDisk(int table)
    {
        FileOutputStream fos;

        try
        {
            if(table == 1)
                fos = new FileOutputStream("backedChuncks.ser");
            else
                if(table == 2)
                    fos = new FileOutputStream("chuncksStorage.ser");
                else
                {
                    System.out.println("Invalid table to save to disk");
                    return;
                }

            ObjectOutputStream oos = new ObjectOutputStream(fos);

            oos.writeObject(backedUpChuncks);
            oos.close();
            fos.close();
        }
        catch(IOException e)
        {
            System.out.println("Couldn't save table to disk");
        }
    }

    public static void printLocalChuncksTable()
    {
        Set<String> keys = backedUpChuncks.keySet();

        for(String key: keys)
        {
            int[] values = backedUpChuncks.get(key);

            System.out.print(key + "-> [");
            
            for(int i = 0; i < values.length; i++)
            {
                System.out.print(values[i]);

                if(i != values.length - 1)
                    System.out.print(", ");
            }

            System.out.println("]");
        }    
    }

    public boolean checkVersion(String msgVersion)
    {
        return version.equals(msgVersion);
    }

    public static void printChuncksStorageTable()
    {
        Set<String> keys = chuncksStorage.keySet();

        for(String key: keys)
        {
            ArrayList<Integer> values = chuncksStorage.get(key);

            System.out.print(key + "-> [");
            
            for(int i = 0; i < values.size(); i++)
            {
                System.out.print(values.get(i));

                if(i != values.size() - 1)
                    System.out.print(", ");
            }

            System.out.println("]");
        }    
    }

    public static void createDirectory()
    {
        new File(id + "/backup").mkdirs();
        new File(id + "/restored").mkdirs();
    }

    @SuppressWarnings("unchecked")
    public static void generateDataBase()
    {
        FileInputStream fis;

        try
        {
            fis = new FileInputStream("backedChuncks.ser");

            try
            {
                ObjectInputStream ois = new ObjectInputStream(fis);

                Peer.setBackupUpChuncksTable((Hashtable<String, int[]>) ois.readObject());

                ois.close();
                fis.close();
            }
            catch(IOException e)
            {
                System.out.println("Couldn't deserialize backed chuncks file");
            }
            catch(ClassNotFoundException e)
            {
                System.out.println("Object serialized doesn't correspond to expected class");
            }
        }
        catch(FileNotFoundException e)
        {
            System.out.println("Couldn't find previous stored chuncks database file, generating new one...");

            Peer.setBackupUpChuncksTable(new Hashtable<String, int[]>());
            Peer.saveTableToDisk(1);
        }

        try
        {
            fis = new FileInputStream("chuncksStorage.ser");

            try
            {
                ObjectInputStream ois = new ObjectInputStream(fis);

                Peer.setChuncksStorageTable((Hashtable<String, ArrayList<Integer>>) ois.readObject());

                ois.close();
                fis.close();
            }
            catch(IOException e)
            {
                System.out.println("Couldn't deserialize backed chuncks file");
            }
            catch(ClassNotFoundException e)
            {
                System.out.println("Object serialized doesn't correspond to expected class");
            }
        }
        catch(FileNotFoundException e)
        {
            System.out.println("Couldn't find previous chuncks storage database file, generating new one...");

            Peer.setChuncksStorageTable(new Hashtable<String, ArrayList<Integer>>());
            Peer.saveTableToDisk(2);
        }
        
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

    

    public boolean sendGetChunk(String fileId, byte[] chunk, int chunkNo){
      String msg = "GETCHUNK " + version + " " + id + " " + fileId + " " + chunkNo + " " + " \r\n\r\n";

      byte[] header = msg.getBytes();
      byte[] getchunk = new byte[header.length + chunk.length];

      System.arraycopy(header, 0, getchunk, 0, header.length);
      System.arraycopy(chunk, 0, getchunk, header.length, chunk.length);

      try
      {
          DatagramPacket packet = new DatagramPacket(getchunk, getchunk.length, mdbAddr, mdrPort);

          restoreSocket.send(packet);
      }
      catch(Exception e)
      {
          System.out.println("Couldn't send getchunk");
      }

      return true;
    }

    

    public void backupFile(String path, int replication)
    {
        Backup backup = new Backup(path, replication);
        backup.start();
    }

    public void restoreFile(String path)
    {
      File file = new File(path);

      if(!file.exists())
      {
          System.out.println("Couldn't find file to restore: " + path);
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

            if(!sendGetChunk(fileId, buffer, partCounter))
                return;
        }
      }
      catch(Exception e)
      {
          System.out.println("Couldn't separate file into chuncks");
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

      try {
          FileWriter writer = new FileWriter("utils/fileNames.txt");
          BufferedWriter bufferedWriter = new BufferedWriter(writer);

          bufferedWriter.write(path);
          bufferedWriter.close();
          System.out.println("Wrote in file: " + path);
      } catch (IOException e) {
          e.printStackTrace();
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

          mcSocket.joinGroup(mcAddr);
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

              //if(!sendPutChunck(fileId, buffer, partCounter, replication))
                //  return;

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

    public static void setVersion(String version)
    {
        Peer.version = version;
    }

    public static void setId(int id)
    {
        Peer.id = id;
    }

    public static void setBackupUpChuncksTable(Hashtable<String, int[]> newTable)
    {
        backedUpChuncks = newTable;
    }

    public static void setChuncksStorageTable(Hashtable<String, ArrayList<Integer>> newTable)
    {
        chuncksStorage = newTable;
    }

    public static Hashtable<String, int[]> getLocalChuncksTable()
    {
        return backedUpChuncks;
    }

    public static Hashtable<String, ArrayList<Integer>> getChuncksStorageTable()
    {
        return chuncksStorage;
    }
}
