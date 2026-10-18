import java.io.*;
import java.util.Scanner;

public class WifiManager {

    // Takes the menu's Scanner: opening a second Scanner on System.in can swallow buffered input
    public static void showNetworks(Scanner input)
            throws IOException, InterruptedException {

        if (!CommandRunner.isWindows()) {
            // nmcli is NetworkManager's CLI, available on most desktop Linux distros
            System.out.println("Non-Windows system detected, using nmcli.");
            CommandRunner.run("nmcli", "device", "wifi", "list");
            return;
        }

        System.out.println("Select process to scanner");
        System.out.println("1. See available networks");
        System.out.println("2. See previously connected networks");
        System.out.println("3. See available networks with key clear (show passwords)");

        int choice = Menu.readInt(input);

        switch (choice) {
            case 1 -> CommandRunner.run("netsh", "wlan", "show", "networks");
            case 2 -> CommandRunner.run("netsh", "wlan", "show", "profiles");
            case 3 -> {
                System.out.println("Enter the name of the WIFI you want to see it's passkey");
                // nextLine so names with spaces work; passing it as its own argument
                // stops the name from being interpreted as extra netsh options
                String name = input.nextLine().trim();
                CommandRunner.run("netsh", "wlan", "show", "profile", "name=" + name, "key=clear");
            }
            default -> System.out.println("Invalid option!");
        }
    }
}
