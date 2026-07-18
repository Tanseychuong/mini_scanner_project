import java.io.*;

public class Traceroute {

    public void trace(String destination)
            throws IOException {

        ProcessBuilder pb =
                new ProcessBuilder("tracert", destination);

        Process process = pb.start();

        BufferedReader reader =
                new BufferedReader(
                        new InputStreamReader(process.getInputStream()));

        String line;

        while((line = reader.readLine()) != null){

            System.out.println(line);

        }

    }

}