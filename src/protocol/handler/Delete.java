package protocol.handler;

import java.io.File;

import app.Peer;

public class Delete extends Peer
{
    private String[] msgParams;

    public Delete(String[] msgParams)
    {
        this.msgParams = msgParams;
    }

    @Override
    public void run()
    {
        if(!checkVersion(msgParams[1]))
        {
            System.out.println("Version mismatch in DELETE message: " + msgParams[1]);
            return;
        }
            
        if(msgParams.length < 4)
        {
            System.out.println("Invalid DELETE message: " + joinMessageParams(msgParams));
            return;
        }

        String fileId = msgParams[3], path = "database/" + id + "/backup/" + fileId, chunckNo, chunckKey;
        File chuncksFolder = new File(path);

        if(chuncksFolder.isDirectory())
        {
            for(File chunck: chuncksFolder.listFiles())
            {
                chunckNo = chunck.getName().substring(3); 
                chunckKey = fileId + "-" + chunckNo;

                chunck.delete();
                backedUpChuncks.remove(chunckKey);
                chuncksStorage.remove(chunckKey);
            }

            saveTableToDisk(1);
            saveTableToDisk(2); 
        }

        chuncksFolder.delete();
    }

}
