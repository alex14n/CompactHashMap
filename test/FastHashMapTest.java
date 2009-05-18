import org.junit.*;
import static org.junit.Assert.*;

public class FastHashMapTest {

  @Test public void testNull () {
    FastHashMap<String,String> map = new FastHashMap<String,String> ();
    assertEquals (0, map.size());
    map.clear();
    assertEquals (0, map.size());

    assertEquals (null, map.get(null));
    assertEquals (null, map.put(null, "null"));
    assertEquals (1, map.size());
    assertEquals ("null", map.get(null));
    assertEquals ("null", map.put(null, "test"));
    assertEquals (1, map.size());
    assertEquals ("test", map.get(null));

    assertEquals (null, map.get("null"));
    assertEquals (null, map.put("null", null));
    assertEquals (2, map.size());
    assertEquals (null, map.get("null"));
    assertEquals (null, map.put("null", "test"));
    assertEquals (2, map.size());
    assertEquals ("test", map.get("null"));
  }

  @Test public void testRemove () {
    FastHashMap<String,String> map = new FastHashMap<String,String> ();
    for (int i = 0; i < 20; i++) {
      map.put("a"+i, i+"x");
      map.put("b"+i, i+"y");
      map.put("c"+i, i+"z");
    }
    assertEquals(60, map.size());

    for (int i = 0; i < 20; i++) {
      assertEquals(i+"x", map.remove("a"+i));
      assertEquals(59-i, map.size());
    }
    for (int i = 0; i < 20; i++) {
      assertFalse(map.containsKey("a"+i));
      assertEquals(i+"y", map.get("b"+i));
      assertEquals(i+"z", map.get("c"+i));
    }

    for (int i = 0; i < 50; i++)
      map.put("d"+i, "("+i+")");

    for (int i = 0; i < 20; i++) {
      assertEquals(i+"y", map.remove("b"+i));
      assertEquals(89-i, map.size());
    }
    for (int i = 0; i < 20; i++) {
      assertFalse(map.containsKey(i+"x"));
      assertFalse(map.containsKey(i+"y"));
      assertEquals(i+"z", map.get("c"+i));
    }

    for (int i = 0; i < 20; i++)
      assertEquals("("+i+")", map.remove("d"+i));
    for (int i = 20; i < 50; i++)
      assertEquals("("+i+")", map.get("d"+i));
    for (int i = 0; i < 20; i++)
      assertEquals(i+"z", map.get("c"+i));
  }
}
