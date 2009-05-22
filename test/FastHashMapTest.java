import java.util.*;
import org.junit.*;
import static org.junit.Assert.*;

public class FastHashMapTest {

  @Test public void testNull () {
    FastHashMap<String,String> map = new FastHashMap<String,String> ();
    assertEquals (0, map.size());
    map.clear();
    assertEquals (0, map.size());

    assertEquals (null, map.get(null));
    assertFalse(map.containsValue(null));
    assertFalse(map.containsValue("null"));
    assertEquals (null, map.put(null, "null"));
    assertEquals (1, map.size());
    assertFalse(map.containsValue(null));
    assertTrue(map.containsValue("null"));
    assertFalse(map.containsValue("test"));
    assertEquals ("null", map.get(null));
    assertEquals ("null", map.put(null, "test"));
    assertEquals (1, map.size());
    assertEquals ("test", map.get(null));
    assertFalse(map.containsValue(null));
    assertFalse(map.containsValue("null"));
    assertTrue(map.containsValue("test"));

    assertEquals (null, map.get("null"));
    assertEquals (null, map.put("null", null));
    assertEquals (2, map.size());
    assertTrue(map.containsValue(null));
    assertFalse(map.containsValue("null"));
    assertTrue(map.containsValue("test"));
    assertEquals (null, map.get("null"));
    assertEquals (null, map.put("null", "test"));
    assertEquals (2, map.size());
    assertEquals ("test", map.get("null"));
    assertFalse(map.containsValue(null));
    assertFalse(map.containsValue("null"));
    assertTrue(map.containsValue("test"));
  }

  @Test public void testRemove () {
    FastHashMap<String,String> map = new FastHashMap<String,String> ();
    int c;

    assertEquals(null, map.remove(null));
    assertEquals(null, map.remove("test"));
    assertEquals(null, map.remove(""));
    assertTrue(map.isEmpty());

    for (int i = 0; i < 20; i++) {
      map.put("a"+i, i+"x");
      map.put("b"+i, i+"y");
      map.put("c"+i, i+"z");
    }
    assertEquals(60, map.size());

    for (int i = 0; i < 20; i++) {
      assertEquals(i+"x", map.remove("a"+i));
      assertFalse(map.containsValue(i+"x"));
      assertEquals(59-i, map.size());
      c = 0;
      for (String e : map.keySet()) c++;
      assertEquals (59-i, c);
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

    assertEquals(50,map.size());
    assertEquals(null, map.remove(null));
    assertEquals(null, map.remove("test"));
    assertEquals(null, map.remove(""));
    assertEquals(50,map.size());

    for (int i = 20; i < 50; i++)
      assertEquals("("+i+")", map.get("d"+i));
    for (int i = 0; i < 20; i++)
      assertEquals(i+"z", map.get("c"+i));

    c = 0;
    for (String e : map.keySet()) c++;
    assertEquals (50, c);
  }

  @Test public void testKeySet () {
    FastHashMap<String,String> map = new FastHashMap<String,String> ();
    map.put("a", "1");
    map.put("b", "2");
    map.put("c", "3");
    map.put("d", "4");
    map.put("e", "5");

    Iterator<String> i = map.keySet().iterator();
    assertEquals(5, map.size());
    assertTrue(i.hasNext());
    assertEquals("a",i.next());
    assertTrue(i.hasNext());
    assertEquals("b",i.next());
    i.remove();
    assertTrue(i.hasNext());
    assertEquals("c",i.next());
    assertTrue(i.hasNext());
    assertEquals("d",i.next());
    i.remove();
    assertTrue(i.hasNext());
    assertEquals("e",i.next());
    assertFalse(i.hasNext());

    assertEquals(3, map.size());
    assertEquals("1", map.get("a"));
    assertFalse(map.containsKey("b"));
    assertEquals("3", map.get("c"));
    assertFalse(map.containsKey("d"));
    assertEquals("5", map.get("e"));

    i = map.keySet().iterator();
    assertTrue(i.hasNext());
    assertEquals("a",i.next());
    i.remove();
    assertTrue(i.hasNext());
    assertEquals("c",i.next());
    assertTrue(i.hasNext());
    assertEquals("e",i.next());
    i.remove();
    assertFalse(i.hasNext());

    i = map.keySet().iterator();
    assertTrue(i.hasNext());
    assertEquals("c",i.next());
    assertFalse(i.hasNext());
  }

  @Test public void testValues () {
    FastHashMap<String,String> map = new FastHashMap<String,String> ();
    map.put("a", "1");
    map.put("b", "2");
    map.put("c", "3");
    map.put("d", "4");
    map.put("e", "5");

    Iterator<String> i = map.values().iterator();
    assertEquals(5, map.size());
    assertTrue(i.hasNext());
    assertEquals("1",i.next());
    assertTrue(i.hasNext());
    assertEquals("2",i.next());
    i.remove();
    assertTrue(i.hasNext());
    assertEquals("3",i.next());
    assertTrue(i.hasNext());
    assertEquals("4",i.next());
    i.remove();
    assertTrue(i.hasNext());
    assertEquals("5",i.next());
    assertFalse(i.hasNext());

    assertEquals(3, map.size());
    assertEquals("1", map.get("a"));
    assertFalse(map.containsKey("b"));
    assertEquals("3", map.get("c"));
    assertFalse(map.containsKey("d"));
    assertEquals("5", map.get("e"));
  }

  @Test public void testClone () {
    FastHashMap<String,String> map = new FastHashMap<String,String> ();
    map.put("a","b");
    FastHashMap<String,String> clone = map.clone();
    clone.put("a", "c");
    map.put("b", "d");
    assertEquals("b", map.get("a"));
    assertEquals("c", clone.get("a"));
    assertEquals("d", map.get("b"));
    assertFalse(clone.containsKey("b"));
  }
}
