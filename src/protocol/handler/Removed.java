package protocol.handler;

import app.Peer;
import protocol.initiator.Backup;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.MulticastSocket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Random;

public class Removed extends Peer
{
    private String[] msgParams;

    public Removed(String[] msgParams) 
    {
        this.msgParams = msgParams;
    }

    @Override
    public void run()
    {
        if(!checkVersion(msgParams[1]))
        {
            System.out.println("Version mismatch in REMOVED message: " + msgParams[1]);
            return;
        }

        if(msgParams.length != 5)
        {
            System.out.println("Invalid REMOVED message: " + joinMessageParams(msgParams));
            return;
        }

        String senderId = msgParams[2], fileId = msgParams[3], chunckNo = msgParams[4], 
        localChunckKey = fileId + "-" + chunckNo;

        if(Integer.parseInt(senderId) == id)
        {
            //System.out.println("REMOVED message received originated from same peer, ignoring...");
            return;
        }
            
        ArrayList<Integer> chunckExternalCount = chuncksStorage.get(localChunckKey);
        int[] localChunckParams = backedUpChuncks.get(localChunckKey);
        int replication;

        if(chunckExternalCount != null)
        {
            chunckExternalCount.remove((Object) Integer.parseInt(senderId));

            if(chunckExternalCount.size() == 0)
                chuncksStorage.remove(localChunckKey);
            else
                chuncksStorage.put(localChunckKey, chunckExternalCount);

            changedChunksStorage.set(true);
            //saveTableToDisk(2);
        }

        if(localChunckParams != null)
            if((replication = getChunckReplication(localChunckKey)) < localChunckParams[0])
            {
                MulticastSocket mdbSocket;
                Random rand = new Random();
                int receiveTime = rand.nextInt(400);
                
                try
                {
                   mdbSocket = new MulticastSocket(mdbPort);
                   mdbSocket.joinGroup(mdbAddr);
                   mdbSocket.setTimeToLive(1);
                   mdbSocket.setSoTimeout(receiveTime);
                }
                catch(IOException e)
                {
                    System.out.println("Couldn't listen on MDB channel for PUTCHUNK messages after REMOVED");
                    return;
                }

                byte[] buffer = new byte[64100];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

                try
                {
                    while(true)
                    {
                        mdbSocket.receive(packet);

                        byte[] actualData = new byte[packet.getLength()];
    
                        System.arraycopy(packet.getData(), 0, actualData, 0, actualData.length);
    
                        String msg = new String(actualData).trim();
    
                        msgParams = msg.split("\\s+");
    
                        if(msgParams.length == 0)
                        {
                            System.out.println("Corrupt message @ REMOVED handler, skipping...");
                            continue;
                        }

                        for(int i = 0; i < msgParams.length; i++)
                            msgParams[i] = msgParams[i].trim();
            
                        if(msgParams[0].equals("PUTCHUNK") && msgParams.length >= 7 && msgParams[3].equals(fileId)
                            && msgParams[4].equals(chunckNo))
                        {
                            System.out.println("PUTCHUNK message received in response to REMOVED, not sending new PUTCHUNK...");
                            mdbSocket.close();
                            return;
                        }
                                      
                    }
                    
                }
                catch(Exception e)
                {
                    if(!(e instanceof SocketTimeoutException))
                    {
                        System.out.println("Couldn't receive packet on MDB channel after REMOVED");
                        mdbSocket.close();
                        return;
                    }


                }
                
                Backup backup = new Backup("database/" + id + "/backup/" + fileId + "/chk" + chunckNo, replication, false);

                backup.start();
                mdbSocket.close();
            }
    }


}