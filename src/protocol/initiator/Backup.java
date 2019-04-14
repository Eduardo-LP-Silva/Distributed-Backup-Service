package protocol.initiator;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.net.MulticastSocket;
import java.net.DatagramPacket;
import java.io.IOException;
import java.net.SocketTimeoutException; 
import app.Peer;

public class Backup extends Peer
{
    private String path;
    private int replication;
    private boolean file;

    public Backup(String path, int replication, boolean file)
    {
        this.path = path;
        this.replication = replication;
        this.file = file;
    }

    @Override
    public void run()
    {
        if(file)
            backupFile();
        else
            backupChunck();
    }

    public void backupFile()
    {
        File file = new File(path);

        if(!file.exists())
        {
            System.out.println("Couldn't find file to backup: " + path);
            return;
        }

        if(replication <= 0)
        {
            System.out.println("Invalid replication degree:" + replication);
            return;
        }

        String fileId = generateFileId(file);
        int fileSize = (int) file.length();
        int partCounter,  nChuncks = (int) Math.ceil((double) fileSize / 64000);
        int responseWaitingTime;
        int attemptNo;

        if(fileSize % 64000 == 0)
            nChuncks += 1;

        backUpRecordsTable.put(fileId, new String[] {path, "" + replication, "" + nChuncks});
        changedRecordsTable.set(true);
        //saveTableToDisk(3);

        try(FileInputStream fis = new FileInputStream(file); BufferedInputStream bis = new BufferedInputStream(fis);)
        {
            int bytesRead = 0;

            for(partCounter = 0; partCounter < nChuncks; partCounter++)
            {
                int aux = (int) file.length() - bytesRead, bufferSize;
                
                responseWaitingTime = 1 * 1000;
                attemptNo = 1;

                if (aux > 64000)
                    bufferSize = 64000; //Maximum chunck size
                else
                    bufferSize = aux;

                byte[] buffer = new byte[bufferSize];

                bytesRead += bis.read(buffer);

                if(!sendPutChunck(fileId, buffer, partCounter, replication))
                    return;

                while(attemptNo <= 5)
                {
                    if(receiveStored(responseWaitingTime, replication, fileId, partCounter))
                        break;
                    else
                    {
                        responseWaitingTime *= 2;
                        attemptNo++;
                    }
                }

                if(attemptNo > 5)
                    System.out.println("Max attempts to send PUTCHUNK reached\nChunk not stored with required replication");
            }
        }
        catch(Exception e)
        {
            System.out.println("Couldn't separate file into chuncks");
        }
    }

    public void backupChunck()
    {
        File chunck = new File(path);

        if(!chunck.exists())
        {
            System.out.println("Couldn't find chunk to backup: " + path);
            return;
        }

        if(replication <= 0)
        {
            System.out.println("Invalid replication degree:" + replication);
            return;
        }

        int responseWaitingTime;
        int attemptNo;
        int chunckSize = (int) chunck.length();
        String fileId = chunck.getParentFile().getName();
        int chunckNo = Integer.parseInt(chunck.getName().substring(3));

        try(FileInputStream fis = new FileInputStream(chunck); BufferedInputStream bis = new BufferedInputStream(fis);)
        {
            byte[] buffer = new byte[chunckSize];
            
            responseWaitingTime = 1 * 1000;
            attemptNo = 1;

            if (!sendPutChunck(fileId, buffer, chunckNo, replication))
                return;

            while (attemptNo <= 5) 
            {
                if (receiveStored(responseWaitingTime, replication, fileId, chunckNo))
                    break;
                else 
                {
                    responseWaitingTime *= 2;
                    attemptNo++;
                }
            }

            if (attemptNo > 5)
                System.out.println("Max attempts to send PUTCHUNK reached\nChunk not stored with required replication");
            
        }
        catch(Exception e)
        {
            System.out.println("Couldn't separate file into chunks");
        }
    }

    public boolean receiveStored(int timeout, int replication, String fileId, int chunckNo)
    {
        byte[] buffer = new byte[64100];
        DatagramPacket receivedPacket = new DatagramPacket(buffer, buffer.length);
        int replicationCounter = 0;
        MulticastSocket mcSocket;

        try
        {
            mcSocket = new MulticastSocket(mcPort);

            mcSocket.joinGroup(mcAddr);
            mcSocket.setTimeToLive(1);
            mcSocket.setSoTimeout(timeout);
        }
        catch(IOException e)
        {
            System.out.println("Couldn't open multicast socket to receive STORED messages");
            return false;
        }

        try
        {
            while(replicationCounter < replication)
            {
                mcSocket.receive(receivedPacket);

                byte[] actualData = new byte[receivedPacket.getLength()];

                System.arraycopy(receivedPacket.getData(), 0, actualData, 0, actualData.length);

                String msg = new String(actualData).trim();
                String[] msgParams = msg.split("\\s+");

                if(msgParams.length == 0)
                {
                    System.out.println("Corrupt message @ Backup protocol initiator STORED message handler, skipping...");
                    continue;
                }

                for(int i = 0; i < msgParams.length; i++)
                    msgParams[i] = msgParams[i].trim();

                if(msgParams[0].equals("STORED"))
                {
                    if(msgParams.length < 5)
                    {
                        System.out.println("Invalid STORED message: " + joinMessageParams(msgParams));
                        continue;
                    }

                    String version = msgParams[1], fileIdReceived = msgParams[3], chunckNoReceived = msgParams[4];

                    if(!version.equals(Peer.version) || !fileIdReceived.equals(fileId)
                        || Integer.parseInt(chunckNoReceived) != chunckNo)
                        continue;
                    else
                        replicationCounter++;
                }
            }
        }
        catch(Exception e)
        {
            if(e instanceof SocketTimeoutException)
                System.out.println("Didn't received required STORED answers to meet replication demands");
            else
                System.out.println("Couldn't received STORED");

            mcSocket.close();
            return false;
        }

        mcSocket.close();
        return true;
    }

    public boolean sendPutChunck(String fileId, byte[] chunck, int chuckNo, int replication)
    {
        String msg = "PUTCHUNK " + version + " " + id + " " + fileId + " " + chuckNo + " " + replication
            + " \r\n\r\n";

        byte[] header = msg.getBytes();
        byte[] putchunck = new byte[header.length + chunck.length];

        System.arraycopy(header, 0, putchunck, 0, header.length);
        System.arraycopy(chunck, 0, putchunck, header.length, chunck.length);

        try
        {
            DatagramPacket packet = new DatagramPacket(putchunck, putchunck.length, mdbAddr, mdbPort);

            backupSocket.send(packet);
        }
        catch(Exception e)
        {
            System.out.println("Couldn't send putchunk");
        }

        return true;
    }
}