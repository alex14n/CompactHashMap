import java.util.*;
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
    assertEquals(null, i.next());
    assertEquals("1", i.next());
    assertEquals("2", i.next());
    assertEquals("3", i.next());
    assertFalse(i.hasNext());

    // Check removal
    assertTrue(set.remove("1"));
    assertFalse(set.remove("1"));
    assertFalse(set.contains("1"));
  }

  @Test(expected=ConcurrentModificationException.class)
  public void testIterator () {
    FastHashSet<String> set = new FastHashSet<String> ();
    set.add("1");
    set.add("2");
    set.add("3");
    set.add("4");
    set.add("5");
    Iterator<String> i = set.iterator();
    assertEquals("1", i.next());
    assertEquals("2", i.next());
    set.remove("2");
    set.remove("4");
    assertEquals("3", i.next());
    set.add("2");
    set.add("4");
    assertEquals("2", i.next()); // This is bad: "2" iterated twice
    assertEquals("5", i.next());
    assertFalse(i.hasNext());
  }
}
