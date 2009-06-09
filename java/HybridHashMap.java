import java.io.Serializable;
import java.util.Map;

/**
 * Improvement for FastHashMap based on discussion in
 * http://mail.openjdk.java.net/pipermail/core-libs-dev/2009-June/001788.html
 *
 * 1) We don't store null keys in table and handle null keys specially
 *    thus if keyValueTable entry is null => it's empty
 * 2)
 *
 * @author  Doug Lea
 * @author  Alex Yakovlev
 *
 * @param <K>
 * @param <V>
 */
public class HybridHashMap<K,V>
//  implements Cloneable, Serializable, Map<K,V>
{
  boolean hasNullKey;
  V nullValue;

  int [] headHash;
  Object [] keyValueNext;

  transient int size;
  public int size() { return size; }
  int threshold;
  final float loadFactor = FastHashMap.DEFAULT_LOAD_FACTOR;
  transient int hashLen = FastHashMap.DEFAULT_INITIAL_CAPACITY;

  void init() {
    threshold = (int)(hashLen * loadFactor);
    headHash = new int [hashLen];
    keyValueNext = new Object [hashLen*3];
    size = hasNullKey ? 1 : 0;
  }

  public HybridHashMap() {
    init();
  }

  public V get (K key) {
    // Null special case
    if (key == null) {
      return nullValue;
    }
    // Hash index for this key
    int hc = FastHashMap.hash(key);
    int index = hc & (hashLen - 1);
    int index3 = (index<<1)+index;
    // Try direct lookup
    Object key1 = keyValueNext[index3];
    if (key1 == null) {
      return null;
    }
    if (key1 == key || key.equals(key1)) {
      return (V)keyValueNext[index3+1];
    }
    // Look for collisions
    Entry<K,V> entry = (Entry<K,V>)keyValueNext[index3+2];
    while (entry != null) { 
      if (entry.hc == hc && (entry.key == key || key.equals(entry.key))) {
        return entry.value;
      }
      entry = entry.next;
    }
    return null;
  }

  public boolean containsKey (K key) {
    if (key == null) return hasNullKey;
    // ToDo: null value case?
    return get(key) != null;
  }

  public V put (K key, V value) {
    // Null special case
    if (key == null) {
      if (!hasNullKey) {
        hasNullKey = true;
        size++;
      }
      V oldValue = nullValue;
      nullValue = value;
      return oldValue;
    }
    // Hash index
    int hc = FastHashMap.hash(key);
    int index = hc & (hashLen - 1);
    int index3 = (index<<1)+index;
    // Try direct lookup
    Object key1 = keyValueNext[index3];
    if (key1 == null) {
      keyValueNext[index3] = key;
      keyValueNext[index3+1] = value;
      headHash[index] = hc;
      size++;
      // validate();
      return null;
    }
    if (key1 == key || key.equals(key1)) {
      V oldValue = (V)keyValueNext[index3+1];
      keyValueNext[index3+1] = value;
      return oldValue;
    }
    // Look for collisions
    Entry<K,V> entry = (Entry<K,V>)keyValueNext[index3+2];
    for (Entry<K,V> e = entry; e != null; e = e.next) { 
      if (e.hc == hc && (e.key == key || key.equals(e.key))) {
        V oldValue = e.value;
        e.value = value;
        return oldValue;
      }
    }
    if (size >= threshold) {
      resize();
      return put (key, value);
    }
    keyValueNext[index3+2] = new Entry (key, value, hc, entry);
    size++;
    // validate();
    return null;
  }
/*
  V remove (K key) {
    // ToDo: implement
    return null;
  }
*/
  void resize() {
    // ToDo: optimize, current version is very slow
    Object[] oldTable = keyValueNext;
    int[] oldHash = headHash;
    int len3 = (hashLen<<1)+hashLen;
    hashLen <<= 1;
    init();
    for (int i = 0; i < len3; i+=3) {
      K key = (K)oldTable[i];
      if (key != null) {
        V value = (V)oldTable[i+1];
        put (key, value);
        Entry<K,V> entry = (Entry<K,V>)oldTable[i+2];
        while (entry != null) {
          put (entry.key, entry.value);
          entry = entry.next;
        }
      }
    }
    // validate();
  }
/*
  void validate() {
    int n = hasNullKey ? 1 : 0;
    for (int i = 0, i3=0; i < hashLen; i++, i3+=3) {
      Object key = keyValueNext[i3];
      if (key != null) {
        // Object value = keyValueTable[(i<<1)+1];
        // System.out.println("Basket "+key+"="+value);
        n++;
        int hc = FastHashMap.hash(key);
        if ((hc & (hashLen-1)) != i)
          throw new RuntimeException("Key "+key+" in wrong hash basket");
        int hash = headHash[i];
        if(hash != hc)
          throw new RuntimeException("Key "+key+" has wrong hashcode");
        Entry<K,V> entry = (Entry<K,V>)keyValueNext[i3+2];
        while (entry != null) {
          n++;
          // value = keyValueTable[((hashLen + (index & (hashLen-1)))<<1)+1];
          // System.out.println("Overflow "+key+"="+value);
          hc = FastHashMap.hash(entry.key);
          if ((hc & (hashLen-1)) != i)
            throw new RuntimeException("Overflow key "+key+" in wrong hash basket");
          if(entry.hc != hc)
            throw new RuntimeException("Overflow key "+key+" has wrong hashcode");
          entry = entry.next;
        }
      }
    }
    if (n != size)
      throw new RuntimeException("Size="+size+", elements="+n+", hasNullKey="+hasNullKey);
  }
*/
  final static class Entry<K,V> {
    final K key;
    final int hc;
    V value;
    Entry<K,V> next;

    Entry (K key, V value, int hc, Entry<K,V> next) {
      this.key = key;
      this.hc = hc;
      this.value = value;
      this.next = next;
    }
  }
}
