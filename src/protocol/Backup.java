package protocol;

import java.net.MulticastSocket;
import java.net.InetAddress;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.ArrayList;

public class Backup extends ProtocolThread
{
    private MulticastSocket mdbSocket;

    private DatagramSocket controlSocket;

    private int mcPort;
    private InetAddress mcAddr; 

    private int id;
    private String version;

    public Backup(int id, String version, int mcPort, InetAddress mcAddr, int mdbPort, InetAddress mdbAddr)
    {
        this.mcPort = mcPort;
        this.mcAddr = mcAddr;
        this.id = id;
        this.version = version;

        try
        {
            mdbSocket = new MulticastSocket(mdbPort);
            mdbSocket.joinGroup(mdbAddr);
            mdbSocket.setTimeToLive(1);

            controlSocket = new DatagramSocket();
        }
        catch(Exception e)
        {
            System.out.println("Couldn't open socket(s) in: Backup");
        }
    }

    @Override
    public void run()
    {
        String msg;
        String[] msgParams;

        while(true)
        {
            byte[] buf = new byte[64100];
            DatagramPacket receivedPacket = new DatagramPacket(buf, buf.length);
            
            try
            {
                mdbSocket.receive(receivedPacket);

                byte[] actualData = new byte[receivedPacket.getLength()];

                System.arraycopy(receivedPacket.getData(), 0, actualData, 0, actualData.length);

                msg = new String(actualData).trim();

                msgParams = msg.split("\\s+");

                if(msgParams.length == 0)
                {
                    System.out.println("Corrupt message @ backup");
                    continue;
                }

                for(int i = 0; i < msgParams.length; i++)
                    msgParams[i] = msgParams[i].trim();

                switch(msgParams[0])
                {
                    case "PUTCHUNCK":
                        putChunck(msgParams, actualData);
                        break;

                    case "STORED":
                        handleStored(msgParams);
                        break;

                    default:
                        System.out.println("Couldn't identify message in backup: " + msgParams[0]);
                }
                    
                
            }
            catch(IOException e)
            {
                System.out.println("Couldn't receive packet");
            }
            
        }
    }

    public void putChunck(String[] msgParams, byte[] bytes)
    {
        if(!checkVersion(msgParams[1]))
            return;

        if(msgParams.length < 6)
        {
            System.out.println("Invalid PUTCHUNCK message");
            return;
        }

        if(msgParams[2].equals("" + id)) //Peer that initiated backup cannot store chuncks
            return;

        String fileId = msgParams[3], chunckNo = msgParams[4], replication = msgParams[5], 
            path = id + "/backup/" + fileId;

        new File(path).mkdirs();

        File chunckFile = new File(path + "/chk" + chunckNo);
        
        try
        {
            if(chunckFile.createNewFile()) //If chunck doesn't exist already
            {
                for(int i = 0; i < bytes.length - 4; i++)
                    if(bytes[i] == 13 && bytes[i + 1] == 10 && bytes[i + 2] == 13 && bytes[i + 3] == 10) //CRLF's
                    {
                        i += 4;

                        byte[] body = new byte[bytes.length - i];

                        System.arraycopy(bytes, i, body, 0, body.length);

                        FileOutputStream fos = new FileOutputStream(chunckFile);

                        fos.write(body);
                        fos.close();

                        String key = fileId + "-" + chunckNo;

                        backedUpChuncks.put(key, new int[] {Integer.parseInt(replication), 
                            bytes.length - i});

                        ArrayList<Integer> chunckLocation = chuncksStorage.get(key);

                        if(chunckLocation == null)
                            chunckLocation = new ArrayList<Integer>();
                        
                        chunckLocation.add(id);
                        
                        chuncksStorage.put(fileId + "-" + chunckNo, chunckLocation);

                        sendStored(fileId, chunckNo);
                        saveTableToDisk(1);
                        saveTableToDisk(2);
                    }
            }
            else
                if(backedUpChuncks.get(fileId + "-" + chunckNo) != null)
                    sendStored(fileId, chunckNo);
        }
        catch(IOException e)
        {
            System.out.println("Couldn't write chunck into file");
            return;
        }    
    }

    public void sendStored(String fileId, String chunckNo)
    {
        String storedMsg = "STORED " + version + " " + id + " " + fileId + " " + chunckNo + " \r\n\r\n";
        byte[] header = storedMsg.getBytes();
        DatagramPacket packet = new DatagramPacket(header, header.length, mcAddr, mcPort);
        Random rand = new Random();
        int waitingTime = rand.nextInt(400);

        try
        {
            TimeUnit.MILLISECONDS.sleep(waitingTime);
        }
        catch(InterruptedException e)
        {
            System.out.println("Couldn't sleep before sending STORED response");
        }
        
        try
        {
            controlSocket.send(packet);
        }
        catch(IOException e)
        {
            System.out.println("Couldn't send STORED message");
        }
    }

    public void handleStored(String[] msgParams)
    {
        if(!checkVersion(msgParams[1]))
            return;

        if(msgParams.length < 5)
        {
            System.out.println("Invalid STORED message");
            return;
        }

        int senderId = Integer.parseInt(msgParams[2]);
        String fileId = msgParams[3], chunckNo = msgParams[4], key = fileId + "-" + chunckNo;
        ArrayList<Integer> chunckLocation = chuncksStorage.get(key);

        if(chunckLocation != null)
        {
            if(chunckLocation.contains(senderId))
                return;
            else
            {
                chunckLocation.add(senderId);
                chuncksStorage.put(key, chunckLocation);
                saveTableToDisk(2);
            }
        }
        else
        {
            chunckLocation = new ArrayList<Integer>();
            chunckLocation.add(senderId);
            chuncksStorage.put(key, chunckLocation);
            saveTableToDisk(2);
        }

    }

    public boolean checkVersion(String msgVersion)
    {
        return version.equals(msgVersion);
    }
}