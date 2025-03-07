import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Graph {
    private Map<Long, List<Long>> adjList = new HashMap<>();

    public Graph(){
    }
    public void addNode(Node n){
        long id = n.Id();
        adjList.put(id, new ArrayList<>());
    }
    public void addEdge(Node x, Node y){
        long idX = x.Id();
        long idY = y.Id();
        if (!adjList.containsKey(idX) || !adjList.containsKey(idY)) {
            throw new IllegalArgumentException("Both nodes must be added to the graph before adding an edge.");
        }
        adjList.get(idX).add(idY);
        adjList.get(idY).add(idX);
    }
    public void clean(){
        for(Map.Entry<Long, List<Long>> entry: adjList.entrySet()){
            long key = entry.getKey();
            List<Long> val = entry.getValue();
            if (val.isEmpty()){
               adjList.remove(key);
            }
        }
    }
}
