/*import org.pcap4j.core.*;

public class PacketSniffer {

    public void sniff() throws Exception {

        PcapNetworkInterface nif = Pcaps.findAllDevs().get(0);
        PcapHandle handle = nif.openLive(65536, PcapNetworkInterface.PromiscuousMode.PROMISCUOUS, 10);

        PacketListener listener = packet -> { System.out.println(packet);
        };

        handle.loop(-1, listener);

    }

}*/

/*
* This class is intentional commented out due to some unfinished dependencies that have not yet been analyzed adn imported properly.
* */