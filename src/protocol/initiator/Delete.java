package protocol.initiator;

import java.io.File;
import java.net.DatagramPacket;
import java.util.concurrent.TimeUnit;
import java.util.Set;

import app.Peer;

public class Delete extends Peer
{
    private String path;

    public Delete(String path)
    {
        this.path = path;
    }

    @Override
    public void run()
    {
        File file = new File(path);

        if(!file.exists())
        {
            System.out.println("Couldn't find file to delete: " + path);
            return;
        }

        String fileId = generateFileId(file);

        for(int i = 0; i < 5; i++)
        {
            if(!sendDelete(fileId))
                return;

            try
            {
                TimeUnit.SECONDS.sleep(1);
            }
            catch(InterruptedException e)
            {
                System.out.println("Couldn't sleep in between DELETE messages sending");
            }
            
        }
        
        Set<String> keys = chuncksStorage.keySet();
        
        for(String key: keys)
        {
            if(key.substring(0, 64).equals(fileId))
                chuncksStorage.remove(key);
            else
                break;
        }
        
        saveTableToDisk(2);
        file.delete();
    }


    public boolean sendDelete(String fileId)
    {
        String msg = "DELETE " + version + " " + id + " " + fileId + " \r\n\r\n";

        byte[] msgBytes = msg.getBytes();

        try
        {
            DatagramPacket packet = new DatagramPacket(msgBytes, msgBytes.length, mcAddr, mcPort);
            controlSocket.send(packet);
        }
        catch(Exception e)
        {
            System.out.println("Couldn't send DELETE");
        }

        return true;
    }
}
