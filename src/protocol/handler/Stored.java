package protocol.handler;

import app.Peer;
import java.util.ArrayList;

public class Stored extends Peer
{
    private String[] msgParams;

    public Stored(String[] msgParams)
    {
        this.msgParams = msgParams;
    }

    @Override
    public void run()
    {
        if(!checkVersion(msgParams[1]))
        {
            System.out.println("Version mismatch in STORED message: " + msgParams[1]);
            return;
        }

        if(msgParams.length < 5)
        {
            System.out.println("Invalid STORED message: " + joinMessageParams(msgParams));
            return;
        }

        int senderId = Integer.parseInt(msgParams[2]);
        String fileId = msgParams[3], chunckNo = msgParams[4], key = fileId + "-" + chunckNo;
        ArrayList<Integer> chunckLocation = chuncksStorage.get(key);

        if(chunckLocation != null)
        {
            if(chunckLocation.contains(senderId))
            {
                //System.out.println("STORED message received from peer that had already stored chunck, ignoring...");
                return;
            } 
            else
            {
                chunckLocation.add(senderId);
                chuncksStorage.put(key, chunckLocation);
                changedChunksStorage.set(true);
                //saveTableToDisk(2);
            }
        }
        else
        {
            chunckLocation = new ArrayList<Integer>();
            chunckLocation.add(senderId);
            chuncksStorage.put(key, chunckLocation);
            changedChunksStorage.set(true);
            //saveTableToDisk(2);
        }
    }
}
