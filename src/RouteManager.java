import java.io.IOException;

public class RouteManager {

    // Adding a route needs administrator/root rights
    public void addRoute(String destination,
                         String subnetMask,
                         String gateway) throws IOException, InterruptedException {

        if (CommandRunner.isWindows()) {
            CommandRunner.run("route", "add", destination, "mask", subnetMask, gateway);
        } else {
            int prefix = Integer.bitCount(Subnet.toInt(subnetMask));
            CommandRunner.run("ip", "route", "add", destination + "/" + prefix, "via", gateway);
        }
    }

    public void printRoutingTable() throws IOException, InterruptedException {

        if (CommandRunner.isWindows()) {
            CommandRunner.run("route", "print");
        } else {
            CommandRunner.run("ip", "route", "show");
        }
    }
}
