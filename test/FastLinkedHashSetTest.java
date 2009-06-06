import java.util.*;
import java.io.*;
import org.junit.*;
import static org.junit.Assert.*;

public class FastLinkedHashSetTest {

  // jdk test

  static Random rnd = new Random(666);

  static Set clone(Set s) {
    Set clone;
    int method = rnd.nextInt(3);
    clone = (method==0 ?  (Set) ((FastLinkedHashSet)s).clone() :
            (method==1 ? new FastLinkedHashSet(Arrays.asList(s.toArray())) :
            serClone(s)));
    assertEquals(s, clone);
    assertEquals(clone, s);
    assertTrue(s.containsAll(clone));
    assertTrue(clone.containsAll(s));
    return clone;
  }

  private static Set serClone(Set m) {
    Set result = null;
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
      result = (Set)in.readObject();
      in.close();
    } catch(Exception e) {
      e.printStackTrace();
    }
    return result;
  }

  static void AddRandoms(Set s, int n) {
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
      Set s1 = new FastLinkedHashSet();
      AddRandoms(s1, setSize);

      Set s2 = new FastLinkedHashSet();
      AddRandoms(s2, setSize);

      Set intersection = clone(s1);
      intersection.retainAll(s2);
      Set diff1 = clone(s1); diff1.removeAll(s2);
      Set diff2 = clone(s2); diff2.removeAll(s1);
      Set union = clone(s1); union.addAll(s2);

      assertFalse(diff1.removeAll(diff2));
      assertFalse(diff1.removeAll(intersection));
      assertFalse(diff2.removeAll(diff1));
      assertFalse(diff2.removeAll(intersection));
      assertFalse(intersection.removeAll(diff1));
      assertFalse(intersection.removeAll(diff1));

      intersection.addAll(diff1); intersection.addAll(diff2);
      assertEquals(union, intersection);

      assertEquals(union.hashCode(), new FastLinkedHashSet(union).hashCode());

      Iterator e = union.iterator();
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
