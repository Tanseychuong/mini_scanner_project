import java.io.*;
import java.util.*;

// Shared helper for the modules that wrap OS commands (tracert, netstat, route, netsh)
public class CommandRunner {

    public static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }

    // Runs a command and prints its output (stdout and stderr merged) line by line
    public static int run(String... command) throws IOException, InterruptedException {

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);

        Process process;
        try {
            process = pb.start();
        } catch (IOException e) {
            System.out.println("Could not run '" + command[0] + "': is it installed and on the PATH?");
            return -1;
        }

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {

            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
        }

        return process.waitFor();
    }

    public static int run(List<String> command) throws IOException, InterruptedException {
        return run(command.toArray(new String[0]));
    }
}
