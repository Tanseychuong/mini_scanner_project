import java.io.*;

public class NetstatViewer {

    public void showConnections() throws IOException, InterruptedException {

        if (CommandRunner.isWindows()) {
            CommandRunner.run("netstat", "-ano");
        } else {
            // -t tcp, -u udp, -n numeric, -a all; ss is the modern replacement on Linux
            if (CommandRunner.run("netstat", "-tuna") != 0) {
                CommandRunner.run("ss", "-tuna");
            }
        }

    }

}
