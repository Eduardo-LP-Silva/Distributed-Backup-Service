package protocol.handler;

import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.net.DatagramPacket;
import java.io.IOException;
import java.util.ArrayList;
import java.io.File;
import java.io.FileOutputStream;
import app.Peer;

public class Chunk extends Peer
{
    private String[] msgParams;
    byte[] bytes;

    public Chunk(String[] msgParams, byte[] bytes)
    {
        super();
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
            System.out.println("Invalid CHUNK message");
            return;
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
