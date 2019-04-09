package protocol.initiator;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
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
        int responseWaitingTime = 1 * 1000;
        int attemptNo = 1;

        if(fileSize % 64000 == 0)
            nChuncks += 1;

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

                if(!sendGetChunk(fileId, buffer, partCounter))
                    return;

                // while(attemptNo <= 5)
                // {
                //     if(receiveStored(responseWaitingTime, replication, fileId, partCounter))
                //         break;
                //     else
                //     {
                //         responseWaitingTime *= 2;
                //         attemptNo++;
                //     }
                // }
                //
                // if(attemptNo > 5)
                //     System.out.println("Max attempts to send PUTCHUNCK reached\nChunck not stored with required replication");
            }
        }
        catch(Exception e)
        {
            System.out.println("Couldn't separate file into chunks");
        }
    }

    // public boolean receiveStored(int timeout, int replication, String fileId, int chunckNo)
    // {
    //     byte[] buffer = new byte[64100];
    //     DatagramPacket receivedPacket = new DatagramPacket(buffer, buffer.length);
    //     int replicationCounter = 0;
    //     MulticastSocket mcSocket;
    //
    //     try
    //     {
    //         mcSocket = new MulticastSocket(mcPort);
    //
    //         mcSocket.joinGroup(mcAddr);
    //         mcSocket.setTimeToLive(1);
    //         mcSocket.setSoTimeout(timeout);
    //     }
    //     catch(IOException e)
    //     {
    //         System.out.println("Couldn't open multicast socket to receive STORED messages");
    //         return false;
    //     }
    //
    //     try
    //     {
    //         while(replicationCounter < replication)
    //         {
    //             mcSocket.receive(receivedPacket);
    //
    //             byte[] actualData = new byte[receivedPacket.getLength()];
    //
    //             System.arraycopy(receivedPacket.getData(), 0, actualData, 0, actualData.length);
    //
    //             String msg = new String(actualData).trim();
    //             String[] msgParams = msg.split("\\s+");
    //
    //             if(msgParams.length == 0)
    //             {
    //                 System.out.println("Corrupt message @ peer.receivePut");
    //                 continue;
    //             }
    //
    //             for(int i = 0; i < msgParams.length; i++)
    //                 msgParams[i] = msgParams[i].trim();
    //
    //             if(msgParams[0].equals("STORED"))
    //             {
    //                 if(msgParams.length < 5)
    //                 {
    //                     System.out.println("Invalid STORED message");
    //                     continue;
    //                 }
    //
    //                 String version = msgParams[1], fileIdReceived = msgParams[3], chunckNoReceived = msgParams[4];
    //
    //                 if(!version.equals(Peer.version) || !fileIdReceived.equals(fileId)
    //                     || Integer.parseInt(chunckNoReceived) != chunckNo)
    //                     continue;
    //                 else
    //                     replicationCounter++;
    //             }
    //         }
    //     }
    //     catch(Exception e)
    //     {
    //         if(e instanceof SocketTimeoutException)
    //             System.out.println("Didn't received required PUT answers to meet replication demands");
    //         else
    //             System.out.println("Couldn't received PUT");
    //
    //         mcSocket.close();
    //         return false;
    //     }
    //
    //     mcSocket.close();
    //     return true;
    // }

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
}
