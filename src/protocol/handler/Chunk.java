package protocol.handler;

import app.Peer;

public class Chunk extends Peer
{
    private String[] msgParams;
    byte[] bytes;

    public Chunk(String[] msgParams, byte[] bytes)
    {
        this.msgParams = msgParams;
        this.bytes = bytes;
    }

    @Override
    public void run()
    {
        if(msgParams.length < 6)
        {
            System.out.println("Invalid CHUNK message");
            return;
        }

        if(!checkVersion(msgParams[1]))
            return;        
    }

}
