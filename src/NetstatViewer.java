import java.io.*;

public class NetstatViewer {

    public void showConnections() throws IOException {

        ProcessBuilder pb =
                new ProcessBuilder("netstat", "-ano");

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