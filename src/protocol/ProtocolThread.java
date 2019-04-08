package protocol;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Hashtable;
import java.util.Set;
import java.util.ArrayList;

public abstract class ProtocolThread extends Thread
{
    protected static Hashtable<String, int[]> backedUpChuncks; //fileID-ChunckNo -> {replication_expected, size}
    protected static Hashtable<String, ArrayList<Integer>> chuncksStorage; //fileID-ChunckNo -> {1, 2, ...}

    public static void setBackupUpChuncksTable(Hashtable<String, int[]> newTable)
    {
        backedUpChuncks = newTable;
    }

    public static void setChuncksStorageTable(Hashtable<String, ArrayList<Integer>> newTable)
    {
        chuncksStorage = newTable;
    }

    public static void saveTableToDisk(int table)
    {
        FileOutputStream fos;

        try
        {
            if(table == 1)
                fos = new FileOutputStream("backedChuncks.ser");
            else
                if(table == 2)
                    fos = new FileOutputStream("chuncksStorage.ser");
                else
                {
                    System.out.println("Invalid table to save to disk");
                    return;
                }

            ObjectOutputStream oos = new ObjectOutputStream(fos);

            oos.writeObject(backedUpChuncks);
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
    }

    public static void printChuncksStorageTable()
    {
        Set<String> keys = chuncksStorage.keySet();

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
    }

    public static Hashtable<String, int[]> getLocalChuncksTable()
    {
        return backedUpChuncks;
    }

    public static Hashtable<String, ArrayList<Integer>> getChuncksStorageTable()
    {
        return chuncksStorage;
    }
}