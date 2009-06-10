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
public class OpenHashMap<K,V>
//  implements Cloneable, Serializable, Map<K,V>
{
  static final float DEFAULT_LOAD_FACTOR = 0.4f;
  static final int DEFAULT_INITIAL_CAPACITY = 8;

  boolean hasNullKey;
  V nullValue;

  int [] headHash;
  Object [] keyValue;

  transient int size;
  public int size() { return size; }
  int threshold;
  final float loadFactor = DEFAULT_LOAD_FACTOR;
  transient int hashLen = DEFAULT_INITIAL_CAPACITY;

  void init() {
    threshold = (int)(hashLen * loadFactor);
    headHash = new int [hashLen];
    keyValue = new Object [hashLen<<1];
  }

  public OpenHashMap() {
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
    //
    while (true) {
      Object key1 = keyValue[index<<1];
      if (key1 == null) {
        return null;
      }
      if (key1 == key || key.equals(key1)) {
        return (V)keyValue[(index<<1)+1];
      }
      index = (index + 1) & (hashLen - 1);
    }
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
    //
    if (size >= threshold) {
      resize();
    }
    // Hash index
    int hc = FastHashMap.hash(key);
    int index = hc & (hashLen - 1);
    //
    while (true) {
      Object key1 = keyValue[index<<1];
      if (key1 == null) {
        keyValue[index<<1] = key;
        keyValue[(index<<1)+1] = value;
        headHash[index] = hc;
        size++;
        return null;
      }
      if (key1 == key || key.equals(key1)) {
        V oldValue = (V)keyValue[(index<<1)+1];
        keyValue[(index<<1)+1] = value;
        return oldValue;
      }
      index = (index + 1) & (hashLen - 1);
    }
  }
/*
  V remove (K key) {
    // ToDo: implement
    return null;
  }
*/
  void resize() {
    Object[] oldTable = keyValue;
    int[] oldHash = headHash;
    int len = hashLen;
    hashLen <<= 1;
    init();
    for (int i = 0; i < len; i++) {
      K key = (K)oldTable[i<<1];
      if (key != null) {
        int hc = oldHash[i];
        V value = (V)oldTable[(i<<1)+1];
        int newIndex = hc & (hashLen-1);
        while (keyValue[newIndex<<1] != null)
          newIndex = (newIndex + 1) & (hashLen - 1);
        headHash[newIndex] = hc;
        keyValue[newIndex<<1] = key;
        keyValue[(newIndex<<1)+1] = value;
      }
    }
  }

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
