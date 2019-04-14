package protocol.initiator;

import app.Peer;
import java.io.File;
import java.util.ArrayList;
import java.util.Set;

public class Reclaim extends Peer
{
    public Reclaim(int maxSpace)
    {
        Peer.diskSpace.set(maxSpace);
    }

    @Override
    public void run()
    {
        File backupFolder = new File("database/" + id + "/backup");
        Set<String> keys = backedUpChuncks.keySet();
        int spaceReleased = 0, spaceOccupied;
        ArrayList<Integer> replications;

        if((spaceOccupied = (int) getFolderSize(backupFolder)) <= diskSpace.get())
        {
            System.out.println("Current occupied space is already lower than new limit, no need to evict chunks");
            return;
        }
            
        if(diskSpace.get() != 0)
            for(String localChunck: keys)
            {
                if((replications = chuncksStorage.get(localChunck)) != null)
                    if(replications.size() >= backedUpChuncks.get(localChunck)[0])
                    {
                        spaceReleased += backedUpChuncks.get(localChunck)[1];
                        sendRemoved(localChunck);
                    
                        if(spaceOccupied - spaceReleased <= diskSpace.get())
                        {
                            cleanEmptyFolders();
                            return;
                        }
                            
                    }        
            }

        for(String localChunck: keys)
        {
            spaceReleased += backedUpChuncks.get(localChunck)[1];
            sendRemoved(localChunck);

            if(spaceOccupied - spaceReleased <= diskSpace.get())
            {
                cleanEmptyFolders();
                return;
            }
        }
    }
}