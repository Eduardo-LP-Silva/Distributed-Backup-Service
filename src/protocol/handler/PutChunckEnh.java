package protocol.handler;

import java.util.Random;
import java.net.DatagramPacket;
import java.io.IOException;
import java.util.ArrayList;
import java.io.File;
import java.io.FileOutputStream;
import app.Peer;
import java.util.Set;

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

        if (getFolderSize(new File("database/" + id + "/backup")) + chunckSize > diskSpace.get())
        {
            if(!freeUpSpace(chunckSize))
            {
                System.out.println("Disk space is too low to store chunck in PUTCHUNK message, ignoring...");
                return;
            }
            else
                cleanEmptyFolders();
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

                changedBackedUpChunks.set(true);
                changedChunksStorage.set(true);
                //saveTableToDisk(1);
                //saveTableToDisk(2);
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

    public boolean freeUpSpace(int chunkSize)
    {
        Set<String> localChunks = backedUpChuncks.keySet();
        int[] backedChunkDetails;
        ArrayList<Integer> chunkExternalStorage;
        File chunkToRemove;
        String[] chunkParams;

        for(String localChunkKey: localChunks)
        {
            backedChunkDetails = backedUpChuncks.get(localChunkKey);

            if(backedChunkDetails.length == 2)
            {
                chunkExternalStorage = chuncksStorage.get(localChunkKey);

                if(chunkExternalStorage != null)
                {
                    chunkParams = localChunkKey.split("-");

                    if(chunkParams.length != 2)
                    {
                        System.out.println("Invalid key in backed up chunks table, ignoring...");
                        continue;
                    }
                        
                    chunkToRemove = new File("database/" + id + "/backup/" + chunkParams[0] + "/chk" + chunkParams[1]);
                    
                    if(chunkToRemove.delete())
                    {
                        sendRemoved(localChunkKey);

                        System.out.println("Dropped chunk to free up space");

                        if(getFolderSize(new File("database/" + id + "/backup")) + chunkSize <= diskSpace.get())
                            return true;
                    }
                    else
                        System.out.println("Couldn't delete over replicated chunk to make space for new chunk");    
                }

            }
        }

        return false;
    }

    public boolean checkForStored(String fileId, String chunckNo, int replication)
    {
        String chunkKey = fileId + "-" + chunckNo;
        Random rand = new Random();
        
        try
        {
            sleep(rand.nextInt(400));
        }
        catch(InterruptedException e)
        {
            System.out.println("Couldn't wait for stored messages in PUTCHUNK handler enhanced");
        }
        
        ArrayList<Integer> chunkExternalStorage = chuncksStorage.get(chunkKey);

        if(chunkExternalStorage != null)
            return chunkExternalStorage.size() >= replication;
        else
            return false;
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