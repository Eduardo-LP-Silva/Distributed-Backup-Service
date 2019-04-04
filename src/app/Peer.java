package app;

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.security.MessageDigest;
import java.rmi.registry.Registry;
import protocol.*;
import java.io.File;

public class Peer implements BackupService
{
    private static DatagramSocket controlSocket;
    private static DatagramSocket backupSocket;
    private static DatagramSocket restoreSocket;
    private static int mcPort, mdbPort, mdrPort;
    private static String mcAddr, mdbAddr, mdrAddr;
    private static String id;
    private static String accessPoint;
    private static String version;

    public static void main(String args[])
    {
        if(args.length >= 3)
        {
            mcAddr = "224.0.0.1";
            mdbAddr = "224.0.0.1";
            mdrAddr = "224.0.0.1";
            
            mcPort = 5001;
            mdbPort = 5002;
            mdrPort = 5003;

            version = args[0];
            id = args[1];
            accessPoint = args[2];

            createDirectory();
        }
        else
        {
            System.out.println("Wrong number of arguments");
            System.out.println("Usage: Peer <ProtocolVersion> <ServerID> <AccessPoint> [<mcAddr> <mcPort> <mdbAddr> <mdbPort> <mdrAddr> <mdrPort>]");
            return;
        }

        if (args.length == 9) 
        {
            mcAddr = args[3];
            mcPort = Integer.parseInt(args[4]);

            mdbAddr = args[5];
            mdbPort = Integer.parseInt(args[6]);

            mdrAddr = args[7];
            mdrPort = Integer.parseInt(args[8]);
        }
            
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
            Control control = new Control(mcPort, InetAddress.getByName(mcAddr));
            Backup backup = new Backup(mcPort, InetAddress.getByName(mcAddr), mdbPort, InetAddress.getByName(mdbAddr));
            Restore restore = new Restore(mcPort, InetAddress.getByName(mcAddr), mdrPort, InetAddress.getByName(mdrAddr));

            control.run();
            backup.run();
            restore.run();
        }
        catch(Exception e)
        {
            System.out.println("Could't create threads");
        }
    }

    public static void createDirectory()
    {
        String dirName = "peer_" + id;
        boolean directoryAlreadyExists = new File(dirName).mkdirs();

        if(!directoryAlreadyExists)
        {
            new File(dirName + "/backup").mkdir();
            new File(dirName + "/restored").mkdir();
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

    public void backupFile(String path, int replication)
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
        long fileSize = file.length();


        
    }

    public void restoreFile()
    {
        //TODO Restore file
    }

    public void deleteFile()
    {
        //TODO Delete file
    }

    public void manageStorage()
    {
        //TODO Manage storage
    }

    public void retrieveInfo()
    {
        //TODO Get info
    }
}