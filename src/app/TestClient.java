package app;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.io.File;

public class TestClient
{
    private static BackupService peer;
    private static boolean enh = false;

    public static void main(String args[])
    {
        if(args.length < 2)
        {
            System.out.println("Wrong number of arguments: " + args.length); 
            System.out.println("Usage: TestClient <peer_ap> <operation> <opnd_1> [<opnd_2>]");
            return;
        }

        if(!connectToBackupService(args[0]))
            return;

        switch (args[1])
        {
            case "BACKUPENH":
                enh = true;

            case "BACKUP":

                if (args.length < 4)
                {
                    System.out.println("Wrong number of arguments: " + args.length);
                    System.out.println("Usage: TestClient <peer_ap> <operation> <opnd_1> <opnd_2>");
                    return;
                }

                try
                {
                    peer.backupFile(args[2], Integer.parseInt(args[3]), enh);
                }
                catch(RemoteException e)
                {
                    System.out.println("Couldn't back up file " + args[2] + "with replication " + args[3]);
                }

                break;

            case "RESTOREENH":
                System.out.println("Restore enhancement not implemented");
                break;

            case "RESTORE":

                if (args.length < 3)
                {
                    System.out.println("Wrong number of arguments: " + args.length);
                    System.out.println("Usage: TestClient <peer_ap> <operation> <opnd_1> <opnd_2>");
                    return;
                }

                try
                {
                    peer.restoreFile(args[2]);
                }
                catch(RemoteException e)
                {
                    System.out.println("Couldn't restore file " + args[2]);
                }
                break;

            case "DELETEENH":
                System.out.println("Delete enhancement not implemented");
                break;

            case "DELETE":

                if (args.length < 3)
                {
                    System.out.println("Wrong number of arguments: " + args.length);
                    System.out.println("Usage: TestClient <peer_ap> <operation> <opnd_1> <opnd_2>");
                    return;
                }

                try
                {
                    peer.deleteFile(args[2]);
                }
                catch(RemoteException e)
                {
                    System.out.println("Couldn't delete file " + args[2]);
                }
                break;

            case "RECLAIM":

                if (args.length < 3)
                {
                    System.out.println("Wrong number of arguments: " + args.length);
                    System.out.println("Usage: TestClient <peer_ap> <operation> <opnd_1> <opnd_2>");
                    return;
                }

                manageStorage(Integer.parseInt(args[2]));
                break;

            case "STATE":
                getInfo();
                break;

            default:
                System.out.println("Unrecognized operation: " + args[1]);

        }
    }

    public static boolean connectToBackupService(String remoteObject)
    {
        try
        {
            Registry registry = LocateRegistry.getRegistry("localhost");
            peer = (BackupService) registry.lookup(remoteObject);
        }
        catch(Exception e)
        {
            System.out.println("Couldn't connect to server with remote object " + remoteObject);
            return false;
        }

        return true;
    }

    public static void restoreFile(String path)
    {
        File file = new File(path);

        if(!file.exists())
        {
            System.out.println("Couldn't find file to restore: " + path);
            return;
        }

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

        try
        {
            peer.manageStorage(maxStorage);
        }
        catch(RemoteException e)
        {
            System.out.println("Couldn't change local storage to value" + maxStorage);
        }
    }

    public static void getInfo()
    {
        try
        {
           String info = peer.retrieveInfo();

           System.out.println(info);
        }
        catch(RemoteException e)
        {
            System.out.println("Couldn't get system info");
        }
    }
}
