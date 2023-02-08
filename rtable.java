package ripv2;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.InetAddress;
import java.util.Vector;

public class rtable implements Serializable{

    void writeObject(ObjectOutputStream out) throws IOException{

        out.defaultWriteObject();  
    }

    void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException{

        in.defaultReadObject();  
    }



    Vector<route> table;

    rtable(){

        table = new Vector<route>();
    }
    

    void add(Byte id, InetAddress router,InetAddress mask, InetAddress next_hop, int cost){

        table.add(new route(id, router,  mask, next_hop, cost));
    }

    route get(int i){
        return table.get(i);
    }

    Vector<route> get_vector(){
        return this.table;

    }

    void remove(int i){
        table.remove(i);
    }

    boolean isEmpty(){

        return table.isEmpty();
    }

    int size(){
        return table.size();
    }

    boolean has(InetAddress router){
        for(int i=0; i<table.size(); i++){

            if (router.equals( (table.get(i)).router )){

                return true;
            }

            else{
                continue;
            }
        }

        return false;
    }

    route getRoute(InetAddress router){

        if (this.has(router)){

            for(int i=0; i<table.size(); i++){

                if (router.equals( (table.get(i)).router )){
    
                    return table.get(i);
                }
    
                else{
                    continue;
                }
            }
        }   

        return null;
    
    }

    int getIndex(InetAddress router){

        if (this.has(router)){

            for(int i=0; i<table.size(); i++){

                if (router.equals( (table.get(i)).router )){
    
                    return i;
                }
    
                else{
                    continue;
                }
            }
        }   

        return -1;
    }

    void add_self(Byte my_id, InetAddress my_ip, InetAddress my_mask){
        
        this.add(my_id, my_ip,my_mask, my_ip, 0);    
    
    }

}
