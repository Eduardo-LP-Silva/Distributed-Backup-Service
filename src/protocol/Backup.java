package protocol;

import java.net.MulticastSocket;
import java.net.InetAddress;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class Backup extends Thread
{
    private MulticastSocket mdbSocket;
    private DatagramSocket backupSocket;

    private DatagramSocket controlSocket;

    private int mcPort;
    private InetAddress mcAddr; 

    private int mdbPort;
    private InetAddress mdbAddr;

    public Backup(int mcPort, InetAddress mcAddr, int mdbPort, InetAddress mdbAddr)
    {
        this.mcPort = mcPort;
        this.mcAddr = mcAddr;
        this.mdbPort = mdbPort;
        this.mdbAddr = mdbAddr;

        try
        {
            mdbSocket = new MulticastSocket(mdbPort);
            mdbSocket.joinGroup(mdbAddr);
            mdbSocket.setTimeToLive(1);

            controlSocket = new DatagramSocket();
            backupSocket = new DatagramSocket();
        }
        catch(Exception e)
        {
            System.out.println("Couldn't open socket(s) in: Backup");
        }
    }

    @Override
    public void run()
    {
        while(true)
        {
            byte[] buf = new byte[1024 * 1024];
            DatagramPacket receivedPacket = new DatagramPacket(buf, buf.length);
            
            try
            {
                mdbSocket.receive(receivedPacket);

                //TODO Complete
            }
            catch(IOException e)
            {
                System.out.println("Couldn't receive packet");
            }
            
        }
    }
}