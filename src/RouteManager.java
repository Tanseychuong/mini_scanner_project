import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class RouteManager {

    public void addRoute(String destination,
                         String subnetMask,
                         String gateway) throws IOException {

        ProcessBuilder pb = new ProcessBuilder(
                "route",
                "add",
                destination,
                "mask",
                subnetMask,
                gateway);

        Process process = pb.start();

        BufferedReader reader =
                new BufferedReader(new InputStreamReader(process.getInputStream()));

        String line;

        while ((line = reader.readLine()) != null) {
            System.out.println(line);
        }
    }

    public void printRoutingTable() throws IOException {

        ProcessBuilder pb = new ProcessBuilder("route", "print");

        Process process = pb.start();

        BufferedReader reader =
                new BufferedReader(new InputStreamReader(process.getInputStream()));

        String line;

        while ((line = reader.readLine()) != null) {
            System.out.println(line);
        }
    }
}