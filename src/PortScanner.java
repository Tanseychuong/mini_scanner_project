import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;

public class PortScanner {

    public record PortResult(String host, int port, String service, String banner) {}

    private static final Map<Integer, String> SERVICES = new TreeMap<>(Map.ofEntries(
            Map.entry(21, "FTP"),
            Map.entry(22, "SSH"),
            Map.entry(23, "Telnet"),
            Map.entry(25, "SMTP"),
            Map.entry(53, "DNS"),
            Map.entry(80, "HTTP"),
            Map.entry(110, "POP3"),
            Map.entry(135, "MS-RPC"),
            Map.entry(139, "NetBIOS"),
            Map.entry(143, "IMAP"),
            Map.entry(443, "HTTPS"),
            Map.entry(445, "SMB"),
            Map.entry(1433, "MSSQL"),
            Map.entry(3306, "MySQL/MariaDB"),
            Map.entry(3389, "RDP"),
            Map.entry(5432, "PostgreSQL"),
            Map.entry(5900, "VNC"),
            Map.entry(6379, "Redis"),
            Map.entry(8080, "HTTP-Alt"),
            Map.entry(8443, "HTTPS-Alt"),
            Map.entry(27017, "MongoDB")
    ));

    private static final Set<Integer> HTTP_PORTS = Set.of(80, 8000, 8080, 8888);

    private static final int THREADS = 100;

    public static List<Integer> commonPorts() {
        return new ArrayList<>(SERVICES.keySet());
    }

    // Parses "common", "22", "1-1024" or "22,80,8000-8100"
    public static List<Integer> parsePorts(String spec) {

        if (spec.isBlank() || spec.trim().equalsIgnoreCase("common")) {
            return commonPorts();
        }

        TreeSet<Integer> ports = new TreeSet<>();

        for (String part : spec.split(",")) {
            part = part.trim();
            try {
                if (part.contains("-")) {
                    String[] range = part.split("-", 2);
                    int start = Integer.parseInt(range[0].trim());
                    int end = Integer.parseInt(range[1].trim());
                    for (int p = start; p <= end; p++) {
                        ports.add(checkPort(p));
                    }
                } else {
                    ports.add(checkPort(Integer.parseInt(part)));
                }
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid port: " + part);
            }
        }

        return new ArrayList<>(ports);
    }

    private static int checkPort(int port) {
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("Port out of range: " + port);
        }
        return port;
    }

    public static String serviceName(int port) {
        return SERVICES.getOrDefault(port, "unknown");
    }

    // Scans the ports in parallel and returns only the open ones
    public static List<PortResult> scan(String host, List<Integer> ports, int timeoutMs)
            throws InterruptedException {

        System.out.println("Scanning " + host + " (" + ports.size() + " ports)...\n");

        ExecutorService pool = Executors.newFixedThreadPool(Math.min(THREADS, ports.size()));
        List<Future<PortResult>> futures = new ArrayList<>();

        for (int port : ports) {
            futures.add(pool.submit(() -> probe(host, port, timeoutMs)));
        }

        List<PortResult> open = new ArrayList<>();

        for (Future<PortResult> future : futures) {
            try {
                PortResult result = future.get();
                if (result != null) {
                    open.add(result);
                }
            } catch (ExecutionException e) {
                // skip ports that failed unexpectedly
            }
        }

        pool.shutdown();

        if (open.isEmpty()) {
            System.out.println("No open ports found.");
        } else {
            System.out.println("OPEN PORTS:\n");
            for (PortResult r : open) {
                System.out.printf("%-6d %-14s %s%n", r.port(), r.service(),
                        r.banner().isEmpty() ? "" : r.banner());
            }
        }

        System.out.println("\n" + open.size() + " open, "
                + (ports.size() - open.size()) + " closed/filtered");

        return open;
    }

    // Returns a result if the port is open, null otherwise
    private static PortResult probe(String host, int port, int timeoutMs) {

        try (Socket socket = new Socket()) {

            socket.connect(new InetSocketAddress(host, port), timeoutMs);

            return new PortResult(host, port, serviceName(port), grabBanner(socket, host, port));

        } catch (IOException e) {
            return null;
        }
    }

    // Reads whatever the service announces first (SSH, FTP, SMTP, Redis errors...).
    // HTTP servers wait for the client, so we send a HEAD request and keep the Server header.
    private static String grabBanner(Socket socket, String host, int port) {

        try {
            socket.setSoTimeout(1500);

            if (HTTP_PORTS.contains(port)) {
                OutputStream out = socket.getOutputStream();
                out.write(("HEAD / HTTP/1.0\r\nHost: " + host + "\r\n\r\n")
                        .getBytes(StandardCharsets.US_ASCII));
                out.flush();
            }

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(socket.getInputStream(), StandardCharsets.US_ASCII));

            String first = reader.readLine();
            if (first == null) {
                return "";
            }

            if (first.startsWith("HTTP/")) {
                String line;
                while ((line = reader.readLine()) != null && !line.isEmpty()) {
                    if (line.toLowerCase().startsWith("server:")) {
                        return first + " | " + line;
                    }
                }
            }

            return clean(first);

        } catch (IOException e) {
            return "";
        }
    }

    private static String clean(String banner) {
        String printable = banner.replaceAll("[^\\x20-\\x7E]", "").trim();
        return printable.length() > 80 ? printable.substring(0, 80) + "..." : printable;
    }
}
