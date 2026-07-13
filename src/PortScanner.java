import java.net.*;

public class PortScanner {


    public static void scan(String host)
            throws Exception {


        int[] ports = {
                21,   // FTP
                22,   // SSH
                80,   // HTTP
                443,  // HTTPS
                3306, // MySQL/MariaDB
                8080
        };


        for(int port : ports){

            try(Socket socket = new Socket()){

                //Using the socket to check the open ports
                socket.connect(new InetSocketAddress(host, port), 300);

                // Printing the open ports
                System.out.println("OPEN: " + port);


            }catch(Exception e){

                System.out.println("CLOSED: " + port);
            }
        }
    }
}