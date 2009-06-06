import java.util.*;
import java.io.*;
import org.junit.*;
import static org.junit.Assert.*;

public class FastLinkedHashSetTest {

  // jdk test

  static Random rnd = new Random(666);

  static Set<Integer> clone(Set<Integer> s) {
    Set<Integer> clone;
    int method = rnd.nextInt(3);
    switch(method) {
      case 0: clone = ((FastLinkedHashSet<Integer>)s).clone(); break;
      case 1:
        @SuppressWarnings("unchecked")
        List<Integer> list = (List)Arrays.asList(s.toArray());
        clone = new FastLinkedHashSet<Integer>(list); break;
      default: clone = serClone(s); break;
    }
    assertEquals(s, clone);
    assertEquals(clone, s);
    assertTrue(s.containsAll(clone));
    assertTrue(clone.containsAll(s));
    return clone;
  }

  @SuppressWarnings("unchecked")
  private static Set<Integer> serClone(Set<Integer> m) {
    Set<Integer> result = null;
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
      result = (Set<Integer>)in.readObject();
      in.close();
    } catch(Exception e) {
      e.printStackTrace();
    }
    return result;
  }

  static void AddRandoms(Set<Integer> s, int n) {
    for (int i=0; i<n; i++) {
      int r = rnd.nextInt() % n;
      Integer e = new Integer(r < 0 ? -r : r);

      int preSize = s.size();
      boolean prePresent = s.contains(e);
      boolean added = s.add(e);
      assertTrue(s.contains(e));
      assertFalse(added == prePresent);
      int postSize = s.size();
      assertFalse(added && preSize == postSize);
      assertFalse(!added && preSize != postSize);
    }
  }

  @Test public void testBasic () {
    int numItr =  500;
    int setSize = 500;

    for (int i=0; i<numItr; i++) {
      Set<Integer> s1 = new FastLinkedHashSet<Integer>();
      AddRandoms(s1, setSize);

      Set<Integer> s2 = new FastLinkedHashSet<Integer>();
      AddRandoms(s2, setSize);

      Set<Integer> intersection = clone(s1);
      intersection.retainAll(s2);
      Set<Integer> diff1 = clone(s1); diff1.removeAll(s2);
      Set<Integer> diff2 = clone(s2); diff2.removeAll(s1);
      Set<Integer> union = clone(s1); union.addAll(s2);

      assertFalse(diff1.removeAll(diff2));
      assertFalse(diff1.removeAll(intersection));
      assertFalse(diff2.removeAll(diff1));
      assertFalse(diff2.removeAll(intersection));
      assertFalse(intersection.removeAll(diff1));
      assertFalse(intersection.removeAll(diff1));

      intersection.addAll(diff1); intersection.addAll(diff2);
      assertEquals(union, intersection);

      assertEquals(union.hashCode(), new FastLinkedHashSet<Integer>(union).hashCode());

      Iterator<Integer> e = union.iterator();
      while (e.hasNext())
        assertTrue(intersection.remove(e.next()));
      assertTrue(intersection.isEmpty());

      e = union.iterator();
      while (e.hasNext()) {
        Object o = e.next();
        assertTrue(union.contains(o));
        e.remove();
        assertFalse(union.contains(o));
      }
      assertTrue(union.isEmpty());

      s1.clear();
      assertTrue(s1.isEmpty());
    }
  }

}
