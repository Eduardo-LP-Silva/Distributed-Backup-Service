package protocol.listener;

import java.net.MulticastSocket;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import app.Peer;
import protocol.handler.PutChunck;
import protocol.handler.PutChunckEnh;

public class MDB extends Peer
{
    private MulticastSocket mdbSocket;

    public MDB()
    {
        try
        {
            mdbSocket = new MulticastSocket(mdbPort);
            mdbSocket.joinGroup(mdbAddr);
            mdbSocket.setTimeToLive(1);

            controlSocket = new DatagramSocket();
        }
        catch(Exception e)
        {
            System.out.println("Couldn't open socket(s) in: MDB");
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
                mdbSocket.receive(receivedPacket);

                byte[] actualData = new byte[receivedPacket.getLength()];

                System.arraycopy(receivedPacket.getData(), 0, actualData, 0, actualData.length);

                msg = new String(actualData).trim();

                msgParams = msg.split("\\s+");

                if(msgParams.length == 0)
                {
                    System.out.println("Corrupt message @ MDB");
                    continue;
                }

                for(int i = 0; i < msgParams.length; i++)
                    msgParams[i] = msgParams[i].trim();

                switch(msgParams[0])
                {
                    case "PUTCHUNK":

                        if(Peer.version.equals("1.0"))
                        {
                            PutChunck pc = new PutChunck(msgParams, actualData);
                            pc.start();
                        }
                        else
                            if(Peer.version.equals("2.0"))
                            {
                                PutChunckEnh pce = new PutChunckEnh(msgParams, actualData);
                                pce.start();
                            }
                        
                        break;

                    default:
                        System.out.println("Couldn't identify message in MDB: " + msgParams[0]);
                }


            }
            catch(IOException e)
            {
                System.out.println("Couldn't receive packet in MDB");
            }

        }
    }
}