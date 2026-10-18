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
            System.out.println("Status: "
                    + (ni.isUp() ? "UP" : "DOWN")
                    + (ni.isLoopback() ? " (loopback)" : ""));

            String mac = formatMac(ni.getHardwareAddress());
            if (mac != null) {
                System.out.println("MAC: " + mac);
            }

            for (InterfaceAddress address : ni.getInterfaceAddresses()) {

                System.out.println(
                        "IP: " + address.getAddress().getHostAddress()
                                + "/" + address.getNetworkPrefixLength()
                );
            }
        }

        Subnet subnet = detectSubnet();
        System.out.println("\nDetected local subnet: "
                + (subnet == null ? "none" : subnet));
    }

    // Finds the subnet of the first active, non-loopback IPv4 interface.
    // Returns null when there is no usable interface.
    public static Subnet detectSubnet() throws SocketException {

        Enumeration<NetworkInterface> interfaces =
                NetworkInterface.getNetworkInterfaces();

        while (interfaces.hasMoreElements()) {

            NetworkInterface ni = interfaces.nextElement();

            if (!ni.isUp() || ni.isLoopback() || ni.isVirtual()) {
                continue;
            }

            for (InterfaceAddress address : ni.getInterfaceAddresses()) {

                if (address.getAddress() instanceof Inet4Address ipv4) {

                    // Very large networks get narrowed to the /24 around our own address
                    int prefix = Math.max(address.getNetworkPrefixLength(), 24);

                    return new Subnet(Subnet.toInt(ipv4), prefix);
                }
            }
        }

        return null;
    }

    private static String formatMac(byte[] mac) {

        if (mac == null || mac.length == 0) {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < mac.length; i++) {
            sb.append(String.format("%02X", mac[i]));
            if (i < mac.length - 1) {
                sb.append(":");
            }
        }
        return sb.toString();
    }
}
