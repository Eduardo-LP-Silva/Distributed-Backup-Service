package app;

import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.registry.Registry;

public class Peer implements BackupService
{
    public static void main(String args[])
    {
        setUpClientInterface();
    }

    public Peer() {}

    public static void setUpClientInterface()
    {
        try
        {
            Peer obj = new Peer();
            BackupService stub = (BackupService) UnicastRemoteObject.exportObject(obj, 0);

            Registry registry = LocateRegistry.getRegistry();
            registry.bind("Backup Service", stub);

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