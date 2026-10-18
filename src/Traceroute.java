
// Importing the java dependencies
import java.io.*;

// class traceroute defintion
public class Traceroute {

    // method for tracing the destination with the OS traceroute tool
    // (tracert on Windows, traceroute on Linux/macOS)
    public void trace(String destination)
            throws IOException, InterruptedException {

        if (CommandRunner.isWindows()) {
            // -d skips reverse DNS lookups, which makes the trace much faster
            CommandRunner.run("tracert", "-d", "-h", "30", destination);
        } else {
            CommandRunner.run("traceroute", "-n", "-m", "30", destination);
        }
    }

}
