import java.io.*;
import java.util.*;
import org.junit.*;
import static org.junit.Assert.*;

public class FastHashMap2Test {

    @Test
    public void nextPrevTest() {
	int maxL = 1 << 15;
	for (int bits = 0; bits < 32; bits++) {
	    int bits5 = bits * 5;
	    int l = 1 << bits;
	    int l1 = l - 1;
	    int initial = l1 / 5;
	    int current = initial;
	    int ll = l > maxL ? maxL : l;
	    for (int i = 0; i < ll; i++) {
		int next = (current * 5 + 1) & (l - 1);
		long nextL = (current * 5L + 1) % l;
		assertEquals(nextL, next);
		int n1 = (next - 1) & (l - 1);
		int prev = n1 / 5 + lookup5[n1 % 5 + bits5];
		assertEquals(current, prev);
		current = next;
		assertTrue("0 again", i == l1 ? current == initial
			: current != initial);
	    }
	}
    }

    static int[] lookup5;
    static {
	lookup5 = new int[160];
	for (int bits = 0; bits < 32; bits++)
	    for (int i = 1; i < 5; i++) {
		long x = (long) i << bits;
		int mod = 5 - (int) (x % 5);
		lookup5[bits * 5 + mod] = 1 + (int) (x / 5);
	    }
    }

    @Test
    public void test0() {
	FastHashMap2<Integer, String> m = new FastHashMap2<Integer, String>();
	assertEquals(0, m.size());

	for (int i = 0, j = 0; i < 100; i++, j += 2) {
	    Integer jw = j;
	    assertNull(m.get(jw));
	    assertNull(m.put(jw, "" + j));
	    assertEquals(i + 1, m.size());
	    // m.validate("" + j);
	}

	for (int j = 0; j < 200; j += 2) {
	    Integer j0 = j;
	    Integer j1 = j + 1;
	    assertEquals("" + j, m.get(j0));
	    assertNull(m.get(j1));
	    assertEquals("" + j, m.put(j0, "!" + j));
	    assertNull(m.put(j1, "!" + (j + 1)));
	}

	assertEquals(200, m.size());
	for (int j = 0; j < 200; j++) {
	    assertEquals("!" + j, m.get(j));
	}

	Set<Integer> mapKeys = m.keySet();
	FastHashMap2<Integer, String> m2 = m.clone();
	assertEquals(m, m2);
	assertEquals(m2, m);

	m2.clear();
	assertTrue(m2.isEmpty());
	m2.putAll(m);
	assertEquals(m, m2);
	assertEquals(m2, m);

	Set<Integer> cloneKeys = m2.keySet();
	assertTrue(mapKeys != cloneKeys);
    }

    static class HC3 {
	private int value;

	public String toString() {
	    return "" + value;
	}

	public HC3(int value) {
	    this.value = value;
	}

	public int hashCode() {
	    return value & 3;
	}

	public boolean equals(Object o) {
	    if (o instanceof HC3)
		return ((HC3) o).value == value;
	    else
		return false;
	}
    }

    @Test
    public void test1() {
	FastHashMap2<HC3, String> m = new FastHashMap2<HC3, String>();
	for (int i = 0; i < 300; i++) {
	    assertNull(m.put(new HC3(i), "" + i));
	}

	assertNull(m.put(null, "test"));
	assertTrue(m.containsValue("test"));
	assertEquals("test", m.put(null, "null"));
	assertFalse(m.containsValue("test"));
	assertTrue(m.containsValue("null"));
	assertEquals(301, m.size());
	// m.validate("test1");

	for (int i = 0; i < 300; i++) {
	    assertEquals("" + i, m.get(new HC3(i)));
	}

	m.clear();
	// m.validate("cleared");
	assertEquals(0, m.size());
	assertNull(m.get(null));
    }

    @Test
    public void testRemove1() {
	FastHashMap2<HC3, String> m = new FastHashMap2<HC3, String>();
	for (int i = 0; i < 45; i += 4)
	    m.put(new HC3(i), "" + i);
	assertNull(m.remove(null));
	assertTrue(m.containsValue("0"));
	for (int i = 0; i < 45; i += 8) {
	    HC3 h = new HC3(i);
	    assertEquals(m.remove(h), "" + i);
	    // m.validate("Remove " + i);
	    assertNull(m.get(h));
	}
	assertFalse(m.containsValue("0"));
	for (int i = 4; i < 45; i += 8)
	    assertEquals(m.get(new HC3(i)), "" + i);
    }

    @Test
    public void testHashCode() {
	FastHashMap2<String, String> m = new FastHashMap2<String, String>();
	assertEquals(0, m.hashCode());
	m.put("Joe", "Blow");
	assertEquals("Joe".hashCode() ^ "Blow".hashCode(), m.hashCode());
    }

    @Test
    public void testKeySetRemove() {
	FastHashMap2<String, String> map = new FastHashMap2<String, String>();
	map.put("bananas", null);
	assertTrue(map.keySet().remove("bananas"));
    }

    @Test
    public void testSelfRef() {
	FastHashMapTest.testMap(new FastHashMap2<Object, Object>());
    }

    @Test
    public void testToString() {
	FastHashMap<String, String> m = new FastHashMap<String, String>();
	m.put(null, null);
	m.entrySet().iterator().next().toString();
    }

    @Test
    public void testReadWrite() throws IOException, ClassNotFoundException {
	ByteArrayOutputStream bos = new ByteArrayOutputStream();
	ObjectOutputStream oos = new ObjectOutputStream(bos);

	// Fill the map
	FastHashMap2<String, String> map = new FastHashMap2<String, String>();
	map.put("a", "1");
	map.put("b", "2");
	map.put("c", "3");
	map.put("d", "4");
	map.put(null, "5");
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
	FastHashMap2<String, String> read = (FastHashMap2<String, String>) ois
		.readObject();
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
	assertEquals("5", read.get(null));
    }

    @Test
    public void testNull() {
	FastHashMap2<String, String> map = new FastHashMap2<String, String>();
	assertEquals(0, map.size());
	map.clear();
	assertEquals(0, map.size());

	assertEquals(null, map.get(null));
	assertFalse(map.containsValue(null));
	assertFalse(map.containsValue("null"));
	assertEquals(null, map.put(null, "null"));
	assertEquals(1, map.size());
	assertFalse(map.containsValue(null));
	assertTrue(map.containsValue("null"));
	assertFalse(map.containsValue("test"));
	assertEquals("null", map.get(null));
	assertEquals("null", map.put(null, "test"));
	assertEquals(1, map.size());
	assertEquals("test", map.get(null));
	assertFalse(map.containsValue(null));
	assertFalse(map.containsValue("null"));
	assertTrue(map.containsValue("test"));

	assertEquals(null, map.get("null"));
	assertEquals(null, map.put("null", null));
	assertEquals(2, map.size());
	assertTrue(map.containsValue(null));
	assertFalse(map.containsValue("null"));
	assertTrue(map.containsValue("test"));
	assertEquals(null, map.get("null"));
	assertEquals(null, map.put("null", "test"));
	assertEquals(2, map.size());
	assertEquals("test", map.get("null"));
	assertFalse(map.containsValue(null));
	assertFalse(map.containsValue("null"));
	assertTrue(map.containsValue("test"));
    }

    @Test
    public void testEntrySetValue() {
	FastHashMap2<String, String> map = new FastHashMap2<String, String>();
	map.put("a", "b");
	Map.Entry<String, String> entry = map.entrySet().iterator().next();
	assertEquals("b", entry.setValue("x"));
	assertEquals("x", map.get("a"));
    }

    @Test
    public void testSetValue() {
	String key = "key";
	String oldValue = "old";
	String newValue = "new";
	FastHashMap2<String, String> m = new FastHashMap2<String, String>();
	m.put(key, oldValue);
	Map.Entry<String, String> e = (Map.Entry<String, String>) m.entrySet()
		.iterator().next();
	Object returnVal = e.setValue(newValue);
	assertEquals(oldValue, returnVal);
    }

    @Test
    public void testToArray2() {
	Map<String, String> m = new FastHashMap2<String, String>();
	m.put("french", "connection");
	m.put("polish", "sausage");
	Object[] mArray = m.entrySet().toArray();
	assertFalse(mArray[0] == mArray[1]);
	mArray[0].toString();
	mArray[1].toString();
    }

    @Test
    public void testRemovedEntry() {
	Map<String, String> m = new FastHashMap2<String, String>();
	assertEquals(null, m.put("1", "2"));
	Map.Entry<String, String> e = m.entrySet().iterator().next();
	assertEquals("2", e.setValue("3"));
	assertEquals("3", m.put("1", "4"));
	assertEquals("1=4", e.toString());
	assertEquals("4", m.put("1", "5"));
	assertEquals("1".hashCode() ^ "5".hashCode(), e.hashCode());
	assertEquals("5", m.put("1", "6"));
	Map.Entry<String, String> e2;
	e2 = new AbstractMap.SimpleImmutableEntry<String, String>("1", "6");
	assertEquals(e, e2);
	assertEquals("6", m.put("1", "7"));
	e2 = new AbstractMap.SimpleImmutableEntry<String, String>("1", "7");
	assertEquals(e2, e);
	assertEquals("7", m.remove("1"));
	assertEquals(null, m.put("a", "b"));
	assertEquals("1", e.getKey());
	assertEquals("7", e.getValue());
	assertEquals("7", e.setValue("8"));
	assertEquals("b", m.get("a"));
	assertEquals("8", e.getValue());
    }

    @Test
    public void testEmptyContainsValue() {
	Map<String, String> map = new FastHashMap2<String, String>();
	assertFalse(map.containsValue("test"));
    }

    @Test
    public void testEntrySetRemove() {
	Map<String, String> m = new FastHashMap2<String, String>();
	Set<Map.Entry<String, String>> entrySet = m.entrySet();
	m.put("a", "b");
	Map.Entry<String, String> e = new AbstractMap.SimpleEntry<String, String>(
		"a", "x");
	assertFalse(entrySet.remove(e));
	assertTrue(m.containsKey("a"));
	e.setValue("b");
	assertTrue(entrySet.remove(e));
	assertFalse(m.containsKey("a"));
    }

    @Test
    public void testClone() {
	FastHashMap2<String, String> map = new FastHashMap2<String, String>();
	map.put("a", "b");
	Set<String> mapKeys = map.keySet();
	FastHashMap2<String, String> clone = map.clone();
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

    @Test
    public void testRemove() {
	FastHashMap2<String, String> map = new FastHashMap2<String, String>();
	int c;
	Iterator<String> it;

	assertEquals(null, map.remove(null));
	assertEquals(null, map.remove("test"));
	assertEquals(null, map.remove(""));
	assertTrue(map.isEmpty());

	for (int i = 0; i < 20; i++) {
	    map.put("a" + i, i + "x");
	    map.put("b" + i, i + "y");
	    map.put("c" + i, i + "z");
	}
	assertEquals(60, map.size());

	for (int i = 0; i < 20; i++) {
	    assertEquals(i + "x", map.remove("a" + i));
	    assertFalse(map.containsValue(i + "x"));
	    assertEquals(59 - i, map.size());
	    c = 0;
	    for (it = map.keySet().iterator(); it.hasNext(); it.next())
		c++;
	    assertEquals(59 - i, c);
	}
	for (int i = 0; i < 20; i++) {
	    assertFalse(map.containsKey("a" + i));
	    assertEquals(i + "y", map.get("b" + i));
	    assertEquals(i + "z", map.get("c" + i));
	}

	for (int i = 0; i < 50; i++)
	    map.put("d" + i, "(" + i + ")");

	for (int i = 0; i < 20; i++) {
	    assertEquals(i + "y", map.remove("b" + i));
	    assertEquals(89 - i, map.size());
	}
	for (int i = 0; i < 20; i++) {
	    assertFalse(map.containsKey(i + "x"));
	    assertFalse(map.containsKey(i + "y"));
	    assertEquals(i + "z", map.get("c" + i));
	}

	for (int i = 0; i < 20; i++)
	    assertEquals("(" + i + ")", map.remove("d" + i));
	for (int i = 20; i < 50; i++)
	    assertEquals("(" + i + ")", map.get("d" + i));
	for (int i = 0; i < 20; i++)
	    assertEquals(i + "z", map.get("c" + i));

	assertEquals(50, map.size());
	assertEquals(null, map.remove(null));
	assertEquals(null, map.remove("test"));
	assertEquals(null, map.remove(""));
	assertEquals(50, map.size());

	for (int i = 20; i < 50; i++)
	    assertEquals("(" + i + ")", map.get("d" + i));
	for (int i = 0; i < 20; i++)
	    assertEquals(i + "z", map.get("c" + i));

	c = 0;
	for (it = map.keySet().iterator(); it.hasNext(); it.next())
	    c++;
	assertEquals(50, c);
    }

    @Test
    public void testToArray3() {
	FastHashMap2<Integer, Integer> map = new FastHashMap2<Integer, Integer>();
	Set<Map.Entry<Integer, Integer>> es = map.entrySet();
	assertEquals(0, es.toArray().length);
	assertEquals(null, es.toArray(new Object[] { Boolean.TRUE })[0]);
	map.put(7, 49);
	assertEquals(1, es.toArray().length);
	Object[] x = es.toArray(new Object[] { Boolean.TRUE, Boolean.TRUE });
	assertEquals(null, x[1]);
	@SuppressWarnings("unchecked")
	Map.Entry<Integer, Integer> e = (Map.Entry<Integer, Integer>) x[0];
	assertEquals(new Integer(7), e.getKey());
	assertEquals(new Integer(49), e.getValue());
    }

    @Test
    public void testOneBasket() {
	FastHashMap2<FastHashMapTest.ZeroHash, String> m = new FastHashMap2<FastHashMapTest.ZeroHash, String>();
	int n = 6;
	for (int i = 0; i < (1 << n); i++) {
	    m.clear();
	    for (int j = 0; j < n; j++)
		m.put(new FastHashMapTest.ZeroHash(j), "" + j);

	    int size = n;
	    for (int j = 0; j < n; j++)
		if ((i & (1 << j)) == 0) {
		    m.remove(new FastHashMapTest.ZeroHash(j));
		    size--;
		}

	    assertEquals(size, m.size());
	    for (int j = 0; j < n; j++)
		assertEquals((i & (1 << j)) != 0, m
			.containsKey(new FastHashMapTest.ZeroHash(j)));

	    //
	    Set<FastHashMapTest.ZeroHash> ks = m.keySet();
	    assertEquals(size, ks.size());
	    Iterator<FastHashMapTest.ZeroHash> ik = ks.iterator();
	    for (int j = 0; j < n; j++)
		if ((i & (1 << j)) != 0)
		    assertTrue((i & (1 << ik.next().n)) != 0);
	    assertFalse(ik.hasNext());
	}
    }
}
