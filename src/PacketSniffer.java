import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.nio.file.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static java.lang.foreign.ValueLayout.*;

// Captures packets through libpcap (Linux/macOS) or Npcap (Windows) using Java's
// Foreign Function & Memory API, so no third-party jars are needed. The Ethernet,
// IP, TCP, UDP, ICMP and ARP headers are decoded by hand below.
//
// Run the JVM with --enable-native-access=ALL-UNNAMED to silence the native access warning.
public class PacketSniffer {

    // libpcap link-layer types we know how to decode
    private static final int DLT_NULL = 0;        // BSD/Npcap loopback
    private static final int DLT_EN10MB = 1;      // Ethernet
    private static final int DLT_RAW = 12;        // raw IP (12 on Linux, 14 on BSD)
    private static final int DLT_RAW_BSD = 14;
    private static final int LINKTYPE_RAW = 101;
    private static final int DLT_LINUX_SLL = 113; // Linux "any" pseudo-device

    private static final int ERRBUF_SIZE = 256;
    private static final int SNAPLEN = 65535;
    private static final int READ_TIMEOUT_MS = 500;
    private static final int PCAP_NETMASK_UNKNOWN = 0xFFFFFFFF;

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    // struct pcap_pkthdr { struct timeval ts; bpf_u_int32 caplen; bpf_u_int32 len; }
    // timeval holds two C longs: 4 bytes each on Windows, 8 bytes each on 64-bit Linux/macOS
    private static final boolean WINDOWS = CommandRunner.isWindows();
    private static final long TV_USEC_SIZE = WINDOWS ? 4 : 8;
    private static final long CAPLEN_OFFSET = WINDOWS ? 8 : 16;
    private static final long LEN_OFFSET = CAPLEN_OFFSET + 4;

    private final MethodHandle findAllDevs, freeAllDevs, create, setSnaplen, setPromisc, setTimeout,
            setImmediateMode, activate, setNonblock, statusToStr, openOffline, close,
            nextEx, datalink, compile, setFilter, freeCode, getErr, libVersion,
            dumpOpen, dump, dumpClose;

    private final Map<String, Integer> stats = new TreeMap<>();

    private volatile boolean running;

    public PacketSniffer() {

        SymbolLookup pcap = loadLibrary();
        Linker linker = Linker.nativeLinker();

        findAllDevs = function(linker, pcap, "pcap_findalldevs", JAVA_INT, ADDRESS, ADDRESS);
        freeAllDevs = function(linker, pcap, "pcap_freealldevs", null, ADDRESS);
        create = function(linker, pcap, "pcap_create", ADDRESS, ADDRESS, ADDRESS);
        setSnaplen = function(linker, pcap, "pcap_set_snaplen", JAVA_INT, ADDRESS, JAVA_INT);
        setPromisc = function(linker, pcap, "pcap_set_promisc", JAVA_INT, ADDRESS, JAVA_INT);
        setTimeout = function(linker, pcap, "pcap_set_timeout", JAVA_INT, ADDRESS, JAVA_INT);
        setImmediateMode = function(linker, pcap, "pcap_set_immediate_mode", JAVA_INT, ADDRESS, JAVA_INT);
        activate = function(linker, pcap, "pcap_activate", JAVA_INT, ADDRESS);
        setNonblock = function(linker, pcap, "pcap_setnonblock", JAVA_INT, ADDRESS, JAVA_INT, ADDRESS);
        statusToStr = function(linker, pcap, "pcap_statustostr", ADDRESS, JAVA_INT);
        openOffline = function(linker, pcap, "pcap_open_offline", ADDRESS, ADDRESS, ADDRESS);
        close = function(linker, pcap, "pcap_close", null, ADDRESS);
        nextEx = function(linker, pcap, "pcap_next_ex", JAVA_INT, ADDRESS, ADDRESS, ADDRESS);
        datalink = function(linker, pcap, "pcap_datalink", JAVA_INT, ADDRESS);
        compile = function(linker, pcap, "pcap_compile", JAVA_INT, ADDRESS, ADDRESS, ADDRESS, JAVA_INT, JAVA_INT);
        setFilter = function(linker, pcap, "pcap_setfilter", JAVA_INT, ADDRESS, ADDRESS);
        freeCode = function(linker, pcap, "pcap_freecode", null, ADDRESS);
        getErr = function(linker, pcap, "pcap_geterr", ADDRESS, ADDRESS);
        libVersion = function(linker, pcap, "pcap_lib_version", ADDRESS);
        dumpOpen = function(linker, pcap, "pcap_dump_open", ADDRESS, ADDRESS, ADDRESS);
        dump = function(linker, pcap, "pcap_dump", null, ADDRESS, ADDRESS, ADDRESS);
        dumpClose = function(linker, pcap, "pcap_dump_close", null, ADDRESS);
    }

    private static SymbolLookup loadLibrary() {

        Arena arena = Arena.global();

        if (WINDOWS) {
            Path npcap = Paths.get(System.getenv().getOrDefault("SystemRoot", "C:\\Windows"),
                    "System32", "Npcap");
            try {
                // wpcap.dll depends on Packet.dll from the same folder, so load that first
                SymbolLookup.libraryLookup(npcap.resolve("Packet.dll"), arena);
                return SymbolLookup.libraryLookup(npcap.resolve("wpcap.dll"), arena);
            } catch (IllegalArgumentException e) {
                throw new IllegalStateException(
                        "Npcap not found. Install it from https://npcap.com to use the packet sniffer.");
            }
        }

        for (String name : List.of("libpcap.so.1", "libpcap.so", "libpcap.dylib", "libpcap.A.dylib")) {
            try {
                return SymbolLookup.libraryLookup(name, arena);
            } catch (IllegalArgumentException e) {
                // try the next name
            }
        }

        throw new IllegalStateException(
                "libpcap not found. Install it (e.g. 'apt install libpcap0.8' or 'apk add libpcap').");
    }

    private static MethodHandle function(Linker linker, SymbolLookup lib, String name,
                                         MemoryLayout result, MemoryLayout... args) {

        MemorySegment symbol = lib.find(name)
                .orElseThrow(() -> new IllegalStateException("Missing libpcap function: " + name));

        FunctionDescriptor descriptor = result == null
                ? FunctionDescriptor.ofVoid(args)
                : FunctionDescriptor.of(result, args);

        return linker.downcallHandle(symbol, descriptor);
    }

    // Native strings come back as zero-length segments; widen them so they can be read
    private static String cString(MemorySegment pointer) {
        return pointer.equals(MemorySegment.NULL) ? "" : pointer.reinterpret(Long.MAX_VALUE).getString(0);
    }

    public String version() throws Throwable {
        return cString((MemorySegment) libVersion.invokeExact());
    }

    // Prints the capture interfaces with a number and returns their names in the same order
    public List<String> listInterfaces() throws Throwable {

        List<String> names = new ArrayList<>();

        try (Arena arena = Arena.ofConfined()) {

            MemorySegment devsPtr = arena.allocate(ADDRESS);
            MemorySegment errbuf = arena.allocate(ERRBUF_SIZE);

            if ((int) findAllDevs.invokeExact(devsPtr, errbuf) != 0) {
                throw new IllegalStateException(errbuf.getString(0));
            }

            // struct pcap_if { pcap_if *next; char *name; char *description; pcap_addr *addresses; u_int flags; }
            MemorySegment first = devsPtr.get(ADDRESS, 0);
            MemorySegment dev = first;
            long ptr = ADDRESS.byteSize();
            int index = 1;

            if (dev.equals(MemorySegment.NULL)) {
                System.out.println("No capture interfaces found (try running as administrator/root).");
                return names;
            }

            while (!dev.equals(MemorySegment.NULL)) {
                MemorySegment entry = dev.reinterpret(5 * ptr);
                String name = cString(entry.get(ADDRESS, ptr));
                String description = cString(entry.get(ADDRESS, 2 * ptr));
                System.out.printf("%2d. %-20s %s%n", index++, name, description);
                names.add(name);
                dev = entry.get(ADDRESS, 0);
            }

            freeAllDevs.invokeExact(first);
        }

        return names;
    }

    // Live capture. Stops after `count` packets or `seconds` seconds, whichever comes first
    // (0 means no limit). `filter` is a BPF expression such as "tcp port 80"; `writeTo`
    // optionally saves the raw packets as a .pcap file that Wireshark can open.
    public void capture(String device, String filter, int count, int seconds, String writeTo)
            throws Throwable {

        try (Arena arena = Arena.ofConfined()) {

            MemorySegment handle = openLive(arena, device);

            String banner = "Capturing on " + device
                    + (filter.isEmpty() ? "" : " (filter: " + filter + ")")
                    + (count > 0 ? ", " + count + " packets max" : "")
                    + (seconds > 0 ? ", " + seconds + "s max" : "") + "...\n";

            run(arena, handle, banner, filter, count, seconds, writeTo);
        }
    }

    private MemorySegment openLive(Arena arena, String device) throws Throwable {

        MemorySegment errbuf = arena.allocate(ERRBUF_SIZE);
        MemorySegment handle = (MemorySegment) create.invokeExact(arena.allocateFrom(device), errbuf);

        if (handle.equals(MemorySegment.NULL)) {
            throw new IllegalStateException(errbuf.getString(0));
        }

        int ignored = (int) setSnaplen.invokeExact(handle, SNAPLEN);
        ignored = (int) setPromisc.invokeExact(handle, 1);
        ignored = (int) setTimeout.invokeExact(handle, READ_TIMEOUT_MS);
        // deliver each packet as soon as it arrives instead of buffering them
        ignored = (int) setImmediateMode.invokeExact(handle, 1);

        int rc = (int) activate.invokeExact(handle);
        if (rc < 0) {
            String message = error(handle);
            if (message.isEmpty()) {
                message = cString((MemorySegment) statusToStr.invokeExact(rc));
            }
            close.invokeExact(handle);
            throw new IllegalStateException("Cannot capture on " + device + ": " + message);
        }

        // Non-blocking, so the loop can check the time and packet limits even when the
        // network is quiet (a blocking read can wait forever on Linux if nothing arrives)
        if ((int) setNonblock.invokeExact(handle, 1, errbuf) != 0) {
            close.invokeExact(handle);
            throw new IllegalStateException(errbuf.getString(0));
        }

        return handle;
    }

    // Reads packets back from a .pcap file instead of a live interface
    public void read(String file, String filter, int count) throws Throwable {

        try (Arena arena = Arena.ofConfined()) {

            MemorySegment errbuf = arena.allocate(ERRBUF_SIZE);
            MemorySegment handle = (MemorySegment) openOffline.invokeExact(
                    arena.allocateFrom(file), errbuf);

            if (handle.equals(MemorySegment.NULL)) {
                throw new IllegalStateException(errbuf.getString(0));
            }

            run(arena, handle, "Reading " + file + "...\n", filter, count, 0, null);
        }
    }

    // `banner` is printed once the filter and output file are set up, so errors come first
    private void run(Arena arena, MemorySegment handle, String banner, String filter,
                     int count, int seconds, String writeTo) throws Throwable {

        MemorySegment dumper = MemorySegment.NULL;
        Thread loopThread = Thread.currentThread();

        // Ctrl+C stops the loop cleanly so the .pcap file is closed and stats are printed
        Thread hook = new Thread(() -> {
            running = false;
            try {
                loopThread.join(2000);
            } catch (InterruptedException ignored) {
            }
        });

        try {
            if (!filter.isEmpty()) {
                applyFilter(arena, handle, filter);
            }

            if (writeTo != null) {
                Path parent = Paths.get(writeTo).toAbsolutePath().getParent();
                if (parent != null) {
                    Files.createDirectories(parent);
                }
                dumper = (MemorySegment) dumpOpen.invokeExact(handle, arena.allocateFrom(writeTo));
                if (dumper.equals(MemorySegment.NULL)) {
                    throw new IllegalStateException(error(handle));
                }
            }

            System.out.println(banner);

            int linkType = (int) datalink.invokeExact(handle);
            MemorySegment headerPtr = arena.allocate(ADDRESS);
            MemorySegment dataPtr = arena.allocate(ADDRESS);

            long deadline = seconds > 0 ? System.currentTimeMillis() + seconds * 1000L : Long.MAX_VALUE;
            int captured = 0;
            stats.clear();
            running = true;
            Runtime.getRuntime().addShutdownHook(hook);

            while (running && (count <= 0 || captured < count) && System.currentTimeMillis() < deadline) {

                int rc = (int) nextEx.invokeExact(handle, headerPtr, dataPtr);

                if (rc == 0) {
                    Thread.sleep(10); // no packet waiting yet
                    continue;
                }
                if (rc == -2) {
                    break; // end of a .pcap file
                }
                if (rc < 0) {
                    throw new IllegalStateException(error(handle));
                }

                MemorySegment header = headerPtr.get(ADDRESS, 0).reinterpret(LEN_OFFSET + 4);
                int caplen = header.get(JAVA_INT, CAPLEN_OFFSET);
                int length = header.get(JAVA_INT, LEN_OFFSET);
                MemorySegment data = dataPtr.get(ADDRESS, 0).reinterpret(caplen);

                if (!dumper.equals(MemorySegment.NULL)) {
                    dump.invokeExact(dumper, header, data);
                }

                captured++;
                System.out.println(time(header) + " " + decode(linkType, data.toArray(JAVA_BYTE), length));
            }

            printStats(captured);
            if (writeTo != null) {
                System.out.println("Saved capture: " + Paths.get(writeTo).toAbsolutePath());
            }

        } finally {
            running = false;
            if (!dumper.equals(MemorySegment.NULL)) {
                dumpClose.invokeExact(dumper);
            }
            close.invokeExact(handle);
            try {
                Runtime.getRuntime().removeShutdownHook(hook);
            } catch (IllegalStateException | IllegalArgumentException ignored) {
                // JVM already shutting down, or the hook was never added
            }
        }
    }

    private void applyFilter(Arena arena, MemorySegment handle, String filter) throws Throwable {

        // struct bpf_program { u_int bf_len; struct bpf_insn *bf_insns; }
        MemorySegment program = arena.allocate(16, 8);

        if ((int) compile.invokeExact(handle, program, arena.allocateFrom(filter), 1, PCAP_NETMASK_UNKNOWN) != 0) {
            throw new IllegalArgumentException("Invalid filter '" + filter + "': " + error(handle));
        }

        int rc = (int) setFilter.invokeExact(handle, program);
        freeCode.invokeExact(program);

        if (rc != 0) {
            throw new IllegalStateException(error(handle));
        }
    }

    private String error(MemorySegment handle) throws Throwable {
        return cString((MemorySegment) getErr.invokeExact(handle));
    }

    private static String time(MemorySegment header) {

        long sec = WINDOWS ? Integer.toUnsignedLong(header.get(JAVA_INT, 0)) : header.get(JAVA_LONG, 0);
        long usec = TV_USEC_SIZE == 4 ? header.get(JAVA_INT, 4) : header.get(JAVA_LONG, 8);

        return LocalTime.ofInstant(Instant.ofEpochSecond(sec, usec * 1000), ZoneId.systemDefault())
                .format(TIME);
    }

    private void printStats(int captured) {

        System.out.println("\n" + captured + " packets captured");
        stats.forEach((protocol, n) -> System.out.printf("  %-6s %d%n", protocol, n));
    }

    private void count(String protocol) {
        stats.merge(protocol, 1, Integer::sum);
    }

    // ---------------------------------------------------------------- decoding

    // Turns one raw frame into a one-line summary like
    // "TCP 172.30.0.5:41000 -> 172.30.0.10:80 [SYN] len=74"
    String decode(int linkType, byte[] p, int length) {

        try {
            String summary = switch (linkType) {
                case DLT_EN10MB -> ethernet(p, 0);
                case DLT_LINUX_SLL -> etherType(p, u16(p, 14), 16);
                case DLT_RAW, DLT_RAW_BSD, LINKTYPE_RAW -> ip(p, 0);
                // the loopback header is the address family in host byte order
                case DLT_NULL -> (p[0] == 2 || p[3] == 2) ? ipv4(p, 4) : ip(p, 4);
                default -> {
                    count("OTHER");
                    yield "link type " + linkType;
                }
            };
            return summary + " len=" + length;

        } catch (ArrayIndexOutOfBoundsException e) {
            count("OTHER");
            return "truncated packet len=" + length;
        }
    }

    private String ethernet(byte[] p, int offset) {

        int type = u16(p, offset + 12);
        int next = offset + 14;

        // 802.1Q VLAN tag: 4 extra bytes before the real EtherType
        if (type == 0x8100) {
            type = u16(p, offset + 16);
            next += 4;
        }

        return etherType(p, type, next);
    }

    private String etherType(byte[] p, int type, int offset) {

        return switch (type) {
            case 0x0800 -> ipv4(p, offset);
            case 0x86DD -> ipv6(p, offset);
            case 0x0806 -> arp(p, offset);
            default -> {
                count("OTHER");
                yield String.format("EtherType 0x%04X", type);
            }
        };
    }

    private String ip(byte[] p, int offset) {
        return (p[offset] >> 4) == 6 ? ipv6(p, offset) : ipv4(p, offset);
    }

    private String arp(byte[] p, int offset) {

        count("ARP");

        int op = u16(p, offset + 6);
        String senderIp = ipv4Address(p, offset + 14);
        String targetIp = ipv4Address(p, offset + 24);

        return op == 1
                ? "ARP who-has " + targetIp + " tell " + senderIp
                : "ARP " + senderIp + " is-at " + mac(p, offset + 8);
    }

    private String ipv4(byte[] p, int offset) {

        int headerLength = (p[offset] & 0x0F) * 4;
        int protocol = p[offset + 9] & 0xFF;
        int ttl = p[offset + 8] & 0xFF;
        String src = ipv4Address(p, offset + 12);
        String dst = ipv4Address(p, offset + 16);

        return transport(p, protocol, offset + headerLength, src, dst, ":") + " ttl=" + ttl;
    }

    private String ipv6(byte[] p, int offset) {

        int nextHeader = p[offset + 6] & 0xFF;
        String src = ipv6Address(p, offset + 8);
        String dst = ipv6Address(p, offset + 24);

        // IPv6 addresses already contain ':', so ports are shown as [addr].port
        return transport(p, nextHeader, offset + 40, "[" + src + "]", "[" + dst + "]", ".");
    }

    private String transport(byte[] p, int protocol, int offset, String src, String dst, String sep) {

        switch (protocol) {

            case 6 -> {
                count("TCP");
                int srcPort = u16(p, offset);
                int dstPort = u16(p, offset + 2);
                int dataOffset = ((p[offset + 12] & 0xFF) >> 4) * 4;
                String payload = httpLine(p, offset + dataOffset);
                return "TCP  " + src + sep + srcPort + " -> " + dst + sep + dstPort
                        + " [" + tcpFlags(p[offset + 13]) + "]"
                        + (payload.isEmpty() ? "" : " \"" + payload + "\"");
            }

            case 17 -> {
                int srcPort = u16(p, offset);
                int dstPort = u16(p, offset + 2);
                boolean dns = srcPort == 53 || dstPort == 53;
                count(dns ? "DNS" : "UDP");
                return (dns ? "DNS  " : "UDP  ") + src + sep + srcPort + " -> " + dst + sep + dstPort;
            }

            case 1 -> {
                count("ICMP");
                return "ICMP " + src + " -> " + dst + " " + icmpType(p[offset] & 0xFF, p[offset + 1] & 0xFF);
            }

            case 58 -> {
                count("ICMPv6");
                return "ICMPv6 " + src + " -> " + dst + " type=" + (p[offset] & 0xFF);
            }

            default -> {
                count("OTHER");
                return "IP proto " + protocol + " " + src + " -> " + dst;
            }
        }
    }

    private static String tcpFlags(byte flags) {

        String[] names = {"FIN", "SYN", "RST", "PSH", "ACK", "URG"};
        StringJoiner joiner = new StringJoiner(",");

        for (int i = 0; i < names.length; i++) {
            if ((flags & (1 << i)) != 0) {
                joiner.add(names[i]);
            }
        }
        return joiner.toString();
    }

    private static String icmpType(int type, int code) {

        return switch (type) {
            case 0 -> "echo reply";
            case 3 -> "destination unreachable (code " + code + ")";
            case 5 -> "redirect";
            case 8 -> "echo request";
            case 11 -> "time exceeded";
            default -> "type " + type;
        };
    }

    // If the TCP payload looks like HTTP, return its first line (request or status line)
    private static String httpLine(byte[] p, int offset) {

        if (offset >= p.length) {
            return "";
        }

        int end = offset;
        while (end < p.length && end - offset < 100 && p[end] != '\r' && p[end] != '\n') {
            end++;
        }

        String line = new String(p, offset, end - offset, java.nio.charset.StandardCharsets.US_ASCII);

        for (String start : List.of("GET ", "POST ", "PUT ", "DELETE ", "HEAD ", "OPTIONS ", "PATCH ", "HTTP/1.")) {
            if (line.startsWith(start)) {
                return line;
            }
        }
        return "";
    }

    private static int u16(byte[] p, int offset) {
        return ((p[offset] & 0xFF) << 8) | (p[offset + 1] & 0xFF);
    }

    private static String ipv4Address(byte[] p, int offset) {
        return (p[offset] & 0xFF) + "." + (p[offset + 1] & 0xFF) + "."
                + (p[offset + 2] & 0xFF) + "." + (p[offset + 3] & 0xFF);
    }

    private static String ipv6Address(byte[] p, int offset) {

        StringJoiner joiner = new StringJoiner(":");
        for (int i = 0; i < 16; i += 2) {
            joiner.add(Integer.toHexString(u16(p, offset + i)));
        }
        return joiner.toString();
    }

    private static String mac(byte[] p, int offset) {
        return String.format("%02x:%02x:%02x:%02x:%02x:%02x",
                p[offset], p[offset + 1], p[offset + 2], p[offset + 3], p[offset + 4], p[offset + 5]);
    }
}
