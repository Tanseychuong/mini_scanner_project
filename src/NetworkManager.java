import java.nio.file.Path;
import java.util.*;

class NetworkManager{

    private static final int DEFAULT_TIMEOUT_MS = 500;

    // Throwable because the packet sniffer's native calls (MethodHandle.invokeExact) declare it
    public static void main(String[] args) throws Throwable {

        if (args.length > 0) {
            System.exit(runCommand(args));
        }

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
            System.out.println("5. Traceroute");
            System.out.println("6. Show Active Connections (netstat)");
            System.out.println("7. Show Routing Table");
            System.out.println("8. Add Route");
            System.out.println("9. Packet Sniffer");
            System.out.println("10. Exit");

            System.out.print("\nSelect option: ");

            try {
                int choice = Menu.readInt(scanner);

                switch (choice) {

                    case 1 -> {
                        System.out.println("\n--- Network Information ---");
                        NetworkInfo.showInterfaces();
                    }

                    case 2 -> {
                        System.out.println("\n--- Host Discovery ---");
                        Subnet detected = NetworkInfo.detectSubnet();
                        String defaultCidr = detected == null ? "" : detected.toString();
                        String cidr = Menu.prompt(scanner,
                                "Subnet to scan [" + defaultCidr + "]: ", defaultCidr);
                        if (cidr.isEmpty()) {
                            System.out.println("No subnet detected, please enter one (e.g. 192.168.1.0/24).");
                            break;
                        }
                        Subnet subnet = Subnet.parse(cidr);
                        List<String> hosts = NetworkScanner.scan(subnet, DEFAULT_TIMEOUT_MS);
                        String format = Menu.askReportFormat(scanner);
                        if (format != null) {
                            System.out.println("Saved: " + ReportWriter.saveHosts(hosts, subnet, format));
                        }
                    }

                    case 3 -> {
                        String ip = Menu.prompt(scanner, "Enter IP address to scan: ", "");
                        if (ip.isEmpty()) {
                            System.out.println("No address entered.");
                            break;
                        }
                        String spec = Menu.prompt(scanner,
                                "Ports (common, 1-1024, 22,80,443) [common]: ", "common");
                        List<Integer> ports = PortScanner.parsePorts(spec);
                        System.out.println("\n--- Port Scan ---");
                        List<PortScanner.PortResult> open = PortScanner.scan(ip, ports, DEFAULT_TIMEOUT_MS);
                        String format = Menu.askReportFormat(scanner);
                        if (format != null) {
                            System.out.println("Saved: " + ReportWriter.savePorts(open, ip, format));
                        }
                    }

                    case 4 -> {
                        System.out.println("\n--- WiFi Networks ---");
                        WifiManager.showNetworks(scanner);
                    }

                    case 5 -> {
                        String destination = Menu.prompt(scanner, "Destination host or IP: ", "");
                        if (!destination.isEmpty()) {
                            System.out.println("\n--- Traceroute ---");
                            new Traceroute().trace(destination);
                        }
                    }

                    case 6 -> {
                        System.out.println("\n--- Active Connections ---");
                        new NetstatViewer().showConnections();
                    }

                    case 7 -> {
                        System.out.println("\n--- Routing Table ---");
                        new RouteManager().printRoutingTable();
                    }

                    case 8 -> {
                        System.out.println("\n--- Add Route (requires admin/root) ---");
                        String destination = Menu.prompt(scanner, "Destination network: ", "");
                        String mask = Menu.prompt(scanner, "Subnet mask [255.255.255.0]: ", "255.255.255.0");
                        String gateway = Menu.prompt(scanner, "Gateway: ", "");
                        if (destination.isEmpty() || gateway.isEmpty()) {
                            System.out.println("Destination and gateway are required.");
                        } else if (Menu.confirm(scanner,
                                "Add route " + destination + " mask " + mask + " via " + gateway + "?")) {
                            new RouteManager().addRoute(destination, mask, gateway);
                        }
                    }

                    case 9 -> {
                        System.out.println("\n--- Packet Sniffer (requires admin/root) ---");
                        sniffInteractive(scanner);
                    }

                    case 10 -> {
                        System.out.println("Closing MiniScanner...");
                        scanner.close();
                        return;
                    }

                    default -> System.out.println("Invalid option!");
                }

            } catch (IllegalArgumentException | IllegalStateException e) {
                System.out.println("Error: " + e.getMessage());
            } catch (NoSuchElementException e) {
                // stdin was closed (Ctrl+D / end of piped input)
                System.out.println("\nInput closed, exiting.");
                return;
            }
        }
    }

    // Non-interactive mode, e.g. `java NetworkManager ports 10.0.0.5 1-1024 --report json`
    private static int runCommand(String[] args) throws Throwable {

        List<String> positional = new ArrayList<>();
        String report = null;
        int timeout = DEFAULT_TIMEOUT_MS;
        String filter = "";
        String write = null;
        String read = null;
        int count = 50;
        int seconds = 30;
        boolean list = false;

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--report" -> report = i + 1 < args.length ? args[++i].toLowerCase() : null;
                case "--timeout" -> {
                    try {
                        timeout = Integer.parseInt(i + 1 < args.length ? args[++i] : "");
                    } catch (NumberFormatException e) {
                        System.out.println("--timeout must be a number of milliseconds");
                        return 2;
                    }
                }
                case "--filter" -> filter = i + 1 < args.length ? args[++i] : "";
                case "--write" -> write = i + 1 < args.length ? args[++i] : null;
                case "--read" -> read = i + 1 < args.length ? args[++i] : null;
                case "--list" -> list = true;
                case "--count", "--seconds" -> {
                    String option = args[i];
                    try {
                        int value = Integer.parseInt(i + 1 < args.length ? args[++i] : "");
                        if (option.equals("--count")) {
                            count = value;
                        } else {
                            seconds = value;
                        }
                    } catch (NumberFormatException e) {
                        System.out.println(option + " must be a number");
                        return 2;
                    }
                }
                default -> positional.add(args[i]);
            }
        }

        if (report != null && !report.equals("json") && !report.equals("csv")) {
            System.out.println("--report must be json or csv");
            return 2;
        }

        String command = positional.isEmpty() ? "help" : positional.get(0);

        try {
            switch (command) {

                case "info" -> NetworkInfo.showInterfaces();

                case "discover" -> {
                    Subnet subnet = positional.size() > 1
                            ? Subnet.parse(positional.get(1))
                            : NetworkInfo.detectSubnet();
                    if (subnet == null) {
                        System.out.println("No subnet detected, pass one: discover 192.168.1.0/24");
                        return 1;
                    }
                    List<String> hosts = NetworkScanner.scan(subnet, timeout);
                    if (report != null) {
                        Path file = ReportWriter.saveHosts(hosts, subnet, report);
                        System.out.println("Saved: " + file);
                    }
                }

                case "ports" -> {
                    if (positional.size() < 2) {
                        System.out.println("Usage: ports <host> [common|1-1024|22,80,443]");
                        return 2;
                    }
                    String host = positional.get(1);
                    List<Integer> ports = PortScanner.parsePorts(
                            positional.size() > 2 ? positional.get(2) : "common");
                    List<PortScanner.PortResult> open = PortScanner.scan(host, ports, timeout);
                    if (report != null) {
                        Path file = ReportWriter.savePorts(open, host, report);
                        System.out.println("Saved: " + file);
                    }
                }

                case "trace" -> {
                    if (positional.size() < 2) {
                        System.out.println("Usage: trace <host>");
                        return 2;
                    }
                    new Traceroute().trace(positional.get(1));
                }

                case "netstat" -> new NetstatViewer().showConnections();

                case "routes" -> new RouteManager().printRoutingTable();

                case "sniff" -> {
                    PacketSniffer sniffer = new PacketSniffer();
                    if (list) {
                        sniffer.listInterfaces();
                    } else if (read != null) {
                        sniffer.read(read, filter, count);
                    } else if (positional.size() < 2) {
                        System.out.println("Usage: sniff <interface> | sniff --list | sniff --read <file>");
                        return 2;
                    } else {
                        sniffer.capture(positional.get(1), filter, count, seconds, write);
                    }
                }

                default -> {
                    System.out.println("""
                            Usage: java NetworkManager [command] [options]

                            Commands:
                              info                         Show network interfaces
                              discover [cidr]              Find live hosts (default: detected subnet)
                              ports <host> [ports]         Scan ports: common | 1-1024 | 22,80,443
                              trace <host>                 Traceroute to a host
                              netstat                      Show active connections
                              routes                       Show the routing table
                              sniff <interface>            Capture packets (needs admin/root + libpcap/Npcap)
                              sniff --list                 List capture interfaces
                              sniff --read <file.pcap>     Decode a saved capture

                            Options:
                              --report json|csv            Save results to the reports/ folder
                              --timeout <ms>               Connection timeout (default 500)
                              --filter "<bpf>"             Capture filter, e.g. "tcp port 80" or "icmp"
                              --count <n>                  Stop after n packets (default 50, 0 = no limit)
                              --seconds <s>                Stop after s seconds (default 30, 0 = no limit)
                              --write <file.pcap>          Save the capture for Wireshark

                            Run with no arguments for the interactive menu.""");
                    return command.equals("help") ? 0 : 2;
                }
            }
        } catch (IllegalArgumentException e) {
            System.out.println("Error: " + e.getMessage());
            return 2;
        } catch (IllegalStateException e) {
            System.out.println("Error: " + e.getMessage());
            return 1;
        }

        return 0;
    }

    private static void sniffInteractive(Scanner scanner) throws Throwable {

        PacketSniffer sniffer = new PacketSniffer();
        System.out.println("Using " + sniffer.version() + "\n");

        List<String> interfaces = sniffer.listInterfaces();
        if (interfaces.isEmpty()) {
            return;
        }

        String choice = Menu.prompt(scanner, "\nInterface number or name [1]: ", "1");
        String device = choice;
        try {
            int index = Integer.parseInt(choice);
            if (index < 1 || index > interfaces.size()) {
                System.out.println("Invalid interface number.");
                return;
            }
            device = interfaces.get(index - 1);
        } catch (NumberFormatException e) {
            // a name was typed, use it as-is
        }

        String filter = Menu.prompt(scanner, "Filter, e.g. tcp port 80 (blank = everything): ", "");
        int count = Integer.parseInt(Menu.prompt(scanner, "Max packets [50]: ", "50"));
        int seconds = Integer.parseInt(Menu.prompt(scanner, "Max seconds [30]: ", "30"));
        String save = Menu.prompt(scanner, "Save to .pcap file (blank = no): ", "");

        sniffer.capture(device, filter, count, seconds, save.isEmpty() ? null : save);
    }
}
