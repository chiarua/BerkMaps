import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.InputStream;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Graph for storing all of the intersection (vertex) and road (edge) information.
 * Uses your GraphBuildingHandler to convert the XML files into a graph. Your
 * code must include the vertices, adjacent, distance, closest, lat, and lon
 * methods. You'll also need to include instance variables and methods for
 * modifying the graph (e.g. addNode and addEdge).
 *
 * @author Alan Yao, Josh Hug
 */
public class GraphDB {
    /** Your instance variables for storing the graph. You should consider
     * creating helper classes, e.g. Node, Edge, etc. */

    /**
     * Example constructor shows how to create and start an XML parser.
     * You do not need to modify this constructor, but you're welcome to do so.
     * @param dbPath Path to the XML file to be parsed.
     */
    private Map<Long, List<Long>> adjList = new HashMap<>();
    // once get thing in hash, should not change it.
    private Map<Long, Node> nodesInBerkeley = new HashMap<>();
    private Map<Long, Way> waysInBerkeley = new HashMap<>();
    private Connection connection;
    private Boolean InitializeDB = false;
    public GraphDB(String dbPath) {
        initializeDatabase();
        try {
            File inputFile = new File(dbPath);
            FileInputStream inputStream = new FileInputStream(inputFile);
            // GZIPInputStream stream = new GZIPInputStream(inputStream);

            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser saxParser = factory.newSAXParser();
            GraphBuildingHandler gbh = new GraphBuildingHandler(this);
            saxParser.parse(inputStream, gbh);
        } catch (ParserConfigurationException | SAXException | IOException e) {
            e.printStackTrace();
        }
        clean();
    }
    public Map<Long, Node> getNodesInBerkeley(){
        return nodesInBerkeley;
    }
    public void putNodeInNodes(long id, Node nodeNow) {
        nodesInBerkeley.put(id, nodeNow);
        if (InitializeDB) {
            storeNodeInDatabase(nodeNow);
        }
    }
    private void storeNodeInDatabase(Node node) {
        String sql = "INSERT INTO locations (id, name, latitude, longitude) VALUES (?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setLong(1, node.Id());
            pstmt.setString(2, node.getName());
            pstmt.setDouble(3, node.getLat());
            pstmt.setDouble(4, node.getLon());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    public void addNodeToGraph(Node n){
        long id = n.Id();
        adjList.put(id, new ArrayList<>());
    }

    public void addEdgeToGraph(Node x, Node y){
        long idX = x.Id();
        long idY = y.Id();
        if (!adjList.containsKey(idX) || !adjList.containsKey(idY)) {
            throw new IllegalArgumentException("Both nodes must be added to the graph before adding an edge.");
        }
        adjList.get(idX).add(idY);
        adjList.get(idY).add(idX);
    }

    public Node getNodeFromId(long nodeId) {
        return nodesInBerkeley.get(nodeId);
    }
    public void putWayInWays(long wayId, Way wayNow) {
        waysInBerkeley.put(wayId, wayNow);
    }
    /**
     * Helper to process strings into their "cleaned" form, ignoring punctuation and capitalization.
     * @param s Input string.
     * @return Cleaned string.
     */
    static String cleanString(String s) {
        return s.replaceAll("[^a-zA-Z ]", "").toLowerCase();
    }

    /**
     *  Remove nodes with no connections from the graph.
     *  While this does not guarantee that any two nodes in the remaining graph are connected,
     *  we can reasonably assume this since typically roads are connected.
     */
    private void clean() {
        List<Long> toClean = new ArrayList<>();
        for(Map.Entry<Long, List<Long>> entry: adjList.entrySet()){
            long key = entry.getKey();
            List<Long> val = entry.getValue();
//            System.out.println(val);
            if (val.isEmpty()){
                toClean.add(key);
            }
        }
        for(long key: toClean){
            adjList.remove(key);
        }
    }

    /**
     * Returns an iterable of all vertex IDs in the graph.
     * @return An iterable of id's of all vertices in the graph.
     */
    Iterable<Long> vertices() {
        //YOUR CODE HERE, this currently returns only an empty list.
        Set<Long> keySet = adjList.keySet();
        List<Long> keyList = new ArrayList<>(keySet);
        return keyList;
    }

    /**
     * Returns ids of all vertices adjacent to v.
     * @param v The id of the vertex we are looking adjacent to.
     * @return An iterable of the ids of the neighbors of v.
     */
    Iterable<Long> adjacent(long v) {
        return adjList.get(v);
    }

    /**
     * Returns the great-circle distance between vertices v and w in miles.
     * Assumes the lon/lat methods are implemented properly.
     * <a href="https://www.movable-type.co.uk/scripts/latlong.html">Source</a>.
     * @param v The id of the first vertex.
     * @param w The id of the second vertex.
     * @return The great-circle distance between the two locations from the graph.
     */
    double distance(long v, long w) {
        return distance(lon(v), lat(v), lon(w), lat(w));
    }

    static double distance(double lonV, double latV, double lonW, double latW) {
        double phi1 = Math.toRadians(latV);
        double phi2 = Math.toRadians(latW);
        double dphi = Math.toRadians(latW - latV);
        double dlambda = Math.toRadians(lonW - lonV);

        double a = Math.sin(dphi / 2.0) * Math.sin(dphi / 2.0);
        a += Math.cos(phi1) * Math.cos(phi2) * Math.sin(dlambda / 2.0) * Math.sin(dlambda / 2.0);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return 3963 * c;
    }

    /**
     * Returns the initial bearing (angle) between vertices v and w in degrees.
     * The initial bearing is the angle that, if followed in a straight line
     * along a great-circle arc from the starting point, would take you to the
     * end point.
     * Assumes the lon/lat methods are implemented properly.
     * <a href="https://www.movable-type.co.uk/scripts/latlong.html">Source</a>.
     * @param v The id of the first vertex.
     * @param w The id of the second vertex.
     * @return The initial bearing between the vertices.
     */
    double bearing(long v, long w) {
        return bearing(lon(v), lat(v), lon(w), lat(w));
    }

    static double bearing(double lonV, double latV, double lonW, double latW) {
        double phi1 = Math.toRadians(latV);
        double phi2 = Math.toRadians(latW);
        double lambda1 = Math.toRadians(lonV);
        double lambda2 = Math.toRadians(lonW);

        double y = Math.sin(lambda2 - lambda1) * Math.cos(phi2);
        double x = Math.cos(phi1) * Math.sin(phi2);
        x -= Math.sin(phi1) * Math.cos(phi2) * Math.cos(lambda2 - lambda1);
        return Math.toDegrees(Math.atan2(y, x));
    }

    /**
     * Returns the vertex closest to the given longitude and latitude.
     * @param lon The target longitude.
     * @param lat The target latitude.
     * @return The id of the node in the graph closest to the target.
     */
    long closest(double lon, double lat) {
        long res = 0;
        double minDis = Double.MAX_VALUE;
        for(long node: vertices()){
            double dis = distance(lon, lat, lon(node), lat(node));
            if (dis<minDis){
                res = node;
                minDis = dis;
            }
        }
        return res;
    }

    /**
     * Gets the longitude of a vertex.
     * @param v The id of the vertex.
     * @return The longitude of the vertex.
     */
    double lon(long v) {
        Node n = nodesInBerkeley.get(v);
        return n.getLon();
    }

    /**
     * Gets the latitude of a vertex.
     * @param v The id of the vertex.
     * @return The latitude of the vertex.
     */
    double lat(long v) {
        Node n = nodesInBerkeley.get(v);
        return n.getLat();
    }
    private void initializeDatabase() {
        try {
            try (Connection adminConn = DriverManager.getConnection(
                    "jdbc:mysql://localhost:3306/?useSSL=false", "root", "pass")) {

                try (Statement stmt = adminConn.createStatement()) {
                    ResultSet rs = stmt.executeQuery("SHOW DATABASES LIKE 'bearmaps'");
                    if (!rs.next()) {
                        InitializeDB = true;
                        stmt.executeUpdate("CREATE DATABASE bearmaps");
                        System.out.println("数据库'bearmaps'不存在，已创建");

                        stmt.executeUpdate("USE bearmaps");

                        // 创建地点表
                        stmt.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS locations (
                            id BIGINT PRIMARY KEY,
                            name VARCHAR(255),
                            latitude DOUBLE NOT NULL,
                            longitude DOUBLE NOT NULL,
                            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                            updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
                        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                        """);

                        // 创建用户表
                        stmt.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS users (
                            id BIGINT AUTO_INCREMENT PRIMARY KEY,
                            username VARCHAR(50) NOT NULL UNIQUE,
                            password_hash VARCHAR(255) NOT NULL,
                            password_salt VARCHAR(50) NOT NULL,
                            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                            updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
                        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                        """);

                        // 创建评论表
                        stmt.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS comments (
                            id BIGINT AUTO_INCREMENT PRIMARY KEY,
                            location_id BIGINT NOT NULL,
                            user_id BIGINT NOT NULL,
                            content TEXT NOT NULL,
                            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                            updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                            FOREIGN KEY (location_id) REFERENCES locations(id) ON DELETE CASCADE,
                            FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
                            INDEX (location_id),
                            INDEX (user_id)
                        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                        """);

                        System.out.println("所有数据库表已创建");
                    } else {
                        System.out.println("'bearmaps' Database already exists");
                    }
                }

            }

            // 连接到bearmaps数据库
            connection = DriverManager.getConnection(
                    "jdbc:mysql://localhost:3306/bearmaps?useSSL=false", "root", "Chia16441801");

            // 验证表是否创建成功
            try (Statement stmt = connection.createStatement()) {
                ResultSet rs = stmt.executeQuery("SELECT 1 FROM locations LIMIT 1");
                System.out.println("Database valid successfully");
            } catch (SQLException e) {
                throw new RuntimeException("数据库表验证失败", e);
            }
            System.out.println("用户认证服务初始化完成");

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Fail to initialize database", e);
        }
    }

    /**
     * 创建新用户
     * @param username 用户名
     * @param password 密码（明文）
     * @return 新创建用户的ID
     */
    public long createUser(String username, String password) {
        // 生成随机盐值
        String salt = generateSalt();
        // 使用密码和盐值生成哈希
        String passwordHash = hashPassword(password, salt);

        String sql = "INSERT INTO users (username, password_hash, password_salt) VALUES (?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, username);
            pstmt.setString(2, passwordHash);
            pstmt.setString(3, salt);
            pstmt.executeUpdate();

            // 获取自动生成的ID
            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getLong(1);
                } else {
                    throw new SQLException("创建用户失败，无法获取ID");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("创建用户失败", e);
        }
    }

    /**
     * 验证用户登录
     * @param username 用户名
     * @param password 密码（明文）
     * @return 如果验证成功返回用户ID，否则返回-1
     */
    public long verifyUser(String username, String password) {
        String sql = "SELECT id, password_hash, password_salt FROM users WHERE username = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, username);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    long userId = rs.getLong("id");
                    String storedHash = rs.getString("password_hash");
                    String salt = rs.getString("password_salt");

                    // 使用相同的盐值对输入的密码进行哈希
                    String inputHash = hashPassword(password, salt);

                    // 比较哈希值
                    if (storedHash.equals(inputHash)) {
                        return userId;
                    }
                }
                return -1; // 验证失败
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("验证用户失败", e);
        }
    }

    /**
     * 添加评论
     * @param locationId 地点ID
     * @param userId 用户ID
     * @param content 评论内容
     * @return 新创建评论的ID
     */
    /**
     * 检查地点ID是否存在于数据库中，如果节点存在于内存中但不在数据库中，则保存到数据库
     * @param locationId 地点ID
     * @return 如果地点存在或已成功保存返回true，否则返回false
     */
    private boolean ensureLocationExists(long locationId) {
        // 首先检查节点是否存在于内存中
        Node node = getNodeFromId(locationId);
        if (node == null) {
            return false; // 节点在内存中不存在
        }
        
        // 检查节点是否存在于数据库中
        String checkSql = "SELECT 1 FROM locations WHERE id = ?";
        try (PreparedStatement checkStmt = connection.prepareStatement(checkSql)) {
            checkStmt.setLong(1, locationId);
            try (ResultSet rs = checkStmt.executeQuery()) {
                if (rs.next()) {
                    return true; // 节点在数据库中已存在
                }
            }
            
            // 节点在内存中存在但在数据库中不存在，保存到数据库
            storeNodeInDatabase(node);
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    public long addComment(long locationId, long userId, String content) {
        System.out.println(locationId);
        
        // 确保地点存在于数据库中
        if (!ensureLocationExists(locationId)) {
            throw new RuntimeException("添加评论失败：地点ID不存在");
        }
        
        String sql = "INSERT INTO comments (location_id, user_id, content) VALUES (?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setLong(1, locationId);
            pstmt.setLong(2, userId);
            pstmt.setString(3, content);
            pstmt.executeUpdate();

            // 获取自动生成的ID
            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getLong(1);
                } else {
                    throw new SQLException("添加评论失败，无法获取ID");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("添加评论失败", e);
        }
    }

    /**
     * 获取地点的所有评论
     * @param locationId 地点ID
     * @return 评论列表
     */
    public List<Map<String, Object>> getCommentsByLocation(long locationId) {
        List<Map<String, Object>> comments = new ArrayList<>();
        String sql = """
            SELECT c.id, c.content, c.created_at, u.username
            FROM comments c
            JOIN users u ON c.user_id = u.id
            WHERE c.location_id = ?
            ORDER BY c.created_at DESC
        """;

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setLong(1, locationId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> comment = new HashMap<>();
                    comment.put("id", rs.getLong("id"));
                    comment.put("content", rs.getString("content"));
                    comment.put("username", rs.getString("username"));
                    comment.put("created_at", rs.getTimestamp("created_at"));
                    comments.add(comment);
                }
            }
            return comments;
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("获取评论失败", e);
        }
    }

    /**
     * 生成随机盐值
     * @return 随机盐值字符串
     */
    private String generateSalt() {
        // 生成16字节的随机盐值
        byte[] salt = new byte[16];
        try {
            java.security.SecureRandom.getInstanceStrong().nextBytes(salt);
            // 将字节数组转换为十六进制字符串
            StringBuilder sb = new StringBuilder();
            for (byte b : salt) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new RuntimeException("生成盐值失败", e);
        }
    }

    /**
     * 使用密码和盐值生成哈希
     * @param password 明文密码
     * @param salt 盐值
     * @return 哈希后的密码
     */
    private String hashPassword(String password, String salt) {
        try {
            // 使用PBKDF2WithHmacSHA256算法
            javax.crypto.SecretKeyFactory skf = javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            // 将盐值转换为字节数组
            byte[] saltBytes = hexStringToByteArray(salt);
            // 设置参数：密码、盐值、迭代次数、密钥长度
            javax.crypto.spec.PBEKeySpec spec = new javax.crypto.spec.PBEKeySpec(
                    password.toCharArray(), saltBytes, 65536, 128);
            // 生成密钥
            byte[] hash = skf.generateSecret(spec).getEncoded();
            // 将字节数组转换为十六进制字符串
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("密码哈希失败", e);
        }
    }

    /**
     * 将十六进制字符串转换为字节数组
     * @param hexString 十六进制字符串
     * @return 字节数组
     */
    private byte[] hexStringToByteArray(String hexString) {
        int len = hexString.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hexString.charAt(i), 16) << 4)
                    + Character.digit(hexString.charAt(i + 1), 16));
        }
        return data;
    }

    private UserAuth userAuth;

    /**
     * 获取用户认证服务
     * @return UserAuth实例
     */
    public UserAuth getUserAuth() {
        if (userAuth == null) {
            userAuth = new UserAuth(this);
        }
        return userAuth;
    }
}
