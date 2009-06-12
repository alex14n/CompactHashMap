public class FastOpenHashMap<K,V> {
  boolean hasNullKey;
  V nullValue;

  int [] indexTable;
  Object [] keyValueTable;

  transient int size;
  public int size() { return size; }

  int threshold;
  final float loadFactor = DEFAULT_LOAD_FACTOR;
  transient int hashLen = DEFAULT_INITIAL_CAPACITY;

  static final int DEFAULT_INITIAL_CAPACITY = 4;
  static final float DEFAULT_LOAD_FACTOR = 0.375f; // 0.75f;
  final static int AVAILABLE_BITS = 0x7FFFFFFF;
  final static int END_OF_LIST = 0x80000000;

  final static int hash(int h) {
    h += ~(h << 9);
    h ^= (h >>> 14);
    h += (h << 4);
    return h ^ (h >>> 10);
  }

  final static int hash(Object o) {
      return o == null ? 0 : hash(o.hashCode());
  }

  void init() {
    threshold = (int)(hashLen * loadFactor);
    indexTable = new int [hashLen];
    keyValueTable = new Object [hashLen<<1];
    size = hasNullKey ? 1 : 0;
  }

  public FastOpenHashMap() {
    init();
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
    // Compute hash index
    int hc = hash(key);
    int index = hc & (hashLen - 1);
    // Try direct lookup
    Object key1 = keyValueTable[index<<1];
    if (key1 == null) {
      keyValueTable[index<<1] = key;
      keyValueTable[(index<<1)+1] = value;
      indexTable[index] = hc | END_OF_LIST;
      size++;
      return null;
    }
    // Resize?
    if (size >= threshold) {
      resize();
      return put (key, value);
    }
    // Are there any keys at all in this hash bin?
    int hc1 = indexTable[index];
    if ((hc & (hashLen-1)) != (hc1 & (hashLen-1))) {
      // Find an empty spot
      int i = (index+1)&(hashLen-1);
      while (keyValueTable[i<<1] != null) {
        if ((hc1 & END_OF_LIST) == 0) {
          // Relocate 'end-of-list' flag for collision
          int hc2 = indexTable[i];
          if ((hc2 & (hashLen-1)) == (hc1 & (hashLen-1)) &&
              (hc2 & END_OF_LIST) != 0) {
            indexTable[i] = hc2 & ~END_OF_LIST;
            hc1 |= END_OF_LIST;
          }
        }
        i = (i+1)&(hashLen-1);
      }
      // Relocate collision
      keyValueTable[i<<1] = key1;
      keyValueTable[(i<<1)+1] = keyValueTable[(index<<1)+1];
      indexTable[i] = hc1;
      // Insert this key/value
      keyValueTable[index<<1] = key;
      keyValueTable[(index<<1)+1] = value;
      indexTable[index] = hc | END_OF_LIST;
      size++;
      // validate();
      return null;
    }
    // Start searching
    int index0 = index;
    while (true) {
      // Compare the found key
      if ((hc & AVAILABLE_BITS) == (hc1 & AVAILABLE_BITS) &&
          key == key1 || key.equals(key1)) {
        @SuppressWarnings("unchecked")
        V oldValue = (V)keyValueTable[(index<<1)+1];
        keyValueTable[(index<<1)+1] = value;
        return oldValue;
      }
      // This is the last key in this hash bin
      if ((hc & (hashLen-1)) == (hc1 & (hashLen-1)) &&
          (hc1 & END_OF_LIST) != 0) {
        // Find an empty spot
        int i = (index+1)&(hashLen-1);
        while (keyValueTable[i<<1] != null)
          i = (i+1)&(hashLen-1);
        // Insert new key/value pair
        keyValueTable[i<<1] = key;
        keyValueTable[(i<<1)+1] = value;
        indexTable[i] = hc | END_OF_LIST;
        indexTable[index] = hc1 & ~END_OF_LIST;
        size++;
        // validate();
        return null;
      }
      // Step forward
      index = (index+1)&(hashLen-1);
      if (index == index0)
        throw new RuntimeException("END_OF_LIST not set");
      key1 = keyValueTable[index<<1];
      hc1 = indexTable[index];
    }
  }

  public V get (K key) {
    // Null special case
    if (key == null) {
      if (!hasNullKey) {
        hasNullKey = true;
        size++;
      }
      return nullValue;
    }
    // Compute hash index
    int hc = hash(key);
    int index = hc & (hashLen - 1);
    // Try direct lookup
    Object key1 = keyValueTable[index<<1];
    if (key1 == null)
      return null;
    // Are there any keys at all in this hash bin?
    int hc1 = indexTable[index];
    if ((hc & (hashLen-1)) != (hc1 & (hashLen-1)))
      return null;
    // Start searching
    while (true) {
      // Compare the found key
      if ((hc & AVAILABLE_BITS) == (hc1 & AVAILABLE_BITS) &&
          key == key1 || key.equals(key1)) {
        @SuppressWarnings("unchecked")
        V value = (V)keyValueTable[(index<<1)+1];
        return value;
      }
      // This was the only key in this hash bin
      if ((hc & (hashLen-1)) == (hc1 & (hashLen-1)) &&
          (hc1 & END_OF_LIST) != 0)
        return null;
      // Step forward
      index = (index+1)&(hashLen-1);
      key1 = keyValueTable[index<<1];
      hc1 = indexTable[index];
    }
  }
/*
  void validate() {
    // System.out.println("Validate, size="+size+", hashLen="+hashLen);
    int n = hasNullKey ? 1 : 0;
    int mask = AVAILABLE_BITS ^ (hashLen-1);
    for (int i = 0; i < hashLen; i++) {
      Object key = keyValueTable[i<<1];
      if (key != null) {
        int index = indexTable[i];
        if ((index & (hashLen-1)) != i)
          continue;
        // Object value = keyValueTable[(i<<1)+1];
        // System.out.println("Basket "+key+"="+value);
        int hc = hash(key);
        if ((hc & (hashLen-1)) != i)
          throw new RuntimeException("Key "+key+" in wrong hash basket");
        if((index & mask) != (hc & mask))
          throw new RuntimeException("Key "+key+" has wrong hashcode bits");
        n++;
        if ((index & END_OF_LIST) != 0)
          continue;
        int j = (index+1)&(hashLen-1);
        while (true) {
          index = indexTable[j];
          if ((index & (hashLen-1)) == i) {
            key = keyValueTable[j<<1];
            // value = keyValueTable[(i<<1)+1];
            // System.out.println("Overflow "+key+"="+value);
            hc = hash(key);
            if ((hc & (hashLen-1)) != i)
              throw new RuntimeException("Overflow Key "+key+" in wrong hash basket");
            if((index & mask) != (hc & mask))
              throw new RuntimeException("Overflow Key "+key+" has wrong hashcode bits");
            n++;
            if ((index & END_OF_LIST) != 0) break;
          }
          j = (j+1)&(hashLen-1);
          if (j == i)
            throw new RuntimeException("END_OF_LIST not set in hash bin "+i);
        }
      }
    }
    if (n != size)
      throw new RuntimeException("Size="+size+", elements="+n+", hasNullKey="+hasNullKey);
  }
*/
  public boolean containsKey (K key) {
    if (key == null)
      return hasNullKey;
    else
      // ToDo: null value case?
      return get(key) != null;
  }

  void resize() {
    // ToDo: optimize (use cached hashcodes)
    Object[] oldTable = keyValueTable;
    int len = hashLen;
    hashLen <<= 1;
    init();
    for (int i = 0; i < len; i++) {
      @SuppressWarnings("unchecked")
      K key = (K)oldTable[i<<1];
      if (key != null) {
        @SuppressWarnings("unchecked")
        V value = (V)oldTable[(i<<1)+1];
        put (key, value);
      }
    }
    // validate();
  }
}
