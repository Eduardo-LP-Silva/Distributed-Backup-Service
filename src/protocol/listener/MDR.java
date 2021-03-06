package protocol.listener;

import java.net.MulticastSocket;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import app.Peer;
import protocol.handler.Chunk;

public class MDR extends Peer
{
    private MulticastSocket mdrSocket;

    public MDR()
    {
        try
        {
            mdrSocket = new MulticastSocket(mdrPort);
            mdrSocket.joinGroup(mdrAddr);
            mdrSocket.setTimeToLive(1);

            controlSocket = new DatagramSocket();
        }
        catch(Exception e)
        {
            System.out.println("Couldn't open socket(s) in: MDR");
        }
    }

    @Override
    public void run()
    {
        String msg;
        String[] msgParams;

        while(true)
        {
            byte[] buf = new byte[64100];
            DatagramPacket receivedPacket = new DatagramPacket(buf, buf.length);

            try
            {
                mdrSocket.receive(receivedPacket);

                byte[] actualData = new byte[receivedPacket.getLength()];

                System.arraycopy(receivedPacket.getData(), 0, actualData, 0, actualData.length);

                msg = new String(actualData).trim();

                msgParams = msg.split("\\s+");

                if(msgParams.length == 0)
                {
                    System.out.println("Corrupt message @ MDR");
                    continue;
                }

                for(int i = 0; i < msgParams.length; i++)
                    msgParams[i] = msgParams[i].trim();

                switch(msgParams[0])
                {
                    case "CHUNK":
                        Chunk chunk = new Chunk(msgParams, actualData);
                        chunk.start();
                        break;

                    default:
                        System.out.println("Couldn't identify message in MDR: " + msgParams[0]);
                }


            }
            catch(IOException e)
            {
                System.out.println("Couldn't receive packet in MDR");
            }

        }
    }
}
