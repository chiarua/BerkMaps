import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.BasicStroke;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.IOException;

public class TrieSet {
    private static final int R = 26;
    private class Node{
        /*Up to R links */
        boolean exists;
        Map<Character, Node> links;
        public Node(){
            links = new TreeMap<>();
            exists = false;
        }
    }
    private Node root = new Node();

    public void put(String key){
        put(root, key, 0);
    }
    private Node put(Node x, String key, int d) {
        if (x == null) {
            x = new Node();
        }
        // 当处理完所有字符后，标记该节点为单词结束
        if (d == key.length()) {
            x.exists = true;
            return x;
        }
        // 获取当前字符
        char c = key.charAt(d);
        // 根据当前字符获取子节点，如果不存在则为 null
        Node child = x.links.get(c);
        // 递归插入后续字符，并将返回的子节点放回 Map 中
        child = put(child, key, d + 1);
        x.links.put(c, child);
        return x;
    }
    private String thisChar(int n){
        char c = (char) (n + 'a');
        return String.valueOf(c);
    }
    private List<String> findAll(Node n, String s) {
        List<String> res = new ArrayList<>();
        // 遍历当前节点所有子节点
        for (Map.Entry<Character, Node> entry : n.links.entrySet()) {
            char c = entry.getKey();
            Node child = entry.getValue();
            // 如果子节点标记为存在一个单词，则将 s+c 加入结果
            if (child.exists) {
                res.add(s + c);
            }
            // 递归查找子节点下的所有单词，并将结果加入列表
            res.addAll(findAll(child, s + c));
        }
        return res;
    }
    public List<String> findByPrefix(String s){
        List<String> res = new ArrayList<>();
        Node now = root;
        int len = s.length();
        for(int i=0;i<len&&now!=null;i++) {
            char c = s.charAt(i);
            now = now.links.get(c);
        }
        if(s==""||now==null){
            return res;
        }
        if(now.exists){
            res.add(s);
            return res;
        }

        return findAll(now, s);
    }
}
