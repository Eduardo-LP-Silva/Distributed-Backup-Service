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

        System.out.println("Message Params");
        for (int i = 0; i < msgParams.length; i++){
          System.out.println(msgParams[i]);
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

}
