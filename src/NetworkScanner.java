import java.net.*;
import java.util.*;

public class NetworkScanner {


    public static void scan() throws Exception {

        String localIP = InetAddress.getLocalHost().getHostAddress();


        String subnet = localIP.substring(0, localIP.lastIndexOf(".") + 1);


        ArrayList<String> hosts =
                new ArrayList<>();


        for(int i = 1; i <= 254; i++){

            String host = subnet + i;

            InetAddress address = InetAddress.getByName(host);


            if(address.isReachable(300)){
                hosts.add(host);

                System.out.println(host + " reachable");
            }
        }


        System.out.println("Hosts found: " + hosts.size());
    }
}