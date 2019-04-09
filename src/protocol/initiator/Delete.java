package protocol.initiator;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.net.MulticastSocket;
import java.net.DatagramPacket;
import java.io.IOException;
import java.net.SocketTimeoutException;
import app.Peer;

public class Delete extends Peer
{
    private String path;

    public Delete(String path)
    {
        this.path = path;
    }

    @Override
    public void run()
    {
        File file = new File(path);

        if(!file.exists())
        {
            System.out.println("Couldn't find file to delete: " + path);
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

                if(!sendDelete(fileId, buffer, partCounter))
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


    public boolean sendDelete(String fileId, byte[] chunk, int chunkNo)
    {
        String msg = "DELETE " + version + " " + id + " " + fileId + " " + chunkNo + " " + " \r\n\r\n";

        byte[] header = msg.getBytes();
        byte[] delete = new byte[header.length];

        System.arraycopy(header, 0, delete, 0, header.length);

        try
        {
            DatagramPacket packet = new DatagramPacket(delete, delete.length, mcAddr, mcPort);
            deleteSocket.send(packet);
        }
        catch(Exception e)
        {
            System.out.println("Couldn't send DELETE");
        }

        return true;
    }
}
