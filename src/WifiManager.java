import java.io.*;
import java.util.NoSuchElementException;
import java.util.Scanner;

public class WifiManager {


    public static void showNetworks()
            throws Exception {
        System.out.println("Select process to scanner");
        System.out.println("1. See available networks");
        System.out.println("2. See previously connected networks");
        System.out.println("3. See available networks with key clear (show passwords)");
        //System.out.println("4. See available networks");

        Scanner input = new Scanner(System.in);
        int choice = input.nextInt();
        if (choice == 1) {// Cmd to execute on the terminal
            Process process = Runtime.getRuntime().exec("netsh wlan show networks");

            //Reading from the terminal with buffer reader
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            String line;
            //printing the results from the executions
            while ((line = reader.readLine()) != null) {
                System.out.println( line);
            }}
        if (choice == 2){
            Process process1 = Runtime.getRuntime().exec("netsh wlan show profiles");
            BufferedReader reader1 = new BufferedReader(new InputStreamReader(process1.getInputStream()));
                String line1;
                while((line1 = reader1.readLine()) != null){
                    System.out.println(line1);
            }}

        if (choice == 3){
            System.out.println("Enter the name of the WIFI you want to see it's passkey");
            try {
                String name = input.next();
                Process process1 = Runtime.getRuntime().exec("netsh wlan show profile " + name + " key=clear");
                BufferedReader reader1 = new BufferedReader(new InputStreamReader(process1.getInputStream()));

            String line2;
            while((line2 = reader1.readLine()) != null){
                System.out.println(line2);
            }}catch (RuntimeException e) {
                System.out.println("Unexpected behaviour occurred while scanner the networks");
            }

            }
        }
        }
