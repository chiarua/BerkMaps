public class Node {
    private long id;
    private double lon, lat;
    private String name = "";
    public Node(long ID, double Lon, double Lat){
        id = ID;
        lon = Lon;
        lat = Lat;
    }
    public void addName(String Name){
        name = Name;
    }
    public long Id(){
        return id;
    }
    public String getName(){
        return name;
    }
    public double getLon(){
        return lon;
    }
    public double getLat() {
        return lat;
    }
    @Override
    public String toString(){
        return name;
    }
}
