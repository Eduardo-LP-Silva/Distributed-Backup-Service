package protocol;

import java.net.MulticastSocket;
import java.net.InetAddress;
import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.io.IOException;

public class Restore extends Thread
{
    private MulticastSocket mdrSocket;
    private DatagramSocket restoreSocket;

    private DatagramSocket controlSocket;

    private int mcPort;
    private InetAddress mcAddr; 

    private int mdrPort;
    private InetAddress mdrAddr; 

    public Restore(int mcPort, InetAddress mcAddr, int mdrPort, InetAddress mdrAddr)
    {
        this.mcPort = mcPort;
        this.mcAddr = mcAddr;
        this.mdrPort = mdrPort;
        this.mdrAddr = mdrAddr;
        
        try
        {
            mdrSocket = new MulticastSocket(mdrPort);
            mdrSocket.joinGroup(mdrAddr);
            mdrSocket.setTimeToLive(1);

            controlSocket = new DatagramSocket();
            restoreSocket = new DatagramSocket();
        }
        catch(Exception e)
        {
            System.out.println("Couldn't open socket(s) in: Restore");
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
                mdrSocket.receive(receivedPacket);

                //TODO Complete
            }
            catch(IOException e)
            {
                System.out.println("Couldn't receive packet");
            }
        }
    }
}