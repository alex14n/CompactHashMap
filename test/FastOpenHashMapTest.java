import java.util.*;
import java.io.*;
import org.junit.*;
import static org.junit.Assert.*;

public class FastOpenHashMapTest {

  @Test public void testNull () {
    FastOpenHashMap<String,String> map = new FastOpenHashMap<String,String> ();
    assertEquals(0, map.size());
    assertEquals(null, map.put(null, "a"));
    assertEquals("a", map.get(null));
    assertEquals(1, map.size());
    assertEquals("a", map.put(null, "b"));
    assertEquals("b", map.get(null));
    assertEquals(1, map.size());
  }

  @Test public void test1 () {
    FastOpenHashMap<String,String> map = new FastOpenHashMap<String,String> ();
    assertEquals(null, map.put("1", "a"));
    assertEquals("a", map.get("1"));
    assertEquals(1, map.size());
    assertEquals("a", map.put("1", "b"));
    assertEquals("b", map.get("1"));
    assertEquals(1, map.size());
  }

  @Test public void test3 () {
    FastOpenHashMap<String,String> map = new FastOpenHashMap<String,String> ();
    assertEquals(null, map.put("1", "a"));
    assertEquals(null, map.put("2", "b"));
    assertEquals(null, map.put("3", "c"));
    assertEquals("a", map.get("1"));
    assertEquals("b", map.get("2"));
    assertEquals("c", map.get("3"));
    assertEquals(3, map.size());
  }
}
