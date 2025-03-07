import java.util.ArrayList;
import java.util.List;

public class Way {
    private long id;
    private List<Node> nodes;
    private boolean isHigh;
    private String name;
    public Way(long Id){
        id = Id;
        nodes = new ArrayList<>();
        isHigh = false;
    }
    public void High(){
        isHigh = true;
    }
    public boolean ifHigh(){
        return isHigh;
    }
    public void addNode(Node n){
        nodes.add(n);
    }
    public void addName(String s){
        name = s;
    }
    public List<Node> returnNode(){
        return nodes;
    }

    public long Id(){
        return id;
    }

    public String getName(){
        return name;
    }
}
