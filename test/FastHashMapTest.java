import java.util.*;
import java.io.*;
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
    Iterator<String> it;

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
      for(it = map.keySet().iterator(); it.hasNext(); it.next()) c++;
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
    for(it = map.keySet().iterator(); it.hasNext(); it.next()) c++;
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
    Set<String> mapKeys = map.keySet();
    FastHashMap<String,String> clone = map.clone();
    clone.put("a", "c");
    map.put("b", "d");
    assertEquals("b", map.get("a"));
    assertEquals("c", clone.get("a"));
    assertEquals("d", map.get("b"));
    assertFalse(clone.containsKey("b"));

    Set<String> cloneKeys = clone.keySet();
    assertTrue(mapKeys.contains("b"));
    assertFalse(cloneKeys.contains("b"));
    assertTrue(mapKeys != cloneKeys);
  }

  @Test public void testReadWrite () throws IOException, ClassNotFoundException {
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    ObjectOutputStream oos = new ObjectOutputStream(bos);

    // Fill the map
    FastHashMap<String,String> map = new FastHashMap<String,String> ();
    map.put("a","1");
    map.put("b","2");
    map.put("c","3");
    map.put("d","4");
    map.remove("a");
    map.remove("d");

    // Write it
    oos.writeObject(map);
    oos.close();
    bos.close();

    // Read it
    ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
    ObjectInputStream ois = new ObjectInputStream(bis);
    @SuppressWarnings("unchecked")
    FastHashMap<String,String> read = (FastHashMap<String,String>)ois.readObject();
    assertEquals(0, ois.available());
    ois.close();
    assertEquals(0, bis.available());
    bis.close();

    // Check it
    assertFalse(map == read);
    assertEquals(map.size(), read.size());
    assertFalse(read.containsKey("a"));
    assertEquals("2", read.get("b"));
    assertEquals("3", read.get("c"));
    assertFalse(read.containsKey("d"));
    assertEquals(map, read);
  }

  @Test public void testEntrySetValue () {
    FastHashMap<String,String> map = new FastHashMap<String,String> ();
    map.put("a","b");
    Map.Entry<String,String> entry = map.entrySet().iterator().next();
    assertEquals("b", entry.setValue("x"));
    assertEquals("x", map.get("a"));
  }

  // jdk tests for HashMap

  @Test public void testKeySetRemove () {
    FastHashMap<String,String> map = new FastHashMap<String,String> ();
    map.put("bananas", null);
    assertTrue(map.keySet().remove("bananas"));
  }

  @Test public void testSetValue () {
    String key      = "key";
    String oldValue = "old";
    String newValue = "new";
    FastHashMap<String,String> m = new FastHashMap<String,String> ();
    m.put(key, oldValue);
    Map.Entry<String,String> e = (Map.Entry<String,String>) m.entrySet().iterator().next();
    Object returnVal = e.setValue(newValue);
    assertEquals(oldValue, returnVal);
  }

  @Test public void testToString () {
    FastHashMap<String,String> m = new FastHashMap<String,String> ();
    m.put(null, null);
    m.entrySet().iterator().next().toString();
  }

  // jdk Hashtable tests

  @Test public void testHashCode () {
    FastHashMap<String,String> m = new FastHashMap<String,String> ();
    assertEquals(0, m.hashCode());
    m.put("Joe", "Blow");
    assertEquals("Joe".hashCode() ^ "Blow".hashCode(), m.hashCode());
  }

  @Test(expected=IllegalArgumentException.class)
  public void testIllegalLoadFactor1 () {
    new FastHashMap<String,String> (100, -3);
  }

  @Test(expected=IllegalArgumentException.class)
  public void testIllegalLoadFactor2 () {
    new FastHashMap<String,String> (100, Float.NaN);
  }

  @Test public void testIllegalLoadFactor3 () {
    new FastHashMap<String,String> (100, .69f);
  }

  static class ReadObject extends FastHashMap<Object,Object> {
    private static final long serialVersionUID = 0L;
    class ValueWrapper implements Serializable {
      private static final long serialVersionUID = 0L;
      private Object mValue;
      ValueWrapper(Object value) {
        mValue = value;
      }
      Object getValue() {
        return mValue;
      }
    };

    public Object get(Object key) {
      ValueWrapper valueWrapper = (ValueWrapper)super.get(key);
      Object value = valueWrapper.getValue();
      if(value instanceof ValueWrapper)
        throw new RuntimeException("Hashtable.get bug");
      return value;
    }

    public Object put(Object key, Object value) {
      if(value instanceof ValueWrapper)
        throw new RuntimeException(
            "Hashtable.put bug: value is already wrapped");
      ValueWrapper valueWrapper = new ValueWrapper(value);
      super.put(key, valueWrapper);
      return value;
    }
  }

  private static Object copyObject(Object oldObj) {
    Object newObj = null;
    try {
      //Create a stream in which to serialize the object.
      ByteArrayOutputStream ostream = new ByteArrayOutputStream();
      ObjectOutputStream p = new ObjectOutputStream(ostream);
      //Serialize the object into the stream
      p.writeObject(oldObj);

      //Create an input stream from which to deserialize the object
      byte[] byteArray = ostream.toByteArray();
      ByteArrayInputStream istream = new ByteArrayInputStream(byteArray);
      ObjectInputStream q = new ObjectInputStream(istream);
      //Deserialize the object
      newObj = q.readObject();
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    return newObj;
  }

  @Test public void testReadObject () {
    ReadObject myHashtable = new ReadObject();
    myHashtable.put("key", "value");
    ReadObject myHashtableCopy = (ReadObject)copyObject(myHashtable);
    myHashtableCopy.get("key");
  }

  private static void testMap(Map<Object,Object> m) {
    assertEquals("{}", m.toString());
    m.put("Harvey", m);
    assertEquals("{Harvey=(this Map)}", m.toString());
    m.clear();
    m.put(m, "Harvey");
    assertEquals("{(this Map)=Harvey}", m.toString());
    m.clear();
    m.hashCode();
  }

  @Test public void testSelfRef () {
    testMap(new FastHashMap<Object,Object>());
    testMap(new FastLinkedHashMap<Object,Object>());
  }

  // put(..., false) test

  @Test public void testCopyConstructor () {
    FastHashMap<String,String> map = new FastHashMap<String,String> ();
    map.put("1", "a");
    map.put("", "c");
    map.put("2", "b");
    map.put(null, "d");
    map.put("3", "e");
    FastHashMap<String,String> copy = new FastHashMap<String,String> (map);
    assertEquals(map.size(), copy.size());
    assertEquals(map, copy);
  }

  // jdk test

  @Test(expected=ConcurrentModificationException.class)
  public void testEmptyMapIterator () {
    Map<String,String> map = new FastHashMap<String,String>();
    Iterator<Map.Entry<String,String>> iter = map.entrySet().iterator();
    map.put("key", "value");
    iter.next();
  }

  // jdk IdentityHashMap tests

  @Test public void testToArray1 () {
    FastHashMap<String,String> mm = new FastHashMap<String,String>();
    mm.put("foo", "bar");
    mm.put("baz", "quux");
    List<Map.Entry<String,String>> lm
        = new ArrayList<Map.Entry<String,String>>(mm.entrySet());
    String s = lm.toString();
    assertEquals("[foo=bar, baz=quux]", s);
  }

  @Test public void testToArray2 () {
    Map<String,String> m = new FastHashMap<String,String>();
    m.put("french", "connection");
    m.put("polish", "sausage");
    Object[] mArray = m.entrySet().toArray();
    assertFalse(mArray[0] == mArray[1]);
    assertEquals("french=connection", mArray[0].toString());
    assertEquals("polish=sausage", mArray[1].toString());
  }

  @Test public void testToArray3 () {
    FastHashMap<Integer,Integer> map = new FastHashMap<Integer,Integer>();
    Set<Map.Entry<Integer,Integer>> es = map.entrySet();
    assertEquals(0, es.toArray().length);
    assertEquals(null, es.toArray(new Object[]{Boolean.TRUE})[0]);
    map.put(7,49);
    assertEquals(1, es.toArray().length);
    Object[] x = es.toArray(new Object[]{Boolean.TRUE, Boolean.TRUE});
    assertEquals(null, x[1]);
    @SuppressWarnings("unchecked")
    Map.Entry<Integer,Integer> e = (Map.Entry<Integer,Integer>) x[0];
    assertEquals(new Integer(7), e.getKey());
    assertEquals(new Integer(49), e.getValue());
  }
}
