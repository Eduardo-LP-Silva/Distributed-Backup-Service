package app;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface BackupService extends Remote
{
    public void backupFile() throws RemoteException;
    public void restoreFile() throws RemoteException;
    public void deleteFile() throws RemoteException;
    public void manageStorage() throws RemoteException;
    public void retrieveInfo() throws RemoteException;
}