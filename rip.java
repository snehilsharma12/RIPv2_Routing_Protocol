/* 
rip.java

Implements the RIPv2 routing protocol

@author: Snehil Sharma
*/

package ripv2;
import java.util.Set;
import java.util.Vector;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;



public class rip{

    static InetAddress my_ip;
    static Set<InetAddress> my_neighbors;
    static int my_port;
    static byte[] multicast_byte_array;
    static InetAddress multicast_ip;
    static Byte my_id;
    static InetAddress my_mask;
    static final int rip_port = 520;
    static DatagramSocket my_socket;
    static DatagramSocket multicast_socket;
    static rtable my_table;
    static Boolean trigger_flag = false;

    
    rtable nb_table;

    public rtable update_table(rtable neighbor_table){

        if(neighbor_table.isEmpty()){
            return my_table;
        }

        InetAddress nb_entry_ip;
        InetAddress nb_next_hop;
        int nb_cost = 0;

        InetAddress my_entry_ip;
        InetAddress my_next_hop;
        int my_cost = 0;

        InetAddress neighbor = get_sender(neighbor_table);

        if( !(my_neighbors.contains(neighbor)) ){

            my_neighbors.add(neighbor);

        }

        for(int i=0; i<neighbor_table.size(); i++){

            try {
                
                route nb_route = neighbor_table.get(i);

                nb_entry_ip = nb_route.router;
                nb_cost = nb_route.cost;
                nb_next_hop = nb_route.next_hop;

                //if the entry is our router
                if (nb_entry_ip.equals(my_ip)){

                    continue;
                }

                
                //if entry exist in our table
                if (my_table.has(nb_entry_ip) ){

                    if(nb_cost == 16){

                        int index = my_table.getIndex(nb_route.router);

                        my_table.remove(index);

                        trigger_flag = true;

                        continue;
                    }

                    //if the next hop of that entry is this router
                    if (nb_next_hop.equals(my_ip)){
                        continue;
                    }

                    my_entry_ip = nb_entry_ip;
                    route my_route = my_table.getRoute(my_entry_ip);
                    my_cost = my_route.cost;
                    my_next_hop = my_route.next_hop;


                    //if our next hop is the their router 
                    if(my_next_hop.equals(nb_entry_ip) ){

                        //see if their route has become better
                        if(my_cost > (nb_cost+1)  ){

                            my_cost = nb_cost + 1;

                        }

                        //if it became worse
                        else if (nb_cost > my_cost - 1){
                            my_cost = nb_cost + 1;
                            
                        }

                    }

                    //if my cost is more
                    if(my_cost > (nb_cost+1)  ){
                        
                        my_cost = nb_cost + 1;
                        my_next_hop = nb_entry_ip;

                    }

                    int index = my_table.getIndex(my_entry_ip);
                    my_table.remove(index);
                    InetAddress mask = nb_route.mask;
                    Byte the_id = nb_route.id;

                    my_table.add(the_id, my_entry_ip, mask, my_next_hop, my_cost);
                    

                }

                //if we don't have that entry
                else{

                    if(nb_cost == 16){

                        trigger_flag = true;

                        continue;
                    }

                    my_entry_ip = nb_entry_ip;
                    my_next_hop = nb_entry_ip;
                    my_cost = nb_route.cost + 1;
                    InetAddress mask = nb_route.mask;
                    Byte the_id = nb_route.id;

                    my_table.add(the_id, my_entry_ip, mask, my_next_hop, my_cost);
                    
                }
                


            } catch (Exception e) {
                
                System.out.println(e);
            }

        }

        return my_table;
    }



    byte[] table_to_array(rtable table){

        byte[] arr= new byte[1000];

        int i = 0;
        for(i=0; i<table.size(); i++){

            route r = table.get(i);
            Byte id = r.id;
            InetAddress ip = r.router;
            byte[] ip_ar = ip.getAddress();
            InetAddress mask = r.mask;
            byte[] mask_arr = mask.getAddress();
            InetAddress hop = r.next_hop;
            byte[] hop_arr = hop.getAddress();
            int cost = r.cost;
            ByteBuffer b = ByteBuffer.allocate(4);
            b.putInt(cost);
            byte[] cost_arr = b.array();


            for(int j=0; j<arr.length; j+=20){

                arr[j] = 0;
                arr[j+1] = id;
                arr[j+2] = 0;
                arr[j+3] = 0;

                arr[j+4] = ip_ar[0];
                arr[j+5] = ip_ar[1];
                arr[j+6] = ip_ar[2];
                arr[j+7] = ip_ar[3];

                arr[j+8] = mask_arr[0];
                arr[j+9] = mask_arr[1];
                arr[j+10] = mask_arr[2];
                arr[j+11] = mask_arr[3];

                arr[j+12] = hop_arr[0];
                arr[j+13] = hop_arr[1];
                arr[j+14] = hop_arr[2];
                arr[j+15] = hop_arr[3];

                arr[j+16] = cost_arr[0];
                arr[j+17] = cost_arr[1];
                arr[j+18] = cost_arr[2];
                arr[j+19] = cost_arr[3];

                
            }

        }

        byte[] trimmed = trim_byte_array(arr, (i+1)*20);

        return trimmed;

    }


    void send_request(){

        //set operation as request
        byte[] data = {1, 2, 0, 0, 0,0,0,0};

        DatagramPacket request = new DatagramPacket(data, data.length);

        try {
            multicast_socket.send(request);

        } catch (Exception e) {
            
            System.out.println(e);
        }
    }

    

    void send_response(InetAddress receiver_addr, int receiver_port){

        try {
            
            DatagramSocket receiver_socket = new DatagramSocket(receiver_port, receiver_addr);

            byte[] table_data = table_to_array(my_table);

            //set operation as response
            byte[] info_header = {2, 2, 0, 0};

            ByteArrayOutputStream concatenate = new ByteArrayOutputStream();

            concatenate.write(info_header);
            concatenate.write(table_data);

            byte[] packet_data = concatenate.toByteArray();

            DatagramPacket response = new DatagramPacket(packet_data, packet_data.length);

            receiver_socket.send(response);

            receiver_socket.close();


        } catch (Exception e) {
            
            System.out.println(e);
        }

    }


    rtable get_table(byte[] packet){

        ByteBuffer buf = ByteBuffer.wrap(packet);

        byte[] header = new byte[4];
        byte[] table_array = new byte[packet.length - 4];

        buf.get(header, 0, header.length);
        buf.get(table_array, 0, table_array.length);

        ByteBuffer tab = ByteBuffer.wrap(table_array);

        rtable recieved_table = new rtable();

        byte[] ip_arr = new byte[4];
        byte[] mask_arr = new byte[4];
        byte[] hop_arr = new byte[4];
        byte[] cost_arr = new byte[4];

        for(int i=0; i<table_array.length; i+=20){

            Byte id = table_array[i+1];
            
            tab.get(ip_arr, i+4, ip_arr.length);
            tab.get(mask_arr, i+8, mask_arr.length);
            tab.get(hop_arr, i+12, hop_arr.length);
            tab.get(cost_arr, i+16, cost_arr.length);


            for(int j=0; j<=4; j++){

                try {
                    InetAddress ip = InetAddress.getByAddress(ip_arr);
                    InetAddress mask = InetAddress.getByAddress(mask_arr);
                    InetAddress hop = InetAddress.getByAddress(hop_arr);
                    int cost = Byte.toUnsignedInt(cost_arr[3]);
                    
                    recieved_table.add(id, ip, mask, hop, cost);

                } catch (Exception e) {
                    
                    e.printStackTrace();
                }

            }
        }

        return recieved_table;

    }



    byte[] trim_byte_array(byte[] data, int data_length){

        int  i = data.length-1;

        while(data[i]==0){
            i--;
        }

        byte[] new_array = new byte[i+1];

        for(int j=0; j<new_array.length; j++){

            new_array[j] = data[j];
        }

        return new_array;

    }



    //trims out the header in header+table packet
    byte[] remove_header(byte[] data){

        byte[] new_array = new byte[data.length - 4];

        int j = 0;
        for(int i = 4; j< new_array.length; i++){

            new_array[j] = data[i];

            j++;
        }

        return new_array;
    }



    InetAddress get_sender(rtable table){

        int i=0;

        for(i=0; i<table.size(); i++){

            if(table.get(i).cost == 0){

                break;
            }

            else{

                continue;
            }
        }
        
        return table.get(i).router;
    }



    void run(){

        while(true){

            byte[] buf = new byte[1000];
            DatagramPacket d_packet = new DatagramPacket(buf, buf.length);

            synchronized(my_ip){

                try {

                    if(my_table.isEmpty()){

                        my_table.add_self(my_id, my_ip, my_mask);

                        send_request();
                    }

                    multicast_socket.receive(d_packet);

                } catch (Exception e) {
                    
                    System.out.println(e);
                }

            }

            try {

                byte[] received_data = d_packet.getData();
                
                int actual_length = d_packet.getLength();

                if (actual_length == 0){

                    continue;

                }

                byte[] actual_packet = trim_byte_array(received_data, actual_length);

                byte[] table_array = remove_header(actual_packet);

                nb_table = get_table(table_array);

                synchronized(my_table){

                    //if operation was request
                    if (actual_packet[0] == 1){
                        
                        InetAddress rcvr_adddress = d_packet.getAddress();

                        int rcvr_port = d_packet.getPort();

                        send_response(rcvr_adddress, rcvr_port);
                    }

                    else{

                        update_table(nb_table);

                    }


                }


            } catch (Exception e) {
                System.out.println(e);
            }

            
        }
    }


    public static void main(String[] args) {
        
        try {

            my_socket = new DatagramSocket(my_port, my_ip);

            multicast_ip = InetAddress.getByAddress(multicast_byte_array);

            multicast_socket = new DatagramSocket(rip_port, multicast_ip);

            


        } catch (Exception e) {
            System.out.println(e);
        }
    }
} 
