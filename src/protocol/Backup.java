package protocol;

import java.net.MulticastSocket;
import java.util.Hashtable;
import java.net.InetAddress;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.io.File;
import java.io.FileOutputStream;

public class Backup extends Thread
{
    private MulticastSocket mdbSocket;
    private DatagramSocket backupSocket;

    private DatagramSocket controlSocket;

    private int mcPort;
    private InetAddress mcAddr; 

    private int mdbPort;
    private InetAddress mdbAddr;

    private String id;
    private String version;

    private Hashtable<String, int[]> storedChuncksReplication; //fileID-ChunckNo -> {replication_expected, actual_replication}

    public Backup(String id, String version, int mcPort, InetAddress mcAddr, int mdbPort, InetAddress mdbAddr)
    {
        this.mcPort = mcPort;
        this.mcAddr = mcAddr;
        this.mdbPort = mdbPort;
        this.mdbAddr = mdbAddr;
        this.id = id;
        this.version = version;
        
        storedChuncksReplication = new Hashtable<String, int[]>();

        try
        {
            mdbSocket = new MulticastSocket(mdbPort);
            mdbSocket.joinGroup(mdbAddr);
            mdbSocket.setTimeToLive(1);

            controlSocket = new DatagramSocket();
            backupSocket = new DatagramSocket();
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

                for(int i = 0; i < msgParams.length; i++)
                    msgParams[i] = msgParams[i].trim();

                if(msgParams[0].equals("PUTCHUNCK"))
                    putChunck(msgParams, actualData);
                
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

        if(msgParams[2].equals(id)) //Peer that initiated backup cannot store chuncks
            return;

        String fileId = msgParams[3], chunckNo = msgParams[4], path = id + "/backup/" + fileId;

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
        }
        catch(IOException e)
        {
            System.out.println("Couldn't write chunck into file");
            return;
        }    
    }

    public boolean checkVersion(String msgVersion)
    {
        return version.equals(msgVersion);
    }
}