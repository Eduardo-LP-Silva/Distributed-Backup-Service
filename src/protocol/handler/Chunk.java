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
    }

}
