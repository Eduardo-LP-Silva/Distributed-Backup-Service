package app;

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
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

    public void breakDownFile(File file)
    {
        long length = file.length();
        

    }

    public static void setUpClientInterface()
    {
        try
        {
            Peer obj = new Peer();
            BackupService stub = (BackupService) UnicastRemoteObject.exportObject(obj, 0);

            Registry registry = LocateRegistry.getRegistry();
            registry.bind(accessPoint, stub);

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

        //TODO Smt
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