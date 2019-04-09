package app;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.io.File;

public class TestClient
{
    private static BackupService peer;

    public static void main(String args[])
    {
        if(args.length < 3)
        {
            System.out.println("Wrong number of arguments\n Usage: TestClient <peer_ap> <operation> <opnd_1> [<opnd_2>]");
            return;
        }

        connectToBackupService(args[0]);

        switch (args[1])
        {
            case "BACKUPENH":

                System.out.println("Backup enhancement not implemented");
                break;

            case "BACKUP":

                if (args.length < 4)
                {
                    System.out.println("Wrong number of arguments\n");
                    System.out.println("Usage: TestClient <peer_ap> <operation> <opnd_1> <opnd_2>");
                    return;
                }

                try
                {
                    peer.backupFile(args[2], Integer.parseInt(args[3]));
                }
                catch(RemoteException e)
                {
                    System.out.println("Couldn't back up service");
                }

                break;

            case "RESTOREENH":
                System.out.println("Restore enhancement not implemented");
                break;

            case "RESTORE":

                try
                {
                    peer.restoreFile(args[2]);
                }
                catch(RemoteException e)
                {
                    System.out.println("Couldn't restore service");
                }
                break;

            case "DELETEENH":
                System.out.println("Delete enhancement not implemented");
                break;

            case "DELETE":
                try
                {
                    peer.deleteFile(args[2]);
                }
                catch(RemoteException e)
                {
                    System.out.println("Couldn't restore service");
                }
                break;

            case "RECLAIMENH":
                System.out.println("Reclaim enhancement not implemented");
                break;

            case "RECLAIM":
                manageStorage(Integer.parseInt(args[2]));
                break;

            case "STATE":
                getInfo();
                break;

            default:
                System.out.println("Unrecognized operation: " + args[1]);

        }
    }

    public static void connectToBackupService(String remoteObject) //remoteObject = "Backup Service"
    {
        try
        {
            Registry registry = LocateRegistry.getRegistry("localhost");
            peer = (BackupService) registry.lookup(remoteObject);
        }
        catch(Exception e)
        {
            System.out.println("Couldn't connect to server");
        }
    }

    public static void restoreFile(String path)
    {
        File file = new File(path);

        if(!file.exists())
        {
            System.out.println("Couldn't find file to restore: " + path);
            return;
        }

        //TODO Smt

        try
        {
            peer.restoreFile(path);
        }
        catch(RemoteException e)
        {
            System.out.println("Couldn't restore file " + path);
        }
    }

    public static void deleteFile(String path)
    {
        File file = new File(path);

        if(!file.exists())
        {
            System.out.println("Couldn't find file to delete: " + path);
            return;
        }

        //TODO Smt

        try
        {
            peer.deleteFile(path);
        }
        catch(RemoteException e)
        {
            System.out.println("Couldn't delete file " + path);
        }
    }

    public static void manageStorage(int maxStorage)
    {
        if(maxStorage < 0)
        {
            System.out.println("Invalid maximum storage:" + maxStorage);
            return;
        }

        //TODO Smt

        try
        {
            peer.manageStorage();
        }
        catch(RemoteException e)
        {
            System.out.println("Couldn't change local storage to value" + maxStorage);
        }
    }

    public static void getInfo()
    {
        //TODO Smt

        try
        {
            peer.retrieveInfo();
        }
        catch(RemoteException e)
        {
            System.out.println("Couldn't get system info");
        }
    }
}
