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
                  System.out.println("Corrupt message @ restore");
                  continue;
              }

              for(int i = 0; i < msgParams.length; i++)
                  msgParams[i] = msgParams[i].trim();

              switch(msgParams[0])
              {
                  case "GETCHUNK":
                      System.out.println("Received a getchunk");
                      // putChunck(msgParams, actualData);
                      break;

                  case "STORED":
                      System.out.println("Received a stored");
                      break;

                  default:
                      System.out.println("Couldn't identify message in backup: " + msgParams[0]);
              }


          }
          catch(IOException e)
          {
              System.out.println("Couldn't receive packet");
          }

      }
    }
}
