import java.util.*;
import java.io.*;
import org.junit.*;
import static org.junit.Assert.*;

public class FastHashSetTest {

  @Test public void test () {
    FastHashSet<String> set = new FastHashSet<String> ();

    // Add some values
    assertTrue(set.add("1"));
    assertFalse(set.add("1"));
    assertTrue(set.add(null));
    assertFalse(set.add(null));
    assertTrue(set.add("2"));
    assertTrue(set.add("3"));

    // Check size
    assertEquals(4, set.size());

    // Check elements
    assertTrue(set.contains("1"));
    assertTrue(set.contains("2"));
    assertTrue(set.contains("3"));
    assertTrue(set.contains(null));
    assertFalse(set.contains("null"));

    // Check iterator
    Iterator<String> i = set.iterator();
    assertEquals("1", i.next());
    assertEquals(null, i.next());
    assertEquals("2", i.next());
    assertEquals("3", i.next());
    assertFalse(i.hasNext());
  }

}
