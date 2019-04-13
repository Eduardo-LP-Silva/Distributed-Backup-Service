package app;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface BackupService extends Remote
{
    public void backupFile(String path, int replication, boolean enh) throws RemoteException;
    public void restoreFile(String path) throws RemoteException;
    public void deleteFile(String path) throws RemoteException;
    public void manageStorage(int maxSpace) throws RemoteException;
    public String retrieveInfo() throws RemoteException;
}
