package protocol;

import java.net.MulticastSocket;
import java.net.InetAddress;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.io.File;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.BufferedWriter;
import java.io.FileWriter;

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

          try
          {
              String path = getFilePath();
              if (path != null){
                File file = new File(id + "/" + path);
                System.out.println("Deleted file: " + id + "/" + path);

                if(file.exists())
                {
                  file.delete();
                  System.out.println("Deleted file: " + id + "/" + path);
                }
              }

              mdbSocket.receive(receivedPacket);

              msg = new String(receivedPacket.getData()).trim();
              String[] newMsg = msg.split("\\s+");

              System.out.println("Chunk Removal");
              String chunkId = newMsg[3];
              int chunkNo = Integer.parseInt(newMsg[4]);

              System.out.println(chunkId);
              System.out.println(chunkNo);

              String chunkPath = id + "/backup/" + chunkId;
              File chunkFile = new File(chunkPath + "/chk" + chunkNo);

              if(chunkFile.exists())
              {
                chunkFile.delete();
                System.out.println("Deleted chunkFile: " + chunkPath + "/chk" + chunkNo);
              }

          }
          catch(IOException e)
          {
              System.out.println("Couldn't receive packet");
          }

      }

    }

    public String getFilePath(){
      try {
          FileReader reader = new FileReader("utils/fileNames.txt");
          BufferedReader bufferedReader = new BufferedReader(reader);

          String line;

          if ((line = bufferedReader.readLine()) != null) {
            reader.close();
            return line;
          }

      } catch (IOException e) {
          e.printStackTrace();
      }
      return null;
    }
}
