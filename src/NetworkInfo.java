import java.net.*;
import java.util.*;

public class NetworkInfo {

    public static void showInterfaces() throws Exception {

        Enumeration<NetworkInterface> interfaces =
                NetworkInterface.getNetworkInterfaces();

        while (interfaces.hasMoreElements()) {

            NetworkInterface ni = interfaces.nextElement();

            System.out.println("\nInterface: " + ni.getName());
            System.out.println("Description: "
                    + ni.getDisplayName());

            Enumeration<InetAddress> addresses =
                    ni.getInetAddresses();

            while (addresses.hasMoreElements()) {

                InetAddress address =
                        addresses.nextElement();

                System.out.println(
                        "IP: " + address.getHostAddress()
                );
            }
        }
    }
}