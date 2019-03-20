package app;

import java.util.Scanner;

public class Server
{
    public static void main(String args[])
    {
        menu();
    }

    public Server()
    {

    }

    public static void menu()
    {
        Scanner scanner = new Scanner(System.in);
        int option;

        System.out.println("+----------------------------------------------+");
        System.out.println("|                 Backup It Up                 |");
        System.out.println("+----------------------------------------------+");
        System.out.println("| 1 - Backup File  | 2 - Restore File          |");
        System.out.println("+----------------------------------------------+");
        System.out.println("| 3 - Delete File  | 4 - Manage Local Storage  |");
        System.out.println("+----------------------------------------------+");
        System.out.println("| 5 - Retrieve local service state information |");
        System.out.println("+----------------------------------------------+");
        
        do
        {
            option = scanner.nextInt();
            
            switch(option)
            {
                case 1:
                    backupFile();
                    break;

                case 2:
                    restoreFile();
                    break;

                case 3:
                    deleteFile();
                    break;

                case 4:
                    manageStorage();
                    break;

                case 5:
                    retrieveInfo();
                    break;

                default:
                    System.out.println("Invalid option! Try again");
            }
        }
        while(option < 1 || option > 5);
    }

    public static void backupFile()
    {
        Scanner scanner = new Scanner(System.in);

        System.out.println("Usage: <file path> <replication degree>");
        
    }

    public static void restoreFile()
    {

    }

    public static void deleteFile()
    {

    }

    public static void manageStorage()
    {

    }

    public static void retrieveInfo()
    {

    }
}