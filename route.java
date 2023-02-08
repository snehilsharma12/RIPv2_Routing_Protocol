package ripv2;
import java.net.InetAddress;

public class route {

        InetAddress router;
        InetAddress next_hop;
        int cost;
        Byte id;
        InetAddress mask;

    
        route(Byte id, InetAddress router, InetAddress mask, InetAddress next_hop, int cost){
            
            this.id = id;
            this.router = router;
            this.next_hop = next_hop;
            this.cost = cost;
            this.mask = mask;

        }
    
}
