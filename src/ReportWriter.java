import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

// Saves host discovery and port scan results as JSON or CSV
public class ReportWriter {

    public static Path saveHosts(List<String> hosts, Subnet subnet, String format) throws IOException {

        StringBuilder sb = new StringBuilder();

        if (format.equals("csv")) {
            sb.append("host\n");
            for (String host : hosts) {
                sb.append(host).append("\n");
            }
        } else {
            sb.append("{\n")
              .append("  \"type\": \"host_discovery\",\n")
              .append("  \"subnet\": ").append(quote(subnet.toString())).append(",\n")
              .append("  \"timestamp\": ").append(quote(LocalDateTime.now().toString())).append(",\n")
              .append("  \"hosts\": [");
            for (int i = 0; i < hosts.size(); i++) {
                sb.append(i == 0 ? "\n" : ",\n").append("    ").append(quote(hosts.get(i)));
            }
            sb.append(hosts.isEmpty() ? "]\n" : "\n  ]\n").append("}\n");
        }

        return write("hosts", format, sb.toString());
    }

    public static Path savePorts(List<PortScanner.PortResult> results, String host, String format)
            throws IOException {

        StringBuilder sb = new StringBuilder();

        if (format.equals("csv")) {
            sb.append("host,port,service,banner\n");
            for (PortScanner.PortResult r : results) {
                sb.append(csv(r.host())).append(",")
                  .append(r.port()).append(",")
                  .append(csv(r.service())).append(",")
                  .append(csv(r.banner())).append("\n");
            }
        } else {
            sb.append("{\n")
              .append("  \"type\": \"port_scan\",\n")
              .append("  \"host\": ").append(quote(host)).append(",\n")
              .append("  \"timestamp\": ").append(quote(LocalDateTime.now().toString())).append(",\n")
              .append("  \"open_ports\": [");
            for (int i = 0; i < results.size(); i++) {
                PortScanner.PortResult r = results.get(i);
                sb.append(i == 0 ? "\n" : ",\n")
                  .append("    {\"port\": ").append(r.port())
                  .append(", \"service\": ").append(quote(r.service()))
                  .append(", \"banner\": ").append(quote(r.banner())).append("}");
            }
            sb.append(results.isEmpty() ? "]\n" : "\n  ]\n").append("}\n");
        }

        return write("ports_" + host.replaceAll("[^A-Za-z0-9.-]", "_"), format, sb.toString());
    }

    private static Path write(String name, String format, String content) throws IOException {

        Path dir = Paths.get("reports");
        Files.createDirectories(dir);

        String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        Path file = dir.resolve(name + "_" + stamp + "." + format);

        Files.writeString(file, content, StandardCharsets.UTF_8);
        return file.toAbsolutePath();
    }

    private static String quote(String s) {

        StringBuilder sb = new StringBuilder("\"");
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        return sb.append("\"").toString();
    }

    private static String csv(String s) {
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }
}
