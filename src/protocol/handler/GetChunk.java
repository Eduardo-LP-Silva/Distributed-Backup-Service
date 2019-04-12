package protocol.handler;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.net.DatagramPacket;
import java.net.MulticastSocket;
import java.net.SocketTimeoutException;
import java.io.IOException;
import java.io.File;
import app.Peer;

public class GetChunk extends Peer
{
    private String[] msgParams;

    public GetChunk(String[] msgParams)
    {
        this.msgParams = msgParams;
    }

    @Override
    public void run()
    {
        if(!checkVersion(msgParams[1]))
            return;

        if(msgParams.length < 5)
        {
            System.out.println("Invalid GETCHUNK message");
            return;
        }

        String fileId = msgParams[3], chunckNo = msgParams[4], chunckKey = fileId + "-" + chunckNo;

        if(backedUpChuncks.get(chunckKey) == null)
            return;

        File chunck = new File("database/" + id + "/backup/" + fileId+ "/chk" + chunckNo);

        try(FileInputStream fis = new FileInputStream(chunck); BufferedInputStream bis = new BufferedInputStream(fis);)
        {
            byte[] buffer = new byte[(int) chunck.length()];

            bis.read(buffer);

            Random rand = new Random();
            MulticastSocket mdrListener = new MulticastSocket(mdrPort);
            
            mdrListener.joinGroup(mdrAddr);
            mdrListener.setTimeToLive(1);
            mdrListener.setSoTimeout(rand.nextInt(400));

            try
            {
                while(true)
                {
                    byte[] buf = new byte[64100];
                    DatagramPacket mdrPacket = new DatagramPacket(buf, buf.length);
    
                    mdrListener.receive(mdrPacket);
    
                    byte[] actualData = new byte[mdrPacket.getLength()];
    
                    System.arraycopy(mdrPacket.getData(), 0, actualData, 0, actualData.length);
    
                    String msg = new String(actualData).trim();
                    String[] params = msg.split("\\s+");
    
                    if(params.length == 0)
                    {
                        System.out.println("Corrupt message @ GETCHUNCK handler");
                        continue;
                    }
    
                    for(int i = 0; i < params.length; i++)
                        params[i] = params[i].trim();

                    if(params.length >= 5 && params[0].equals("CHUNCK") && params[3].equals(fileId) 
                        && params[4].equals(chunckNo))
                    {
                        mdrListener.close();
                        return;
                    }       
                }
            }
            catch(SocketTimeoutException e) 
            {
                sendChunk(fileId, Integer.parseInt(chunckNo), buffer);
            }

            mdrListener.close();
        }
        catch(Exception e)
        {
            System.out.println("Couldn't read from chunck");
        }

    }

    public void sendChunk(String fileId, int chunckNo, byte[] chunk)
    {
        String storedMsg = "CHUNK " + version + " " + id + " " + fileId + " " + chunckNo + " \r\n\r\n";
        byte[] header = storedMsg.getBytes();
        byte[] chunkMsg = new byte[header.length + chunk.length];

        System.arraycopy(header, 0, chunkMsg, 0, header.length);
        System.arraycopy(chunk, 0, chunkMsg, header.length, chunk.length);

        DatagramPacket packet = new DatagramPacket(chunkMsg, chunkMsg.length, mdrAddr, mdrPort);

        try
        {
            restoreSocket.send(packet);
        }
        catch(IOException e)
        {
            System.out.println("Couldn't send CHUNK message");
        }
    }

}
