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

  int [] indexTable;
  Object [] keyValueTable;

  transient int size;
  public int size() { return size; }
  transient int firstEmptyIndex;
  // transient int firstDeletedIndex;
  int threshold;
  final float loadFactor = FastHashMap.DEFAULT_LOAD_FACTOR;
  transient int hashLen = FastHashMap.DEFAULT_INITIAL_CAPACITY;

  /**
   * We don't need `deleted' bit here
   */
  final static int AVAILABLE_BITS = 0x7FFFFFFF;

  /**
   *
   */
  final static int END_OF_LIST = 0x80000000;

  void init() {
    threshold = (int)(hashLen * loadFactor);
    // ToDo: separate main and overflow (collisions) parts
    // and allocate them separately or even store it
    // in HashEntry-like structure? or ternary tries?
    indexTable = new int [(hashLen + threshold)];
    keyValueTable = new Object [(hashLen + threshold)<<1];
    firstEmptyIndex = 0;
    // firstDeletedIndex = -1;
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
    // Try direct lookup
    Object key1 = keyValueTable[index<<1];
    if (key1 == null) return null;
    if (key1 == key || key.equals(key1))
      return (V)keyValueTable[(index<<1)+1];
    // Look for collisions
    int mask = AVAILABLE_BITS ^ (hashLen-1);
    int i = indexTable[index];
    do {
      if ((i & END_OF_LIST) != 0) return null;
      index = i & (hashLen - 1);
      i = indexTable[hashLen + index];
      if ((i & mask) == (hc & mask)) {
        key1 = keyValueTable[(hashLen + index)<<1];
        if (key1 == key || key.equals(key1)) {
          return (V)keyValueTable[((hashLen + index)<<1)+1];
        }
      }
    } while(true);
  }

  public boolean containsKey (K key) {
    if (key == null)
      return hasNullKey;
    else
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
    int index0 = hc & (hashLen - 1);
    // Try direct lookup
    Object key1 = keyValueTable[index0<<1];
    if (key1 == null) {
      keyValueTable[index0<<1] = key;
      keyValueTable[(index0<<1)+1] = value;
      indexTable[index0] = hc | END_OF_LIST; // & AVAILABLE_BITS
      size++;
      // validate();
      return null;
    }
    if (key1 == key || key.equals(key1)) {
      V oldValue = (V)keyValueTable[(index0<<1)+1];
      keyValueTable[(index0<<1)+1] = value;
      return oldValue;
    }
    // Look for collisions
    int mask = AVAILABLE_BITS ^ (hashLen-1);
    int i0 = indexTable[index0];
    int i = i0;
    int index = index0;
    do {
      if ((i & END_OF_LIST) != 0) {
        if (size >= threshold) { // firstEmptyIndex >= threshold ?
          resize();
          return put (key, value);
        }
        // ToDo: deleted index ...
        indexTable[hashLen + firstEmptyIndex] = (hc & mask) | (i0 & ~mask);
        indexTable[index0] = (i0 & mask) | firstEmptyIndex;
        keyValueTable[(hashLen + firstEmptyIndex)<<1] = key;
        keyValueTable[((hashLen + firstEmptyIndex)<<1)+1] = value;
        size++;
        firstEmptyIndex++;
        // validate();
        return null;
      }
      index = i & (hashLen - 1);
      i = indexTable[hashLen + index];
      if ((i & mask) == (hc & mask)) {
        key1 = keyValueTable[(hashLen + index)<<1];
        if (key1 == key || key.equals(key1)) {
          V oldValue = (V)keyValueTable[((hashLen + index)<<1)+1];
          keyValueTable[((hashLen + index)<<1)+1] = value;
          return oldValue;
        }
      }
    } while(true);
  }
/*
  V remove (K key) {
    // ToDo: implement
    return null;
  }
*/
  void resize() {
    // ToDo: optimize, current version is very slow
    Object[] oldTable = keyValueTable;
    int len = hashLen + firstEmptyIndex;
    hashLen <<= 1;
    init();
    for (int i = 0; i < len; i++) {
      K key = (K)oldTable[i<<1];
      if (key != null) {
        V value = (V)oldTable[(i<<1)+1];
        put (key, value);
      }
    }
    // validate();
  }
/*
  void validate() {
    int n = hasNullKey ? 1 : 0;
    int mask = AVAILABLE_BITS ^ (hashLen-1);
    for (int i = 0; i < hashLen; i++) {
      Object key = keyValueTable[i<<1];
      if (key != null) {
        // Object value = keyValueTable[(i<<1)+1];
        // System.out.println("Basket "+key+"="+value);
        n++;
        int hc = FastHashMap.hash(key);
        if ((hc & (hashLen-1)) != i)
          throw new RuntimeException("Key "+key+" in wrong hash basket");
        int index = indexTable[i];
        if((index & mask) != (hc & mask))
          throw new RuntimeException("Key "+key+" has wrong hashcode bits");
        while ((index & END_OF_LIST) == 0) {
          n++;
          key = keyValueTable[(hashLen + (index & (hashLen-1)))<<1];
          // value = keyValueTable[((hashLen + (index & (hashLen-1)))<<1)+1];
          // System.out.println("Overflow "+key+"="+value);
          index = indexTable[hashLen + (index & (hashLen-1))];
          hc = FastHashMap.hash(key);
          if ((hc & (hashLen-1)) != i)
            throw new RuntimeException("Overflow key "+key+" in wrong hash basket");
          if((index & mask) != (hc & mask))
            throw new RuntimeException("Overflow key "+key+" has wrong hashcode bits");
        }
      }
    }
    if (n != size)
      throw new RuntimeException("Size="+size+", elements="+n+", hasNullKey="+hasNullKey);
  }
*/
}
