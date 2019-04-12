package protocol.handler;

import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.net.DatagramPacket;
import java.io.IOException;
import java.util.ArrayList;
import java.io.File;
import java.io.FileOutputStream;
import app.Peer;

public class PutChunck extends Peer
{
    private String[] msgParams;
    byte[] bytes;

    public PutChunck(String[] msgParams, byte[] bytes)
    {
        this.msgParams = msgParams;
        this.bytes = bytes;
    }

    @Override
    public void run()
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

        int bodyIndex = getMessageBodyIndex(bytes);

        if(bodyIndex == -1)
        {
            System.out.println("Couldn't find CRLF sequence in PUTCHUNCK message");
            return;
        }

        int chunckSize = bytes.length - bodyIndex;

        if (getFolderSize(new File("database/" + id + "/backup")) + chunckSize > diskSpace)
            return;

        String fileId = msgParams[3], chunckNo = msgParams[4], replication = msgParams[5],
            path = "database/" + id + "/backup/" + fileId;

        new File(path).mkdirs();

        File chunckFile = new File(path + "/chk" + chunckNo);

        try
        {
            if(chunckFile.createNewFile()) //If chunck doesn't exist already
            {
                byte[] body = new byte[bytes.length - bodyIndex];

                System.arraycopy(bytes, bodyIndex, body, 0, body.length);

                FileOutputStream fos = new FileOutputStream(chunckFile);

                fos.write(body);
                fos.close();

                String key = fileId + "-" + chunckNo;

                backedUpChuncks.put(key, new int[] { Integer.parseInt(replication), bytes.length - bodyIndex});

                ArrayList<Integer> chunckLocation = chuncksStorage.get(key);

                if (chunckLocation == null)
                    chunckLocation = new ArrayList<Integer>();

                chunckLocation.add(id);

                chuncksStorage.put(fileId + "-" + chunckNo, chunckLocation);

                sendStored(fileId, chunckNo);
                saveTableToDisk(1);
                saveTableToDisk(2);
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

}