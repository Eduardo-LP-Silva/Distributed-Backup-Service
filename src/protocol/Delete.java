package protocol;

import java.net.MulticastSocket;
import java.net.InetAddress;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.io.File;

public class Delete extends Thread
{
    private MulticastSocket mdbSocket;
    private DatagramSocket deleteSocket;

    private DatagramSocket controlSocket;

    private int mcPort;
    private InetAddress mcAddr;

    private int mdbPort;
    private InetAddress mdbAddr;

    String id;

    public Delete(String id, int mcPort, InetAddress mcAddr, int mdbPort, InetAddress mdbAddr)
    {
        this.mcPort = mcPort;
        this.mcAddr = mcAddr;
        this.mdbPort = mdbPort;
        this.mdbAddr = mdbAddr;
        this.id = id;

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
      String msg;

      while(true)
      {
          byte[] buf = new byte[1024 * 1024];
          DatagramPacket receivedPacket = new DatagramPacket(buf, buf.length);

          String path = "testFile.txt";

          File file = new File("peer_" + id + "/" + path);
          System.out.println("Deleted file: " + "peer_" + id + "/" + path);

          if(file.exists())
          {
            file.delete();
            System.out.println("Deleted file: " + "peer_" + id + "/" + path);
          }

          try
          {
              mdbSocket.receive(receivedPacket);

              msg = new String(receivedPacket.getData()).trim();
              String[] newMsg = msg.split("\\s+");

              System.out.println("Chunk Removal");
              String chunkId = newMsg[3];
              int chunkNo = Integer.parseInt(newMsg[4]);

              System.out.println(chunkId);
              System.out.println(chunkNo);

              String chunkPath = "peer_" + id + "/backup/" + chunkId;
              File chunkFile = new File(chunkPath + "/chk" + chunkNo);

              if(chunkFile.exists())
              {
                chunkFile.delete();
                System.out.println("Deleted chunkFile: " + "peer_" + chunkPath + "/chk" + chunkNo);
              }

          }
          catch(IOException e)
          {
              System.out.println("Couldn't receive packet");
          }

      }

    }
}
