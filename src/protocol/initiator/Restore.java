package protocol.initiator;
import java.io.FileOutputStream;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.net.MulticastSocket;
import java.net.DatagramPacket;
import java.io.IOException;
import java.net.SocketTimeoutException;
import app.Peer;
import java.io.BufferedWriter;
import java.io.FileWriter;

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
        int responseWaitingTime = 1 * 1000;
        int attemptNo = 1;

        if(fileSize % 64000 == 0)
            nChuncks += 1;

        try (FileOutputStream output = new FileOutputStream("database/" + id + "/restored/" + trimPath(path))) {
          output.close();
        }
        catch(IOException e){
          System.out.println(e);
        }


        try(FileInputStream fis = new FileInputStream(file); BufferedInputStream bis = new BufferedInputStream(fis);)
        {
            int bytesRead = 0;

            for(partCounter = 0; partCounter < nChuncks; partCounter++)
            {
                int aux = (int) file.length() - bytesRead, bufferSize;

                if (aux > 64000)
                    bufferSize = 64000; //Maximum chunck size
                else
                    bufferSize = aux;

                byte[] buffer = new byte[bufferSize];

                bytesRead += bis.read(buffer);

                if(!sendGetChunk(fileId, buffer, partCounter)){
                  System.out.println("Falhou no sendGetChunk");
                  return;
                }

                while(attemptNo <= 5)
                {
                    if(receiveChunk(responseWaitingTime, fileId, partCounter, buffer))
                        break;
                    else
                    {
                        responseWaitingTime *= 2;
                        attemptNo++;
                    }
                }

                if(attemptNo > 5)
                    System.out.println("Max attempts to send GETCHUNK reached. File not restored");
            }
        }
        catch(Exception e)
        {
            System.out.println("Couldn't separate file into chunks");
        }

        System.out.println("NUMERO DE CHUNKS");
        System.out.println(nChuncks);
    }

    public boolean receiveChunk(int timeout, String fileId, int chunckNo, byte[] chunk)
    {
        byte[] buffer = new byte[64100];
        DatagramPacket receivedPacket = new DatagramPacket(buffer, buffer.length);
        MulticastSocket mdrSocket;

        try
        {
            mdrSocket = new MulticastSocket(mdrPort);

            mdrSocket.joinGroup(mdrAddr);
            mdrSocket.setTimeToLive(1);
            mdrSocket.setSoTimeout(timeout);
        }
        catch(IOException e)
        {
            System.out.println("Couldn't open multicast socket to receive CHUNK messages");
            return false;
        }

        try
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
                System.out.println(msgParams[0]);
                System.out.println(msgParams[4]);
                  if(msgParams.length < 6)
                  {
                      System.out.println("Invalid CHUNK message");
                  }
                  else{
                    String version = msgParams[1], fileIdReceived = msgParams[3], chunckNoReceived = msgParams[4];
                    if(version.equals(Peer.version) && fileIdReceived.equals(fileId) && Integer.parseInt(chunckNoReceived) == chunckNo){
                        boolean flag = false;
                        for (int i = 5; i < actualData.length; i++){
                          try (FileOutputStream output = new FileOutputStream("database/" + id + "/restored/" + trimPath(path), true)) {
                            if(actualData[i] == 13 && actualData[i + 1] == 10 && actualData[i + 2] == 13 && actualData[i + 3] == 10){
                                i +=4;
                                flag = true;
                              }
                              if (flag == true){
                                output.write(actualData[i]);
                              }
                          }
                        }
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

    public boolean sendGetChunk(String fileId, byte[] chunk, int chunkNo)
    {
        String msg = "GETCHUNK " + version + " " + id + " " + fileId + " " + chunkNo + " " + " \r\n\r\n";

        byte[] header = msg.getBytes();
        byte[] getchunk = new byte[header.length];

        System.arraycopy(header, 0, getchunk, 0, header.length);

        try
        {
            DatagramPacket packet = new DatagramPacket(getchunk, getchunk.length, mdrAddr, mdrPort);
            restoreSocket.send(packet);
        }
        catch(Exception e)
        {
            System.out.println("Couldn't send GETCHUNK");
        }

        return true;
    }

    public String trimPath(String path){
      return path.substring(6);
    }
}
