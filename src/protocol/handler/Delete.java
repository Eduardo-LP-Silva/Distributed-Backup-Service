package protocol.handler;

import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.net.DatagramPacket;
import java.io.IOException;
import java.util.ArrayList;
import java.io.File;
import java.io.FileOutputStream;
import app.Peer;

public class Delete extends Peer
{
    private String[] msgParams;
    byte[] bytes;

    public Delete(String[] msgParams)
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
            System.out.println("Invalid DELETE message");
            return;
        }

        String path = "database/" + id + "/backup/" + msgParams[3] + "/chk" + msgParams[4];
        File file = new File(path);

        if (Integer.parseInt(msgParams[2]) == id){
          if(!file.exists())
          {
              System.out.println("Couldn't find chunk to delete: " + path);
          }
          else{
            System.out.println("File deleted: " + path);
            file.delete();
          }
        }
        File folder = new File("database/" + id + "/backup/" + msgParams[3]);
        folder.delete();
    }

}
