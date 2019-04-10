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

public class GetChunk extends Peer
{
    private String[] msgParams;
    byte[] bytes;

    public GetChunk(String[] msgParams)
    {
        super();
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

        System.out.println("GetChunk Message Params");
        for (int i = 0; i < msgParams.length; i++){
          System.out.println(msgParams[i]);
        }


        String path = "files/testFile.txt";
        File file = new File(path);

        if(!file.exists())
        {
            System.out.println("Couldn't find file to backup: " + path);
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

                bytesRead += bis.read(buffer);

                sendChunk(fileId, partCounter, buffer);

            }
        }
        catch(Exception e)
        {
            System.out.println("Couldn't separate file into chuncks");
        }


        // if(msgParams[2].equals("" + id)) //Peer that initiated backup cannot store chuncks
        //     return;
        //
        // String fileId = msgParams[3], chunckNo = msgParams[4], replication = msgParams[5],
        //     path = id + "/backup/" + fileId;
        //
        // new File(path).mkdirs();
        //
        // File chunckFile = new File(path + "/chk" + chunckNo);

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
