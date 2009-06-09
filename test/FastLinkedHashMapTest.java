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

  static class HeadEntryTester extends FastLinkedHashMap<String,String> {
    private static final long serialVersionUID = 0L;
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

  static class EldestNullRemovingMap extends FastLinkedHashMap<String,String> {
    private static final long serialVersionUID = 0L;
    protected boolean removeEldestEntry(Map.Entry<String,String> eldest) {
      return eldest.getKey() == null;
    }
  }
  @Test public void test1 () {
    EldestNullRemovingMap map = new EldestNullRemovingMap();
    map.put("1", "x");
    map.put(null, "y");
    map.remove("1");
    map.put("", "z");
    assertEquals(1, map.size());
    assertFalse(map.containsKey(null));
  }

  // jdk tests

  @SuppressWarnings("unchecked")
  private static Map<Integer,Integer> serClone(Map<Integer,Integer> m) {
    Map<Integer,Integer> result = null;
    try {
      // Serialize
      ByteArrayOutputStream bos = new ByteArrayOutputStream();
      ObjectOutputStream out = new ObjectOutputStream(bos);
      out.writeObject(m);
      out.flush();

      // Deserialize
      ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
      out.close();
      ObjectInputStream in = new ObjectInputStream(bis);
      result = (Map<Integer,Integer>)in.readObject();
      in.close();
    } catch(Exception e) {
      e.printStackTrace();
    }
    return result;
  }

  @Test public void testBasic () {
    Random rnd = new Random(666);
    Integer nil = new Integer(0);
    int numItr =  500;
    int mapSize = 500;

    // Linked List test
    for (int i=0; i<numItr; i++) {
      Map<Integer,Integer> m = new FastLinkedHashMap<Integer,Integer>();
      Integer head = nil;

      for (int j=0; j<mapSize; j++) {
        Integer newHead;
        do {
          newHead = new Integer(rnd.nextInt());
        } while (m.containsKey(newHead));
        m.put(newHead, head);
        head = newHead;
      }
      assertEquals(mapSize, m.size());
      assertEquals(new HashMap<Integer,Integer>(m).hashCode(), m.hashCode());

      Map<Integer,Integer> m2 = new FastLinkedHashMap<Integer,Integer>(); m2.putAll(m);
      m2.values().removeAll(m.keySet());
      assertEquals(1, m2.size());
      assertTrue(m2.containsValue(nil));

      int j=0;
      while (head != nil) {
        assertTrue(m.containsKey(head));
        Integer newHead = m.get(head);
        assertFalse(newHead == null);
        m.remove(head);
        head = newHead;
        j++;
      }
      assertTrue(m.isEmpty());
      assertEquals(mapSize, j);
    }

    FastLinkedHashMap<Integer,Integer> m = new FastLinkedHashMap<Integer,Integer>();
    for (int i=0; i<mapSize; i++)
      assertEquals(null, m.put(new Integer(i), new Integer(2*i)));
    for (int i=0; i<2*mapSize; i++)
      assertEquals(i%2==0, m.containsValue(new Integer(i)));
    assertFalse(m.put(nil, nil) == null);
    Map<Integer,Integer> m2 = new FastLinkedHashMap<Integer,Integer>(); m2.putAll(m);
    assertEquals(m, m2);
    assertEquals(m2, m);
    Set<Map.Entry<Integer,Integer>> s = m.entrySet(), s2 = m2.entrySet();
    assertEquals(s, s2);
    assertEquals(s2, s);
    assertTrue(s.containsAll(s2));
    assertTrue(s2.containsAll(s));

    m2 = serClone(m);
    assertEquals(m, m2);
    assertEquals(m2, m);
    s = m.entrySet(); s2 = m2.entrySet();
    assertEquals(s, s2);
    assertEquals(s2, s);
    assertTrue(s.containsAll(s2));
    assertTrue(s2.containsAll(s));

    s2.removeAll(s);
    assertTrue(m2.isEmpty());

    m2.putAll(m);
    m2.clear();
    assertTrue(m2.isEmpty());

    Iterator<Map.Entry<Integer,Integer>> it = m.entrySet().iterator();
    while(it.hasNext()) {
      it.next();
      it.remove();
    }
    assertTrue(m.isEmpty());

    // Test ordering properties with insert order
    m = new FastLinkedHashMap<Integer,Integer>();
    List<Integer> l = new ArrayList<Integer>(mapSize);
    for (int i=0; i<mapSize; i++) {
      Integer x = new Integer(i);
      m.put(x, x);
      l.add(x);
    }
    assertEquals(l, new ArrayList<Integer>(m.keySet()));
    for (int i=mapSize-1; i>=0; i--) {
      Integer x = (Integer) l.get(i);
      assertEquals(x, m.get(x));
    }
    assertEquals(l, new ArrayList<Integer>(m.keySet()));

    for (int i=mapSize-1; i>=0; i--) {
      Integer x = (Integer) l.get(i);
      m.put(x, x);
    }
    assertEquals(l, new ArrayList<Integer>(m.keySet()));

    m2 = m.clone();
    assertEquals(m, m2);
    assertEquals(m2, m);

    List<Integer> l2 = new ArrayList<Integer>(l);
    Collections.shuffle(l2);
    for (int i=0; i<mapSize; i++) {
      Integer x = (Integer) l2.get(i);
      assertEquals(x, m2.get(x));
    }
    assertEquals(l, new ArrayList<Integer>(m2.keySet()));

    // Test ordering properties with access order
    m = new FastLinkedHashMap<Integer,Integer>(1000, .75f, true);
    for (int i=0; i<mapSize; i++) {
      Integer x = new Integer(i);
      m.put(x, x);
    }
    assertEquals(l, new ArrayList<Integer>(m.keySet()));

    for (int i=0; i<mapSize; i++) {
      Integer x = (Integer) l2.get(i);
      assertEquals(x, m.get(x));
    }
    assertEquals(l2, new ArrayList<Integer>(m.keySet()));

    for (int i=0; i<mapSize; i++) {
      Integer x = new Integer(i);
      m.put(x, x);
    }
    assertEquals(l, new ArrayList<Integer>(m.keySet()));

    m2 = m.clone();
    assertEquals(m, m2);
    assertEquals(m2, m);
    for (int i=0; i<mapSize; i++) {
      Integer x = (Integer) l.get(i);
      assertEquals(x, m2.get(x));
    }
    assertEquals(l, new ArrayList<Integer>(m2.keySet()));
  }

  @Test public void testCache () {
    final int MAP_SIZE = 10;
    final int NUM_KEYS = 100;

    Map<Integer,String> m = new FastLinkedHashMap<Integer,String>() {
      private static final long serialVersionUID = 0L;
      protected boolean removeEldestEntry(Map.Entry<Integer,String> eldest) {
        return size() > MAP_SIZE;
      }
    };

    for (int i = 0; i < NUM_KEYS; i++) {
      m.put(new Integer(i), "");
      int eldest = ((Integer) m.keySet().iterator().next()).intValue();
      assertEquals(Math.max(i-9, 0), eldest);
    }
  }

  //

  @Test public void testIterationOrder () {
    Map<String,String> map1 = new FastLinkedHashMap<String,String> ();
    map1.put("1", "a");
    map1.put("2", "b");
    map1.remove("1");
    map1.put("3", "c");
    assertEquals("{2=b, 3=c}", map1.toString());
    Map<String,String> map2 = new FastLinkedHashMap<String,String> ();
    map2.putAll(map1);
    Iterator<String> i = map2.keySet().iterator();
    assertEquals("2", i.next());
    assertEquals("3", i.next());
    assertFalse(i.hasNext());
    assertEquals("{2=b, 3=c}", map2.toString());
  }
}
