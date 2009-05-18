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

}
