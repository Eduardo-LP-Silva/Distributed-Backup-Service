package protocol.initiator;
import java.io.FileOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.net.MulticastSocket;
import java.net.DatagramPacket;
import java.io.IOException;
import java.net.SocketTimeoutException;
import app.Peer;

public class Restore extends Peer
{
    private String path;

    public Restore(String path)
    {
        this.path = path;
    }

    @Override
    public void run()
    {
        File file = new File(path);

        if(!file.exists())
        {
            System.out.println("Couldn't find file to restore: " + path);
            return;
        }

        String fileId = generateFileId(file);
        int fileSize = (int) file.length();
        int partCounter,  nChuncks = (int) Math.ceil((double) fileSize / 64000);

        if(fileSize % 64000 == 0)
            nChuncks += 1;

        File restoredFile = new File("database/" + id + "/restored/" + file.getName());

        if(restoredFile.exists())
            restoredFile.delete();

        FileOutputStream output;

        try
        {
            restoredFile.createNewFile();
        }
        catch(IOException e)
        {
            System.out.println("Couldn't create restored file");
            return;
        }

        try
        {
            output = new FileOutputStream(restoredFile, true);
        }
        catch(FileNotFoundException e)
        {
            System.out.println("Couldn't open restored file");
            return;
        }

        for (partCounter = 0; partCounter < nChuncks; partCounter++) 
        {
            if(!sendGetChunk(fileId, partCounter)) 
                break;

            if(!receiveChunk(fileId, partCounter, output))
                break;
        }

        try
        {
            output.close();
        }
        catch(IOException e)
        {
            System.out.println("Couldn't close restored file");
        }
        
    }

    public boolean receiveChunk(String fileId, int chunckNo, FileOutputStream output)
    {
        byte[] buffer = new byte[64100];
        DatagramPacket receivedPacket = new DatagramPacket(buffer, buffer.length);
        MulticastSocket mdrSocket;

        try
        {
            mdrSocket = new MulticastSocket(mdrPort);

            mdrSocket.joinGroup(mdrAddr);
            mdrSocket.setTimeToLive(1);
            mdrSocket.setSoTimeout(8 * 1000);
        }
        catch(IOException e)
        {
            System.out.println("Couldn't open multicast socket to receive CHUNK messages");
            return false;
        }

        try
        {
            while(true)
            {
                mdrSocket.receive(receivedPacket);

                byte[] actualData = new byte[receivedPacket.getLength()];
  
                System.arraycopy(receivedPacket.getData(), 0, actualData, 0, actualData.length);
  
                String msg = new String(actualData).trim();
  
                String[] msgParams = msg.split("\\s+");
  
                if(msgParams.length == 0)
                {
                    System.out.println("Corrupt message @ peer.receiveChunk");
                }
  
                for(int i = 0; i < msgParams.length; i++)
                    msgParams[i] = msgParams[i].trim();
  
                if(msgParams[0].equals("CHUNK"))
                {
                  if (msgParams.length < 6) 
                  {
                      System.out.println("Invalid CHUNK message");
                  } 
                  else 
                  {
                      String fileIdReceived = msgParams[3], chunckNoReceived = msgParams[4];
  
                      if (!checkVersion(msgParams[1]) || !fileIdReceived.equals(fileId)
                              || Integer.parseInt(chunckNoReceived) != chunckNo) 
                        continue;
  
                      int bodyIndex = getMessageBodyIndex(actualData);
  
                      if(bodyIndex == -1)
                        continue;
  
                      output.write(actualData, bodyIndex, actualData.length - bodyIndex);
                      break;
                  }
                }
            }
        }
        catch(Exception e)
        {
            if(e instanceof SocketTimeoutException)
                System.out.println("Didn't received required CHUNK");
            else
                System.out.println("Couldn't received CHUNK");

            mdrSocket.close();
            return false;
        }

        mdrSocket.close();
        return true;
    }

    public boolean sendGetChunk(String fileId, int chunkNo)
    {
        String msg = "GETCHUNK " + version + " " + id + " " + fileId + " " + chunkNo + " \r\n\r\n";
        byte[] getChunck = msg.getBytes();

        try
        {
            DatagramPacket packet = new DatagramPacket(getChunck, getChunck.length, mcAddr, mcPort);
            restoreSocket.send(packet);
        }
        catch(Exception e)
        {
            System.out.println("Couldn't send GETCHUNK");
        }

        return true;
    }

    public String trimPath(String path)
    {
      return path.substring(6);
    }
}
