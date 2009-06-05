import java.util.*;
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

  class HeadEntryTester extends FastLinkedHashMap<String,String> {
    String lastValue = null;
    protected boolean removeEldestEntry(Map.Entry<String,String> eldest) {
      lastValue = eldest.getValue();
      return false;
    }
    public HeadEntryTester clone() {
      HeadEntryTester that = (HeadEntryTester)super.clone();
      that.lastValue = null;
      return that;
  }
  }

  @Test public void testHeadEntry () {
    HeadEntryTester map1 = new HeadEntryTester();
    assertEquals(null, map1.lastValue);
    map1.put("a", "1");
    map1.put("b", "x");
    assertEquals("1", map1.lastValue);
    HeadEntryTester map2 = map1.clone();
    map1.put("a", "2");
    map1.put("c", "+");
    assertEquals("2", map1.lastValue);
    map2.put("c", "-");
    assertEquals("1", map2.lastValue);
  }

}
