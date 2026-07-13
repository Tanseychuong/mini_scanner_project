import javax.lang.model.util.ElementScanner6;
import java.util.Scanner;

class NetworkManager{
    public static void main(String[] args) throws Exception {


        Scanner scanner = new Scanner(System.in);

        Banner.show();

        while (true) {

            System.out.println("\n==============================");
            System.out.println("        MiniScanner v1.0");
            System.out.println("==============================");

            System.out.println("1. Show Network Information");
            System.out.println("2. Discover Network Hosts");
            System.out.println("3. Scan Ports");
            System.out.println("4. Show Available WiFi Networks");
            System.out.println("5. Exit");

            System.out.print("\nSelect option: ");

            int choice = scanner.nextInt();
            scanner.nextLine();


            switch(choice) {


                case 1:
                    System.out.println("\n--- Network Information ---");
                    NetworkInfo.showInterfaces();
                    break;


                case 2:
                    System.out.println("\n--- Host Discovery ---");
                    NetworkScanner.scan();
                    break;


                case 3:

                    System.out.print("Enter IP address to scan: ");

                    String ip = scanner.nextLine();

                    System.out.println("\n--- Port Scan ---");

                    PortScanner.scan(ip);
                    break;


                case 4:

                    System.out.println("\n--- WiFi Networks ---");
                    WifiManager.showNetworks();
                    break;


                case 5:

                    System.out.println("Closing MiniScanner...");
                    scanner.close();
                    return;


                default:
                    System.out.println("Invalid option!");
            }
        }
    }
}