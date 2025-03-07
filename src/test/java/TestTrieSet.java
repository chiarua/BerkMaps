import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TestTrieSet {

    private TrieSet trieSet;

    @BeforeEach
    void setUp() {
        trieSet = new TrieSet();
        // 添加一些测试数据
        trieSet.put("apple");
        trieSet.put("app");
        trieSet.put("apricot");
        trieSet.put("banana");
        trieSet.put("bat");
        trieSet.put("ball");
    }

    // 测试查找一个空的前缀
    @Test
    void testFindByPrefixEmpty() {
        List<String> result = trieSet.findByPrefix("");
        assertNotNull(result);
        assertTrue(result.isEmpty(), "空前缀应返回一个空的列表");
    }

    // 测试查找一个不存在的前缀
    @Test
    void testFindByPrefixNonExistent() {
        List<String> result = trieSet.findByPrefix("xyz");
        System.out.println("testFindByPrefixNonExistent 返回的内容: " + result);
        assertNotNull(result);
        assertTrue(result.isEmpty(), "不存在的前缀应返回一个空的列表");
    }

    // 测试查找一个存在的前缀
    @Test
    void testFindByPrefixExists() {
        List<String> result = trieSet.findByPrefix("ap");
        assertNotNull(result);
        System.out.println("返回的内容: " + result);
        assertEquals(3, result.size(), "前缀'ap'应该返回3个单词");
        assertTrue(result.contains("apple"));
        assertTrue(result.contains("app"));
        assertTrue(result.contains("apricot"));
    }

    // 测试查找一个部分匹配的前缀
    @Test
    void testFindByPrefixPartial() {
        List<String> result = trieSet.findByPrefix("ba");
        assertNotNull(result);
        System.out.println("返回的内容: " + result);
        assertEquals(3, result.size(), "前缀'ba'应该返回3个单词");
        assertTrue(result.contains("banana"));
        assertTrue(result.contains("bat"));
        assertTrue(result.contains("ball"));
    }

    // 测试前缀后有其他内容时的查找
    @Test
    void testFindByPrefixWithAdditionalContent() {
        List<String> result = trieSet.findByPrefix("bat");
        assertNotNull(result);
        assertEquals(1, result.size(), "前缀'bat'应该返回1个单词");
        assertTrue(result.contains("bat"));
    }

    // 测试查找前缀时没有任何元素的情况
    @Test
    void testFindByPrefixNoWords() {
        TrieSet emptyTrieSet = new TrieSet();
        List<String> result = emptyTrieSet.findByPrefix("a");
        assertNotNull(result);
        assertTrue(result.isEmpty(), "没有元素的前缀应返回一个空的列表");
    }

    // 测试完全匹配前缀与单词
    @Test
    void testFindByPrefixExactMatch() {
        List<String> result = trieSet.findByPrefix("ball");
        assertNotNull(result);
        assertEquals(1, result.size(), "前缀'ball'应该只返回一个单词");
        assertTrue(result.contains("ball"));
    }
}
