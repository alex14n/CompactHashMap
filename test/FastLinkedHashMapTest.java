import java.util.*;
import java.io.*;
import org.junit.*;
import static org.junit.Assert.*;

public class FastLinkedHashMapTest {
  @Test public void test () {
    FastLinkedHashMap<String,String> map = new FastLinkedHashMap<String,String> (16, .75f, true);

    // Insert some elements
    map.put("1", "a");
    map.put("2", "b");
    map.put("3", "c");
    map.put("4", "d");
    map.put("5", "e");

    // Check insertion order
    Iterator<String> i = map.keySet().iterator();
    assertEquals("1", i.next());
    assertEquals("2", i.next());
    assertEquals("3", i.next());
    assertEquals("4", i.next());
    assertEquals("5", i.next());
    assertFalse(i.hasNext());

    // Reorder...
    assertEquals("b", map.get("2"));
    assertEquals("d", map.get("4"));

    // Check new order
    i = map.keySet().iterator();
    assertEquals("1", i.next());
    assertEquals("3", i.next());
    assertEquals("5", i.next());
    assertEquals("2", i.next());
    assertEquals("4", i.next());
    assertFalse(i.hasNext());

    // Remove some elements
    assertEquals("a", map.remove("1"));
    assertEquals("e", map.remove("5"));

    // Check new order
    i = map.keySet().iterator();
    assertEquals("3", i.next());
    assertEquals("2", i.next());
    assertEquals("4", i.next());
    assertFalse(i.hasNext());
  }
}
