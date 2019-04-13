package protocol.handler;

import java.util.Random;
import java.net.DatagramPacket;
import java.net.MulticastSocket;
import java.net.SocketTimeoutException;
import java.io.IOException;
import java.util.ArrayList;
import java.io.File;
import java.io.FileOutputStream;
import app.Peer;

public class PutChunckEnh extends Peer
{
    private String[] msgParams;
    byte[] bytes;

    public PutChunckEnh(String[] msgParams, byte[] bytes)
    {
        this.msgParams = msgParams;
        this.bytes = bytes;
    }

    @Override
    public void run()
    {
        if(!checkVersion(msgParams[1]))
        {
            System.out.println("Version mismatch in PUTCHUNK message: " + msgParams[1]);
            return;
        }
            
        if(msgParams.length < 6)
        {
            System.out.println("Invalid PUTCHUNK message: " + joinMessageParams(msgParams));
            return;
        }

        if(msgParams[2].equals("" + id)) //Peer that initiated backup cannot store chuncks
        {
            //System.out.println("PUTCHUNK message received originated from same peer, ignoring...");
            return;
        }
            
        int bodyIndex = getMessageBodyIndex(bytes);

        if(bodyIndex == -1)
        {
            System.out.println("Couldn't find double CRLF sequence in PUTCHUNK message");
            return;
        }

        int chunckSize = bytes.length - bodyIndex;

        if (getFolderSize(new File("database/" + id + "/backup")) + chunckSize > diskSpace)
        {
            System.out.println("Disk space is too low to store chunck in PUTCHUNK message, ignoring...");
            return;
        }
            
        String fileId = msgParams[3], chunckNo = msgParams[4], replication = msgParams[5],
            path = "database/" + id + "/backup/" + fileId;

        if(checkForStored(fileId, chunckNo, Integer.parseInt(replication)))
        {
            System.out.println("Number of peers that backed up chunk already complies with desired replication, ignoring...");
            return;
        }

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
            System.out.println("Couldn't write chunk into file");
            return;
        }
    }

    public boolean checkForStored(String fileId, String chunckNo, int replication)
    {
        int counter = 0;
        Random rand = new Random();
        MulticastSocket mdbListener = null;
        
        try 
        {
            mdbListener = new MulticastSocket(mcPort);

            mdbListener.joinGroup(mcAddr);
            mdbListener.setTimeToLive(1);
            mdbListener.setSoTimeout(rand.nextInt(400));

            while (true) 
            {
                byte[] buf = new byte[64100];
                DatagramPacket packet = new DatagramPacket(buf, buf.length);

                mdbListener.receive(packet);

                byte[] actualData = new byte[packet.getLength()];

                System.arraycopy(packet.getData(), 0, actualData, 0, actualData.length);

                String msg = new String(actualData).trim();
                String[] params = msg.split("\\s+");

                if (params.length == 0) 
                {
                    System.out.println("Corrupt message @ PUTCHUNCk protocol handler STORED message receiver, skipping...");
                    continue;
                }

                for (int i = 0; i < params.length; i++)
                    params[i] = params[i].trim();

                if(params.length == 5 && params[0].equals("STORED") && checkVersion(params[1]) && params[3].equals(fileId)
                        && params[4].equals(chunckNo)) 
                    counter++;
            }
        } 
        catch (SocketTimeoutException e) 
        {
            mdbListener.close();

            return counter >= replication;
        }
        catch(IOException e)
        {
            System.out.println("Couldn't listen for STORED messages in PUTCHUNCK handler");
            return false;
        }
    }

    public void sendStored(String fileId, String chunckNo)
    {
        String storedMsg = "STORED " + version + " " + id + " " + fileId + " " + chunckNo + " \r\n\r\n";
        byte[] header = storedMsg.getBytes();
        DatagramPacket packet = new DatagramPacket(header, header.length, mcAddr, mcPort);
        
        try
        {
            controlSocket.send(packet);
        }
        catch(IOException e)
        {
            System.out.println("Couldn't send STORED message: " + storedMsg);
        }
    }

}