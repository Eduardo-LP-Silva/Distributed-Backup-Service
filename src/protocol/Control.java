package protocol;

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.DatagramPacket;
import java.io.IOException;

public class Control extends Thread
{
    private MulticastSocket mcSocket;
    private DatagramSocket controlSocket;

    private int mcPort;
    private InetAddress mcAddr; 

    public Control(int mcPort, InetAddress mcAddr)
    {
        this.mcPort = mcPort;
        this.mcAddr = mcAddr;

        try
        {
            mcSocket = new MulticastSocket(mcPort);
            mcSocket.joinGroup(mcAddr);
            mcSocket.setTimeToLive(1);

            controlSocket = new DatagramSocket();
        }
        catch(Exception e)
        {
            System.out.println("Couldn't open socket(s) in: Control");
        }
        
    }

    @Override
    public void run()
    {
        while(true)
        {
            byte[] buf = new byte[1024];
            DatagramPacket receivedPacket = new DatagramPacket(buf, buf.length);
            
            try
            {
                mcSocket.receive(receivedPacket);

                //TODO Complete
            }
            catch(IOException e)
            {
                System.out.println("Couldn't receive packet");
            }
        }
    }
}