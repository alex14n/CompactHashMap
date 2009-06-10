import org.junit.*;
import static org.junit.Assert.*;

public class OpenHashMapTest {

  @Test public void testNull () {
    OpenHashMap<String,String> map = new OpenHashMap<String,String>();
    assertEquals(0, map.size());
    assertEquals(null, map.put(null, "1"));
    assertEquals(1, map.size());
    assertEquals("1", map.put(null, "2"));
    assertEquals("2", map.get(null));
  }

  @Test public void testA () {
    OpenHashMap<String,String> map = new OpenHashMap<String,String>();
    assertEquals(0, map.size());
    assertEquals(null, map.put("a", "1"));
    assertEquals(1, map.size());
    assertEquals("1", map.put("a", "2"));
    assertEquals("2", map.get("a"));
  }

  @Test public void testABC () {
    OpenHashMap<String,String> map = new OpenHashMap<String,String>();
    assertEquals(null, map.put("a", "1"));
    assertEquals(null, map.put("b", "2"));
    assertEquals(null, map.put("c", "3"));
    assertEquals("1", map.put("a", "x"));
    assertEquals("2", map.put("b", "y"));
    assertEquals("3", map.put("c", "z"));
  }

  static class Hash1 {
    int n;
    Hash1(int n) { this.n = n; }
    public int hashCode() { return 1; }
    public boolean equals(Object o) {
      return o instanceof Hash1 ? ((Hash1)o).n == n : false;
    }
    public String toString() { return "Hash1("+n+")"; }
  }
  @Test public void testOneBasket () {
    OpenHashMap<Hash1,String> map = new OpenHashMap<Hash1,String>();
    assertEquals(null, map.put(new Hash1(1), "1"));
    assertEquals(1, map.size());
    assertEquals(null, map.put(new Hash1(2), "2"));
    assertEquals(2, map.size());
    assertEquals(null, map.put(new Hash1(3), "3"));
    assertEquals(3, map.size());
    assertEquals("3", map.get(new Hash1(3)));
    assertEquals(null, map.put(new Hash1(4), "4"));
    assertEquals("1", map.get(new Hash1(1)));
    assertEquals("4", map.get(new Hash1(4)));
    assertEquals(null, map.put(new Hash1(5), "5"));
    assertEquals("5", map.get(new Hash1(5)));
    assertEquals(null, map.put(new Hash1(6), "6"));
    assertEquals("1", map.get(new Hash1(1)));
    assertEquals("2", map.get(new Hash1(2)));
    assertEquals("3", map.get(new Hash1(3)));
    assertEquals("4", map.get(new Hash1(4)));
    assertEquals("5", map.get(new Hash1(5)));
    assertEquals("6", map.get(new Hash1(6)));
    assertEquals("1", map.put(new Hash1(1), "x"));
    assertEquals("2", map.put(new Hash1(2), "y"));
    assertEquals("3", map.put(new Hash1(3), "z"));
    assertEquals("4", map.put(new Hash1(4), "a"));
    assertEquals("5", map.put(new Hash1(5), "b"));
    assertEquals("x", map.get(new Hash1(1)));
    assertEquals("y", map.get(new Hash1(2)));
    assertEquals("z", map.get(new Hash1(3)));
  }
}
