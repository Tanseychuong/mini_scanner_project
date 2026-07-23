
// Importing the java dependencies
import java.io.*;

// class traceroute defintion
public class Traceroute {

    // method for tracing the destion using the processBuilder in the java .io
    public void trace(String destination)
            throws IOException {

        ProcessBuilder pb = new ProcessBuilder("tracert", destination);

        Process process = pb.start();

        // Buffered reader to reading from the process
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()));

        String line;

        while ((line = reader.readLine()) != null) {

            System.out.println(line);

        }

    }

}