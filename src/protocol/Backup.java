package protocol;

import java.net.MulticastSocket;
import java.util.Hashtable;
import java.net.InetAddress;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class Backup extends Thread
{
    private MulticastSocket mdbSocket;

    private DatagramSocket controlSocket;

    private int mcPort;
    private InetAddress mcAddr;

    private String id;
    private String version;

    private Hashtable<String, int[]> storedChuncksReplication; //fileID-ChunckNo -> {replication_expected, actual_replication}

    public Backup(String id, String version, int mcPort, InetAddress mcAddr, int mdbPort, InetAddress mdbAddr)
    {
        this.mcPort = mcPort;
        this.mcAddr = mcAddr;
        this.id = id;
        this.version = version;

        storedChuncksReplication = new Hashtable<String, int[]>();

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
                        updateChunckReplication(msgParams);
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

        if(msgParams[2].equals(id)) //Peer that initiated backup cannot store chuncks
            return;

        String fileId = msgParams[3], chunckNo = msgParams[4], replication = msgParams[5],
            path = id + "/backup/" + fileId;

        new File(path).mkdirs();

        File chunckFile = new File(path + "/chk" + chunckNo);

        try
        {
            if(chunckFile.createNewFile()) //If chunck doesn't exist already
            {
                for(int i = 0; i < bytes.length - 1; i++)
                    if(bytes[i] == 13 && bytes[i + 1] == 10) //CRLF's
                        for(int j = i + 2; j < bytes.length - 1; j++)
                            if(bytes[j] == 13 && bytes[j + 1] == 10)
                            {
                                j += 2;

                                byte[] body = new byte[bytes.length - j];

                                System.arraycopy(bytes, j, body, 0, body.length);

                                FileOutputStream fos = new FileOutputStream(chunckFile);

                                fos.write(body);
                                fos.close();
                            }
            }

            storedChuncksReplication.put(fileId + "-" + chunckNo, new int[] {Integer.parseInt(replication), 1});
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

    public void updateChunckReplication(String[] msgParams)
    {
        if(!checkVersion(msgParams[1]))
            return;

        if(msgParams.length < 5)
        {
            System.out.println("Invalid STORED message");
            return;
        }

        String fileId = msgParams[3], chunckNo = msgParams[4], key = fileId + "-" + chunckNo;
        int[] replications;

        if((replications = storedChuncksReplication.get(key)) != null)
        {
            replications[1]++;
            storedChuncksReplication.put(key, replications);
        }

    }

    public boolean checkVersion(String msgVersion)
    {
        return version.equals(msgVersion);
    }
}
