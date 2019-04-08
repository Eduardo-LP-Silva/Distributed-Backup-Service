package protocol;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Hashtable;
import java.util.Set;

public abstract class ProtocolThread extends Thread
{
    protected static Hashtable<String, int[]> backedUpChuncks; //fileID-ChunckNo -> {replication_expected, actual_replication}
    
    public static void setRecordsTable(Hashtable<String, int[]> newTable)
    {
        backedUpChuncks = newTable;
    }

    public static void saveTableToDisk()
    {
        try
        {
            FileOutputStream fos = new FileOutputStream("backedChuncks.ser");
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

    public static void printTable()
    {
        Set<String> keys = backedUpChuncks.keySet();

        for(String key: keys)
        {
            int[] values = backedUpChuncks.get(key);

            System.out.println(key + "-> [" + values[0] + "," + values[1] + "]");
        }
            
    }
}