package protocol;

import java.net.MulticastSocket;
import java.net.InetAddress;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class Delete extends Thread
{
    private MulticastSocket mdbSocket;
    private DatagramSocket deleteSocket;

    private DatagramSocket controlSocket;

    private int mcPort;
    private InetAddress mcAddr;

    private int mdbPort;
    private InetAddress mdbAddr;

    public Delete(int mcPort, InetAddress mcAddr, int mdbPort, InetAddress mdbAddr)
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
            deleteSocket = new DatagramSocket();
        }
        catch(Exception e)
        {
            System.out.println("Couldn't open socket(s) in: Delete");
        }
    }

    @Override
    public void run()
    {
      // Como aceder ao path?
      System.out.println();

      // File file = new File(path);
      //
      // if(file.exists())
      // {
      //   file.delete();
      //   System.out.println("Deleted" + path);
      // }

    }
}
