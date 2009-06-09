import org.junit.*;
import static org.junit.Assert.*;

public class HybridHashMapTest {

  @Test public void testNull () {
    HybridHashMap<String,String> map = new HybridHashMap<String,String>();
    assertEquals(0, map.size());
    assertEquals(null, map.put(null, "1"));
    assertEquals(1, map.size());
    assertEquals("1", map.put(null, "2"));
    assertEquals("2", map.get(null));
  }

  @Test public void testA () {
    HybridHashMap<String,String> map = new HybridHashMap<String,String>();
    assertEquals(0, map.size());
    assertEquals(null, map.put("a", "1"));
    assertEquals(1, map.size());
    assertEquals("1", map.put("a", "2"));
    assertEquals("2", map.get("a"));
  }

  @Test public void testABC () {
    HybridHashMap<String,String> map = new HybridHashMap<String,String>();
    assertEquals(null, map.put("a", "1"));
    assertEquals(null, map.put("b", "2"));
    assertEquals(null, map.put("c", "3"));
    assertEquals("1", map.put("a", "x"));
    assertEquals("2", map.put("b", "y"));
    assertEquals("3", map.put("c", "z"));
  }

  static class ZeroHash {
    int n;
    ZeroHash(int n) { this.n = n; }
    public int hashCode() { return 1; }
    public boolean equals(Object o) {
      return o instanceof ZeroHash ? ((ZeroHash)o).n == n : false;
    }
    public String toString() { return "ZeroHash("+n+")"; }
  }
  @Test public void testOneBasket () {
    HybridHashMap<ZeroHash,String> map = new HybridHashMap<ZeroHash,String>();
    assertEquals(null, map.put(new ZeroHash(1), "1"));
    assertEquals(null, map.put(new ZeroHash(2), "2"));
    assertEquals(null, map.put(new ZeroHash(3), "3"));
    assertEquals("3", map.get(new ZeroHash(3)));
    assertEquals(null, map.put(new ZeroHash(4), "4"));
    assertEquals("1", map.get(new ZeroHash(1)));
    assertEquals("4", map.get(new ZeroHash(4)));
    assertEquals(null, map.put(new ZeroHash(5), "5"));
    assertEquals("5", map.get(new ZeroHash(5)));
    assertEquals(null, map.put(new ZeroHash(6), "6"));
    assertEquals("1", map.get(new ZeroHash(1)));
    assertEquals("2", map.get(new ZeroHash(2)));
    assertEquals("3", map.get(new ZeroHash(3)));
    assertEquals("4", map.get(new ZeroHash(4)));
    assertEquals("5", map.get(new ZeroHash(5)));
    assertEquals("6", map.get(new ZeroHash(6)));
    assertEquals("1", map.put(new ZeroHash(1), "x"));
    assertEquals("2", map.put(new ZeroHash(2), "y"));
    assertEquals("3", map.put(new ZeroHash(3), "z"));
    assertEquals("4", map.put(new ZeroHash(4), "a"));
    assertEquals("5", map.put(new ZeroHash(5), "b"));
    assertEquals("x", map.get(new ZeroHash(1)));
    assertEquals("y", map.get(new ZeroHash(2)));
    assertEquals("z", map.get(new ZeroHash(3)));
  }
}
