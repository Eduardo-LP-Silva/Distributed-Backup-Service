package protocol.initiator;

import app.Peer;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Set;
import java.net.DatagramPacket;

public class Reclaim extends Peer
{
    private int maxSpace;

    public Reclaim(int maxSpace)
    {
        this.maxSpace = maxSpace;
    }

    @Override
    public void run()
    {
        File backupFolder = new File("database/" + id + "/backup");
        Set<String> keys = backedUpChuncks.keySet();
        int spaceReleased = 0, spaceOccupied;
        ArrayList<Integer> replications;

        if((spaceOccupied = (int) getFolderSize(backupFolder)) <= maxSpace)
            return;
        
        if(maxSpace != 0)
            for(String localChunck: keys)
            {
                if((replications = chuncksStorage.get(localChunck)) != null)
                    if(replications.size() >= backedUpChuncks.get(localChunck)[0])
                    {
                        spaceReleased += backedUpChuncks.get(localChunck)[1];
                        sendRemoved(localChunck);
                    
                        if(spaceOccupied - spaceReleased <= maxSpace)
                            return;
                    }        
            }

        for(String localChunck: keys)
        {
            spaceReleased += backedUpChuncks.get(localChunck)[1];
            sendRemoved(localChunck);

            if(spaceOccupied - spaceReleased <= maxSpace)
                return;
        }
    }

    public void sendRemoved(String localChunckKey)
    {
        String[] chunckParams = localChunckKey.split("-");

        if(chunckParams.length != 2)
        {
            System.out.println("Invalid local chunck format stored in local chuncks table");
            return;
        }

        String fileId = chunckParams[0], chunckNo = chunckParams[1];
        String msg = "REMOVED " + version + " " + id + " " + fileId  + " " + chunckNo + " \r\n\r\n";
        byte[] msgData = msg.getBytes(); 
        File localChunck = new File("database/" + id + "/backup/" + fileId + "/chk" + chunckNo);

        localChunck.delete();
        backedUpChuncks.remove(localChunckKey);

        ArrayList<Integer> chunckExternalStorage = chuncksStorage.get(localChunckKey);
        
        chunckExternalStorage.remove((Object) id);
        chuncksStorage.put(localChunckKey, chunckExternalStorage);

        DatagramPacket packet = new DatagramPacket(msgData, msgData.length, mcAddr, mcPort);

        try
        {
            controlSocket.send(packet);
        }
        catch(IOException E)
        {
            System.out.println("Couldn't send REMOVED message");
        }
        
    }


}