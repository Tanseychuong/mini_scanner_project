import java.io.IOException;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class NetworkScanner {

    // Ports used as a fallback liveness check when ICMP is blocked or unavailable
    private static final int[] PROBE_PORTS = {80, 443, 22, 445, 139, 3389};

    private static final int THREADS = 64;

    // Scans every host in the subnet in parallel and returns the reachable ones in order
    public static List<String> scan(Subnet subnet, int timeoutMs) throws InterruptedException {

        List<String> candidates = subnet.hosts();

        System.out.println("Scanning " + subnet + " (" + candidates.size()
                + " hosts, " + THREADS + " threads)...\n");

        ExecutorService pool = Executors.newFixedThreadPool(THREADS);
        Map<String, Future<Boolean>> results = new LinkedHashMap<>();

        for (String host : candidates) {
            results.put(host, pool.submit(() -> isAlive(host, timeoutMs)));
        }

        List<String> hosts = new ArrayList<>();

        for (Map.Entry<String, Future<Boolean>> entry : results.entrySet()) {
            try {
                if (entry.getValue().get()) {
                    hosts.add(entry.getKey());
                    System.out.println(entry.getKey() + " reachable" + hostname(entry.getKey()));
                }
            } catch (ExecutionException e) {
                // An unexpected error on one host shouldn't stop the whole scan
            }
        }

        pool.shutdown();

        System.out.println("\nHosts found: " + hosts.size());
        return hosts;
    }

    static boolean isAlive(String host, int timeoutMs) {

        try {
            if (InetAddress.getByName(host).isReachable(timeoutMs)) {
                return true;
            }
        } catch (IOException e) {
            // fall through to the TCP probes
        }

        for (int port : PROBE_PORTS) {
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress(host, port), timeoutMs);
                return true;
            } catch (ConnectException e) {
                // "Connection refused" means something answered, so the host is up
                return true;
            } catch (IOException e) {
                // timeout or unreachable, try the next port
            }
        }

        return false;
    }

    private static String hostname(String ip) {

        try {
            String name = InetAddress.getByName(ip).getCanonicalHostName();
            return name.equals(ip) ? "" : "  (" + name + ")";
        } catch (UnknownHostException e) {
            return "";
        }
    }
}
