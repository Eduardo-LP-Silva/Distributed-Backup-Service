package app;

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.security.MessageDigest;
import java.rmi.registry.Registry;
import protocol.initiator.Restore;
import protocol.initiator.Delete;
import protocol.initiator.Reclaim;
import protocol.initiator.Backup;
import protocol.listener.MDB;
import protocol.listener.MDR;
import protocol.listener.MC;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Date;
import java.net.DatagramPacket;

public class Peer extends Thread implements BackupService
{
    protected static DatagramSocket controlSocket;
    protected static DatagramSocket backupSocket;
    protected static DatagramSocket restoreSocket;
    protected static int mcPort, mdbPort, mdrPort;
    protected static InetAddress mcAddr, mdbAddr, mdrAddr;
    protected static int id;
    protected static AtomicInteger diskSpace;
    protected static String accessPoint;
    protected static String version;
    protected static ConcurrentHashMap<String, int[]> backedUpChuncks; //fileID-ChunckNo -> {replication_expected, size}
    protected static ConcurrentHashMap<String, ArrayList<Integer>> chuncksStorage; //fileID-ChunckNo -> {1, 2, ...}
    protected static ConcurrentHashMap<String, String[]> backUpRecordsTable; //fileID -> {filePath, expected_replication, n_chuncks}
    protected static AtomicBoolean changedBackedUpChunks;
    protected static AtomicBoolean changedChunksStorage;
    protected static AtomicBoolean changedRecordsTable;
    
    public static void main(String args[])
    {
        System.setProperty("java.net.preferIPv4Stack", "true");

        diskSpace = new AtomicInteger(Integer.MAX_VALUE);

        if(args.length >= 3)
        {
            try
            {
                mcAddr = InetAddress.getByName("224.0.0.1");
                mdbAddr = InetAddress.getByName("224.0.0.1");
                mdrAddr = InetAddress.getByName("224.0.0.1");
            }
            catch(UnknownHostException e)
            {
                System.out.println("Unknown Host");
                return;
            }

            mcPort = 5001;
            mdbPort = 5002;
            mdrPort = 5003;

            version = args[0];
            id = Integer.parseInt(args[1]);
            accessPoint = args[2];

            createDirectory();
        }
        else
        {
            System.out.println("Wrong number of arguments: " + args.length);
            System.out.println("Usage: Peer <ProtocolVersion> <ServerID> <AccessPoint> [<mcAddr> <mcPort> <mdbAddr> <mdbPort> <mdrAddr> <mdrPort>]");
            return;
        }

        if (args.length == 9)
        {
            try
            {
                mcAddr = InetAddress.getByName(args[3]);
                mcPort = Integer.parseInt(args[4]);

                mdbAddr = InetAddress.getByName(args[5]);
                mdbPort = Integer.parseInt(args[6]);

                mdrAddr = InetAddress.getByName(args[7]);
                mdrPort = Integer.parseInt(args[8]);
            }
            catch(UnknownHostException e)
            {
                System.out.println("Unknown Host(s): " + args[3] + ", " + args[5] + ", " + args[7]);
                return;
            }

        }
        
        generateDataBase();
        savePeriodically();
        setUpClientInterface();

        try
        {
            controlSocket = new DatagramSocket();
            backupSocket = new DatagramSocket();
            restoreSocket = new DatagramSocket();
        }
        catch(SocketException e)
        {
            System.out.println("Couldn't open communication sockets in peer.");
            return;
        }

        try
        {
            MDB mdbListener = new MDB();
            MDR mdrListener = new MDR();
            MC mcListener = new MC();

            mdbListener.start();
            mdrListener.start();
            mcListener.start();

            System.out.println("Started channel listeners");
        }
        catch(Exception e)
        {
            System.out.println("Could't create channel listeners");
        }
    }

    public static void savePeriodically()
    {
        Timer timer = new Timer();

        timer.scheduleAtFixedRate(new TimerTask()
        {
            @Override
            public void run()
            {
                if(changedBackedUpChunks.get())
                {
                    System.out.println("Saved backed up chunks table");
                    saveTableToDisk(1);
                    changedBackedUpChunks.set(false);
                }

                if(changedChunksStorage.get())
                {
                    System.out.println("Saved chunks storage table");
                    saveTableToDisk(2);
                    changedChunksStorage.set(false);
                }

                if(changedRecordsTable.get())
                {
                    System.out.println("Saved back up records table");
                    saveTableToDisk(3);
                    changedRecordsTable.set(false);
                }
            }
        }, new Date(), 30 * 1000);
    }

    public static void saveTableToDisk(int table)
    {
        FileOutputStream fos;

        try
        {
            ObjectOutputStream oos = null;

            switch(table)
            {
                case 1:
                    fos = new FileOutputStream("database/" + id + "/backedChuncks.ser");
                    oos = new ObjectOutputStream(fos);
                    oos.writeObject(backedUpChuncks);
                    break;

                case 2:
                    fos = new FileOutputStream("database/" + id + "/chuncksStorage.ser");
                    oos = new ObjectOutputStream(fos);
                    oos.writeObject(chuncksStorage);
                    break;

                case 3:
                    fos = new FileOutputStream("database/" + id + "/backupRecords.ser");
                    oos = new ObjectOutputStream(fos);
                    oos.writeObject(backUpRecordsTable);
                    break;

                default:
                    System.out.println("Invalid table to save to disk: " + table);
                    return;
            }

            
            oos.close();
            fos.close();
        }
        catch(IOException e)
        {
            System.out.println("Couldn't save table to disk");
        }
    }

    public static void printLocalChuncksTable()
    {
        Set<String> keys = backedUpChuncks.keySet();

        System.out.println("--- Backed Up Chuncks ---");

        for(String key: keys)
        {
            int[] values = backedUpChuncks.get(key);

            System.out.print(key + "-> [");

            for(int i = 0; i < values.length; i++)
            {
                System.out.print(values[i]);

                if(i != values.length - 1)
                    System.out.print(", ");
            }

            System.out.println("]");
        }

        System.out.println("\n");
    }

    public boolean checkVersion(String msgVersion)
    {
        return version.equals(msgVersion);
    }

    public static void printChuncksStorageTable()
    {
        Set<String> keys = chuncksStorage.keySet();

        System.out.println("--- Chuncks Storage ---");

        for(String key: keys)
        {
            ArrayList<Integer> values = chuncksStorage.get(key);

            System.out.print(key + "-> [");

            for(int i = 0; i < values.size(); i++)
            {
                System.out.print(values.get(i));

                if(i != values.size() - 1)
                    System.out.print(", ");
            }

            System.out.println("]");
        }

        System.out.println("\n");
    }

    public static void printBackupRecordsTable()
    {
        Set<String> keys = backUpRecordsTable.keySet();

        System.out.println("--- Backup Records ---");

        for(String key: keys)
        {
            String[] values = backUpRecordsTable.get(key);

            System.out.print(key + "-> [");

            for(int i = 0; i < values.length; i++)
            {
                System.out.print(values[i]);

                if(i != values.length - 1)
                    System.out.print(", ");
            }

            System.out.println("]");
        }
    }

    public static void createDirectory()
    {
        new File("database/" + id + "/backup").mkdirs();
        new File("database/" + id + "/restored").mkdirs();
    }

    @SuppressWarnings("unchecked")
    public static void generateDataBase()
    {
        FileInputStream fis;

        changedBackedUpChunks = new AtomicBoolean();
        changedChunksStorage = new AtomicBoolean();
        changedRecordsTable = new AtomicBoolean();

        try
        {
            fis = new FileInputStream("database/" + id + "/backedChuncks.ser");

            try
            {
                ObjectInputStream ois = new ObjectInputStream(fis);

                Peer.setBackupUpChuncksTable((ConcurrentHashMap<String, int[]>) ois.readObject());

                ois.close();
                fis.close();
            }
            catch(IOException e)
            {
                System.out.println("Couldn't deserialize backed chuncks file");
            }
            catch(ClassNotFoundException e)
            {
                System.out.println("Object serialized doesn't correspond to expected class ConcurrentHashMap<String, int[]>");
            }
        }
        catch(FileNotFoundException e)
        {
            System.out.println("Couldn't find previous stored chuncks database file in " + "database/" + id + "/backedChuncks.ser" + ", generating new one...");

            Peer.setBackupUpChuncksTable(new ConcurrentHashMap<String, int[]>());
            Peer.saveTableToDisk(1);
        }

        try
        {
            fis = new FileInputStream("database/" + id + "/chuncksStorage.ser");

            try
            {
                ObjectInputStream ois = new ObjectInputStream(fis);

                Peer.setChuncksStorageTable((ConcurrentHashMap<String, ArrayList<Integer>>) ois.readObject());

                ois.close();
                fis.close();
            }
            catch(IOException e)
            {
                System.out.println("Couldn't deserialize backed chuncks file");
            }
            catch(ClassNotFoundException e)
            {
                System.out.println("Object serialized doesn't correspond to expected class ConcurrentHashMap<String, ArrayList<Integer>>");
            }
        }
        catch(FileNotFoundException e)
        {
            System.out.println("Couldn't find previous chuncks storage database file in " + "database/" + id + "/chuncksStorage.ser" + ", generating new one...");

            Peer.setChuncksStorageTable(new ConcurrentHashMap<String, ArrayList<Integer>>());
            Peer.saveTableToDisk(2);
        }

        try
        {
            fis = new FileInputStream("database/" + id + "/backupRecords.ser");

            try
            {
                ObjectInputStream ois = new ObjectInputStream(fis);

                Peer.setBackupRecordsTable((ConcurrentHashMap<String, String[]>) ois.readObject());

                ois.close();
                fis.close();
            }
            catch(IOException e)
            {
                System.out.println("Couldn't deserialize backed up records file");
            }
            catch(ClassNotFoundException e)
            {
                System.out.println("Object serialized doesn't correspond to expected class ConcurrentHashMap<String, String[]>");
            }
        }
        catch(FileNotFoundException e)
        {
            System.out.println("Couldn't find previous backup up records database file in " + "database/" + id + "/backupRecords.ser" + ", generating new one...");

            Peer.setBackupRecordsTable(new ConcurrentHashMap<String, String[]>());
            Peer.saveTableToDisk(3);
        }

    }

    public String generateFileId(File file)
    {
        try
        {
            //Get metadata attributes
            BasicFileAttributes attributes = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
            String hashInput = file.getName() + attributes.lastModifiedTime() + attributes.size(); //Create the value to hash
            MessageDigest digest = MessageDigest.getInstance("SHA-256"); //Create hash instance
            byte[] hash = digest.digest(hashInput.getBytes(StandardCharsets.UTF_8)); //Hash
            char[] hexChars = new char[hash.length * 2], hexArray = "0123456789ABCDEF".toCharArray(); //Convert to format

            for(int j = 0; j < hash.length; j++ )
            {
                int v = hash[j] & 0xFF;
                hexChars[j * 2] = hexArray[v >>> 4];
                hexChars[j * 2 + 1] = hexArray[v & 0x0F];
            }

            return new String(hexChars);

        }
        catch(Exception e)
        {
            System.out.println("Couldn't generate file id");
            return null;
        }
    }

    public static void setUpClientInterface()
    {
        try
        {
            Peer obj = new Peer();
            BackupService stub = (BackupService) UnicastRemoteObject.exportObject(obj, 0);

            Registry registry = LocateRegistry.getRegistry();
            registry.rebind(accessPoint, stub);

            System.out.println("Client-Server Interface Set Up");
        }
        catch(Exception e)
        {
            System.out.println("Client-Server exception: " + e.toString());
        }
    }


    public void backupFile(String path, int replication, boolean enh)
    {
        if(enh)
            Peer.version = "2.0";

        Backup backup = new Backup(path, replication, true);
        backup.start();
    }

    public void restoreFile(String path)
    {
      Restore restore = new Restore(path);
      restore.start();
    }

    public void deleteFile(String path)
    {
      Delete delete = new Delete(path);
      delete.start();
    }

    public void manageStorage(int maxSpace)
    {
        Reclaim reclaim = new Reclaim(maxSpace);
        reclaim.start();
    }

    public String retrieveInfo()
    {
        String info;

        info = "--- Initiated Backup Records ---\n\n";

        Set<String> keys = backUpRecordsTable.keySet();
        String[] backUpDetails;
        String externalStorageKey;
        ArrayList<Integer> externalStorage;

        for(String fileId: keys)
        {
            backUpDetails = backUpRecordsTable.get(fileId);

            if(backUpDetails.length != 3)
            {
                info += "Invalid back up record with only " + backUpDetails.length + " fields\n\n";
                continue;
            }

            info += "File: " + backUpDetails[0] + "\n\n";
            info += "File ID: " + fileId + "\n\n";
            info += "Desired Replication: " + backUpDetails[1] + "\n\n";
            info += "Number of Chuncks: " + backUpDetails[2] + "\n\n";

            for(int i = 0; i < Integer.parseInt(backUpDetails[2]); i++)
            {
                externalStorageKey = fileId+ "-" + i;
                externalStorage = chuncksStorage.get(externalStorageKey);

                if(externalStorage != null)
                    info += "Chunck " + i + " replication: " + externalStorage.size() + "\n\n";
                else
                    info += "Chunck " + i + " replication: " + 0 + "\n\n";
            }
        }

        info += "\n--- Local Chuncks---\n\n\n";

        keys = backedUpChuncks.keySet();

        String[] chunckParams;
        int replication;

        for(String localChunckKey: keys)
        {
            chunckParams = localChunckKey.split("-");

            info += "Chunck " + chunckParams[1] + " of file with ID "+ chunckParams[0] + ":\n\n";
            info += "\t- Size: " + backedUpChuncks.get(localChunckKey)[1] / 1000 + "kB\n\n";

            externalStorage = chuncksStorage.get(localChunckKey);

            if(externalStorage != null)
                replication = externalStorage.size();
            else
                replication = 0;

            info += "\t- Replication: " + replication + "\n\n";
        }

        info += "\n--- Storage Details ---\n\n";

        int spaceOccupied = getFolderSize(new File("database/" + id + "/backup"));

        info += "Storage Capacity: " + diskSpace.get() / 1000 + "kB\n\n";
        info += "Occupied space: " + spaceOccupied / 1000 + "kB\n\n";

        return info;
    }

    public void sendRemoved(String localChunckKey)
    {
        String[] chunckParams = localChunckKey.split("-");

        if(chunckParams.length != 2)
        {
            System.out.println("Invalid local chunck format stored in local chuncks table with only " + chunckParams.length + " fields");
            return;
        }

        String fileId = chunckParams[0], chunckNo = chunckParams[1];
        String msg = "REMOVED " + version + " " + id + " " + fileId  + " " + chunckNo + " \r\n\r\n";
        byte[] msgData = msg.getBytes(); 
        File localChunck = new File("database/" + id + "/backup/" + fileId + "/chk" + chunckNo);

        localChunck.delete();
        backedUpChuncks.remove(localChunckKey);

        ArrayList<Integer> chunckExternalStorage = chuncksStorage.get(localChunckKey);
        
        chunckExternalStorage.remove((Object) id);

        if(chunckExternalStorage.size() == 0)
            chuncksStorage.remove(localChunckKey);
        else
            chuncksStorage.put(localChunckKey, chunckExternalStorage);

        changedChunksStorage.set(true);
        //saveTableToDisk(2);

        DatagramPacket packet = new DatagramPacket(msgData, msgData.length, mcAddr, mcPort);

        try
        {
            controlSocket.send(packet);
        }
        catch(IOException E)
        {
            System.out.println("Couldn't send REMOVED message: " + msg);
        }
    }

    public void cleanEmptyFolders()
    {
        String databasePath = "database/" + id + "/backup";
        File backupFolder = new File(databasePath);

        for(File file: backupFolder.listFiles())
            if(file.listFiles().length == 0)
                file.delete();
    }

    public String joinMessageParams(String[] msgParams)
    {
        String message = "";

        for(int i = 0; i < msgParams.length; i++)
            message += msgParams[i];

        return message;
    }

    public static void setVersion(String version)
    {
        Peer.version = version;
    }

    public static void setId(int id)
    {
        Peer.id = id;
    }

    public static void setBackupUpChuncksTable(ConcurrentHashMap<String, int[]> newTable)
    {
        backedUpChuncks = newTable;
    }

    public static void setChuncksStorageTable(ConcurrentHashMap<String, ArrayList<Integer>> newTable)
    {
        chuncksStorage = newTable;
    }

    public static void setBackupRecordsTable(ConcurrentHashMap<String, String[]> newTable)
    {
        backUpRecordsTable = newTable;
    }

    public static ConcurrentHashMap<String, int[]> getLocalChuncksTable()
    {
        return backedUpChuncks;
    }

    public static ConcurrentHashMap<String, ArrayList<Integer>> getChuncksStorageTable()
    {
        return chuncksStorage;
    }

    public static int getChunckReplication(String chunckKey)
    {
        int rep = 0;
        ArrayList<Integer> chunckExternalStorage = chuncksStorage.get(chunckKey);

        if(chunckExternalStorage != null)
            rep = chunckExternalStorage.size();

        return rep;
    }

    public static int getFolderSize(File folder)
    {
        int size = 0;

        for (File file : folder.listFiles()) 
        {
            if (file.isFile())
                size += file.length();
            else
                size += getFolderSize(file);
        }

        return size;
    }

    public static int getMessageBodyIndex(byte[] bytes)
    {
        int i;

        for(i = 0; i < bytes.length - 4; i++)
            if(bytes[i] == 13 && bytes[i + 1] == 10 && bytes[i + 2] == 13 && bytes[i + 3] == 10) //CRLF's
            {
                i += 4;
                break;
            }    

        if(i == 0)
            return -1;
        else
            return i;
    }
}
