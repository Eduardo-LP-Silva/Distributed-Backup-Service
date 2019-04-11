package protocol.handler;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.net.DatagramPacket;
import java.io.IOException;
import java.util.ArrayList;
import java.io.File;
import java.io.FileOutputStream;
import app.Peer;
import java.io.BufferedReader;
import java.io.FileReader;


public class GetChunk extends Peer
{
    private String[] msgParams;
    byte[] bytes;

    public GetChunk(String[] msgParams)
    {
        this.msgParams = msgParams;
    }

    @Override
    public void run()
    {
        if(!checkVersion(msgParams[1]))
            return;

        if(msgParams.length < 4)
        {
            System.out.println("Invalid GETCHUNK message");
            return;
        }

        String path = "database/" + id + "/backup/" + msgParams[3] + "/chk" + msgParams[4];
        File file = new File(path);

        if(!file.exists())
        {
            System.out.println("Couldn't find chunk to restore: " + path);
            return;
        }

        try(FileInputStream fis = new FileInputStream(file); BufferedInputStream bis = new BufferedInputStream(fis);)
        {
            int bytesRead = 0;

            int aux = (int) file.length();

            byte[] buffer = new byte[aux];
            bis.read(buffer);

            sendChunk(msgParams[3], Integer.parseInt(msgParams[4]), buffer);

        }
        catch(Exception e)
        {
            System.out.println("Couldn't separate file into chuncks");
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
        Random rand = new Random();
        int waitingTime = rand.nextInt(400);

        try
        {
            TimeUnit.MILLISECONDS.sleep(waitingTime);
        }
        catch(InterruptedException e)
        {
            System.out.println("Couldn't sleep before sending CHUNK response");
        }

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
