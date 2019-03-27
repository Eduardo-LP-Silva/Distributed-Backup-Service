package app;

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.registry.Registry;
import protocol.*;

public class Peer implements BackupService
{
    private static DatagramSocket controlSocket;
    private static DatagramSocket backupSocket;
    private static DatagramSocket restoreSocket;
    private static int mcPort, mdbPort, mdrPort;
    private static String mcAddr, mdbAddr, mdrAddr;
    public static void main(String args[])
    {
        

        if(args.length == 0)
        {
            mcAddr = "224.0.0.1";
            mdbAddr = "224.0.0.1";
            mdrAddr = "224.0.0.1";
            
            mcPort = 5001;
            mdbPort = 5002;
            mdrPort = 5003;
        }
        else
            if(args.length == 6)
            {
                mcAddr = args[0];
                mcPort = Integer.parseInt(args[1]);

                mdbAddr = args[2];
                mdbPort = Integer.parseInt(args[3]);

                mdrAddr = args[4];
                mdrPort = Integer.parseInt(args[5]);
            }
            else
            {
                System.out.println("Wrong number of arguments");
                System.out.println("Usage: Peer [<mcAddr> <mcPort> <mdbAddr> <mdbPort> <mdrAddr> <mdrPort>]");
                return;
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

    public Peer() {}

    public static void setUpClientInterface()
    {
        try
        {
            Peer obj = new Peer();
            BackupService stub = (BackupService) UnicastRemoteObject.exportObject(obj, 0);

            Registry registry = LocateRegistry.getRegistry();
            registry.bind("BackupService", stub);

            System.out.println("Client-Server Interface Set Up");
        }
        catch(Exception e)
        {
            System.out.println("Client-Server exception: " + e.toString());
        }
    }

    public void backupFile()
    {
        //TODO Backup file
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