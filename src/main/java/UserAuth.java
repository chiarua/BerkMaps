import java.util.HashMap;
import java.util.Map;

/**
 * 用户认证服务类，封装用户注册和登录功能
 */
public class UserAuth {
    private GraphDB graphDB;

    public UserAuth(GraphDB graphDB) {
        this.graphDB = graphDB;
    }

    /**
     * 注册新用户
     * @param username 用户名
     * @param password 密码
     * @return 包含注册结果的Map
     */
    public Map<String, Object> registerUser(String username, String password) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 验证输入
            if (username == null || username.trim().isEmpty()) {
                result.put("success", false);
                result.put("message", "用户名不能为空");
                return result;
            }
            
            if (password == null || password.trim().isEmpty()) {
                result.put("success", false);
                result.put("message", "密码不能为空");
                return result;
            }
            
            // 调用GraphDB的createUser方法创建用户
            long userId = graphDB.createUser(username, password);
            
            if (userId > 0) {
                result.put("success", true);
                result.put("message", "注册成功");
                result.put("userId", userId);
            } else {
                result.put("success", false);
                result.put("message", "注册失败");
            }
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "注册失败: " + e.getMessage());
        }
        
        return result;
    }

    /**
     * 用户登录
     * @param username 用户名
     * @param password 密码
     * @return 包含登录结果的Map
     */
    public Map<String, Object> loginUser(String username, String password) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 验证输入
            if (username == null || username.trim().isEmpty()) {
                result.put("success", false);
                result.put("message", "用户名不能为空");
                return result;
            }
            
            if (password == null || password.trim().isEmpty()) {
                result.put("success", false);
                result.put("message", "密码不能为空");
                return result;
            }
            
            // 调用GraphDB的verifyUser方法验证用户
            long userId = graphDB.verifyUser(username, password);
            
            if (userId > 0) {
                result.put("success", true);
                result.put("message", "登录成功");
                result.put("userId", userId);
                result.put("username", username);
            } else {
                result.put("success", false);
                result.put("message", "用户名或密码错误");
            }
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "登录失败: " + e.getMessage());
        }
        
        return result;
    }
}