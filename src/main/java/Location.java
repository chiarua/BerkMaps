import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

/*
handle search locations.
 */
public class Location {
    private static TrieSet locationName = new TrieSet();
    private static Map<String, List<Map<String, Object>>> locations = new HashMap<>();
    public Location(Map<Long, Node>nodes){
        for(Node node: nodes.values()){
            String name = node.getName();
            double lon = node.getLon();
            double lat = node.getLat();
            long id = node.Id();
            Map<String, Object> loc = new HashMap();
            loc.put("name", name);
            loc.put("lon", lon);
            loc.put("lat", lat);
            loc.put("id", id);
            locationName.put(name);

            if (locations.containsKey(name)) {
                locations.get(name).add(loc);
            } else {
                List<Map<String, Object>> list = new ArrayList<>();
                list.add(loc);
                locations.put(name, list);
            }
        }
    }


    public static char toUpperCase(char c) {
        if (97 <= c && c <= 122) {
            c ^= 32;
        }
        return c;
    }
    public List<String> getLocationsByPrefix(String prefix){
        char[] prefixChar = prefix.toCharArray();
        prefixChar[0] = toUpperCase(prefixChar[0]);
        prefix = String.valueOf(prefixChar);
//        System.out.println(prefix);
        return locationName.findByPrefix(prefix);
    }

    public List<Map<String, Object>> getLocations(String locationName){
        return locations.get(locationName);
    }

}
