import java.net.*;
import java.util.*;

// An IPv4 network in CIDR form, e.g. 192.168.1.0/24
public class Subnet {

    // Refuse anything bigger than a /20 so a typo can't start a 16 million host scan
    public static final int MIN_PREFIX = 20;

    private final int network;
    private final int prefix;

    public Subnet(int address, int prefix) {

        if (prefix < MIN_PREFIX || prefix > 32) {
            throw new IllegalArgumentException(
                    "Prefix /" + prefix + " not supported (use /" + MIN_PREFIX + " to /32)");
        }

        this.prefix = prefix;
        this.network = address & mask();
    }

    // Parses "10.0.0.0/24", or a single IP which is treated as /32
    public static Subnet parse(String cidr) {

        String[] parts = cidr.trim().split("/");

        int prefix = 32;
        if (parts.length == 2) {
            try {
                prefix = Integer.parseInt(parts[1]);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid prefix: " + parts[1]);
            }
        } else if (parts.length != 1) {
            throw new IllegalArgumentException("Invalid CIDR: " + cidr);
        }

        return new Subnet(toInt(parts[0]), prefix);
    }

    public static int toInt(String ip) {

        String[] octets = ip.trim().split("\\.");

        if (octets.length != 4) {
            throw new IllegalArgumentException("Invalid IPv4 address: " + ip);
        }

        int value = 0;
        for (String octet : octets) {
            int n;
            try {
                n = Integer.parseInt(octet);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid IPv4 address: " + ip);
            }
            if (n < 0 || n > 255) {
                throw new IllegalArgumentException("Invalid IPv4 address: " + ip);
            }
            value = (value << 8) | n;
        }
        return value;
    }

    public static int toInt(Inet4Address address) {
        byte[] b = address.getAddress();
        return ((b[0] & 0xFF) << 24) | ((b[1] & 0xFF) << 16) | ((b[2] & 0xFF) << 8) | (b[3] & 0xFF);
    }

    public static String toIp(int value) {
        return ((value >>> 24) & 0xFF) + "." + ((value >>> 16) & 0xFF) + "."
                + ((value >>> 8) & 0xFF) + "." + (value & 0xFF);
    }

    private int mask() {
        return prefix == 0 ? 0 : -1 << (32 - prefix);
    }

    // Usable host addresses (skips network and broadcast addresses for /30 and larger)
    public List<String> hosts() {

        List<String> hosts = new ArrayList<>();

        if (prefix >= 31) {
            long size = 1L << (32 - prefix);
            for (long i = 0; i < size; i++) {
                hosts.add(toIp(network + (int) i));
            }
            return hosts;
        }

        int broadcast = network | ~mask();
        for (int ip = network + 1; ip != broadcast; ip++) {
            hosts.add(toIp(ip));
        }
        return hosts;
    }

    @Override
    public String toString() {
        return toIp(network) + "/" + prefix;
    }
}
