import java.util.*;
import java.io.Serializable;

public class FastHashMap<K,V>
implements Cloneable // Map<K,V> // Serializable
{
    /**
     * The default initial capacity - MUST be a power of two.
     */
    static final int DEFAULT_INITIAL_CAPACITY = 16;

    /**
     * The load factor used when none specified in constructor.
     */
    static final float DEFAULT_LOAD_FACTOR = 0.75f;

    /**
     * The load factor for the hash table.
     *
     * @serial
     */
    final float loadFactor;

    /**
     * Applies a supplemental hash function to a given hashCode, which
     * defends against poor quality hash functions.  This is critical
     * because HashMap uses power-of-two length hash tables, that
     * otherwise encounter collisions for hashCodes that do not differ
     * in lower bits. Note: Null keys always map to hash 0, thus index 0.
     */
    static int hash(Object o) {
        // This function ensures that hashCodes that differ only by
        // constant multiples at each bit position have a bounded
        // number of collisions (approximately 8 at default load factor).
        int h = o == null ? 0 : o.hashCode();
        return (h >>> 20) ^ (h >>> 12) ^ (h >>> 7) ^ (h >>> 4) ^ h;
    }

    private int counter = 0;
    private Object[] myKeys;
    private Object[] myValues;
    private int[] myIndices;
    private int firstEmptyIndex = 0;
    private int firstDeletedIndex = -1;

    private int hashLen;
    private int valueLen;

    /**
     * Constructs an empty <tt>HashMap</tt> with the default initial capacity
     * (16) and the default load factor (0.75).
     */
    public FastHashMap() {
        loadFactor = DEFAULT_LOAD_FACTOR;
        hashLen = DEFAULT_INITIAL_CAPACITY;
        valueLen = (int)(hashLen * loadFactor);
        myKeys = new Object[valueLen];
        myValues = new Object[valueLen];
        myIndices = new int[hashLen+valueLen];
    }

    /**
     * Constructs an empty <tt>HashMap</tt> with the specified initial
     * capacity and load factor.
     *
     * @param  initialCapacity the initial capacity
     * @param  loadFactor      the load factor
     * @throws IllegalArgumentException if the initial capacity is negative
     *         or the load factor is nonpositive
     */
    public FastHashMap(int initialCapacity, float loadFactor) {
        // ToDo: throw new IllegalArgumentException
        this.loadFactor = loadFactor;
        for (hashLen = DEFAULT_INITIAL_CAPACITY;
             hashLen < initialCapacity;
             hashLen <<= 1);
        valueLen = (int)(hashLen * loadFactor);
        myKeys = new Object[valueLen];
        myValues = new Object[valueLen];
        myIndices = new int[hashLen+valueLen];
    }

    /**
     * Constructs an empty <tt>HashMap</tt> with the specified initial
     * capacity and the default load factor (0.75).
     *
     * @param  initialCapacity the initial capacity.
     * @throws IllegalArgumentException if the initial capacity is negative.
     */
    public FastHashMap(int initialCapacity) {
        this(initialCapacity, DEFAULT_LOAD_FACTOR);
    }

    private void resize() {
        int newHashLen = hashLen << 1;
        int newValueLen = (int)(newHashLen * loadFactor);
        Object[] newKeys = new Object[newValueLen];
        System.arraycopy(myKeys, 0, newKeys, 0, counter);
        Object[] newValues = new Object[newValueLen];
        System.arraycopy(myValues, 0, newValues, 0, counter);
        int[] newIndices = new int[newHashLen+newValueLen];
        Arrays.fill(newIndices, newHashLen, newHashLen+counter, 1);
        int mask = 0x7FFFFFFF ^ (hashLen-1);
        int newMask = 0x7FFFFFFF ^ (newHashLen-1);
        for (int i = 0; i < hashLen; i++) {
            int next1 = 0;
            int next2 = 0;
            int arrayIndex;
            for (int j = ~myIndices[i];
                j >= 0;
                j = ~myIndices[hashLen + arrayIndex])
            {
                arrayIndex = j & (hashLen-1);
                int hashIndex = i | (j & (newMask ^ mask));
                if (hashIndex == i) {
                    if (next1 < 0) newIndices[newHashLen + arrayIndex] = next1;
                    next1 = ~(arrayIndex | (j & newMask));
                } else {
                    if (next2 < 0) newIndices[newHashLen + arrayIndex] = next2;
                    next2 = ~(arrayIndex | (j & newMask));
                }
            }
            if (next1 < 0) newIndices[i] = next1;
            if (next2 < 0) newIndices[i + hashLen] = next2;
        }
        hashLen = newHashLen;
        valueLen = newValueLen;
        myKeys = newKeys;
        myValues = newValues;
        myIndices = newIndices;
    }

    private int positionOf(Object elem) {
        int hc = hash(elem);
        int mask = 0x7FFFFFFF ^ (hashLen-1);
        int hcBits = hc & mask;
        for (int i = ~myIndices[hc & (hashLen-1)];
             i >= 0;
             i = ~myIndices[hashLen + (i & (hashLen-1))])
        {
            if (hcBits != (i & mask)) continue;
            Object x = myKeys[i & (hashLen-1)];
            if (x == elem || x != null && x.equals(elem))
                return i & (hashLen-1);
        }
        return -1;
    }

    /**
     * Associates the specified value with the specified key in this map.
     * If the map previously contained a mapping for the key, the old
     * value is replaced.
     *
     * @param key key with which the specified value is to be associated
     * @param value value to be associated with the specified key
     * @return the previous value associated with <tt>key</tt>, or
     *         <tt>null</tt> if there was no mapping for <tt>key</tt>.
     *         (A <tt>null</tt> return can also indicate that the map
     *         previously associated <tt>null</tt> with <tt>key</tt>.)
     */
    public V put(K key, V value) {
        int hc = hash(key);
        int i = hc & (hashLen - 1);
        int next = myIndices[i];
        int mask = 0x7FFFFFFF ^ (hashLen-1);
        int hcBits = hc & mask;
        //
        int k;
        for (int j = ~next; j >= 0; j = ~myIndices[hashLen+k]) {
            k = j & (hashLen - 1);
            if (hcBits == (j & mask)) {
                Object o = myKeys[k];
                if (o == key || o != null && o.equals(key)) {
                    Object oldValue = myValues[k];
                    myValues[k] = value;
                    return (V)oldValue;
                }
            }
        }
        //
        if (counter >= valueLen) {
            resize();
            i = hc & (hashLen - 1);
            next = myIndices[i];
            mask = 0x7FFFFFFF ^ (hashLen-1);
            hcBits = hc & mask;
        }
        int newIndex;
        if (firstDeletedIndex >= 0) {
            newIndex = firstDeletedIndex;
            firstDeletedIndex = myIndices[hashLen+firstDeletedIndex];
            if (firstDeletedIndex >= 2)
                firstDeletedIndex -= 2;
            else
                firstDeletedIndex = -1;
        } else {
            newIndex = firstEmptyIndex;
            firstEmptyIndex++;
        }
        myValues[newIndex] = value;
        myKeys[newIndex] = key;
        myIndices[hashLen + newIndex] = next < 0 ? next : 1;
        myIndices[i] = ~(newIndex | hcBits);
        counter++;
        return null;
    }

    /**
     * Removes the mapping for the specified key from this map if present.
     *
     * @param  key key whose mapping is to be removed from the map
     * @return the previous value associated with <tt>key</tt>, or
     *         <tt>null</tt> if there was no mapping for <tt>key</tt>.
     *         (A <tt>null</tt> return can also indicate that the map
     *         previously associated <tt>null</tt> with <tt>key</tt>.)
     */
    public V remove(Object key) {
      int hc = hash(key);
      int h = hc & (hashLen-1);
      int i0 = ~myIndices[h];
      int mask = 0x7FFFFFFF ^ (hashLen-1);
      int hcBits = hc & mask;
      int prev = -1;
      for (int i = i0; i >= 0; i = ~myIndices[hashLen+prev]) {
          if (hcBits == (i & mask)) {
              Object o = myKeys[i & (hashLen-1)];
              if (o == key || o != null && o.equals(key)) {
                  i &= hashLen - 1;
                  counter--;
                  if (prev >= 0)
                      myIndices[hashLen+prev] = myIndices[hashLen+i];
                  else
                      myIndices[h] = myIndices[hashLen+i];
                  if (i == firstEmptyIndex-1) {
                      firstEmptyIndex = i;
                      myIndices[hashLen+i] = 0;
                  } else if (firstDeletedIndex == hashLen-2) {
                      myIndices[hashLen+i] = myIndices[hashLen+firstDeletedIndex];
                      myIndices[hashLen+firstDeletedIndex] = i+2;
                  } else {
                      myIndices[hashLen+i] = firstDeletedIndex < 0 ? 0 : firstDeletedIndex+2;
                      firstDeletedIndex = i;
                  }
                  Object oldValue = myValues[i];
                  myKeys[i] = null;
                  myValues[i] = null;
                  return (V)oldValue;
              }
          }
          prev = i & (hashLen - 1);
      }
      return null;
    }

    /**
     * Removes all of the mappings from this map.
     * The map will be empty after this call returns.
     */
    public void clear() {
        Arrays.fill(myKeys, 0, firstEmptyIndex, null);
        Arrays.fill(myValues, 0, firstEmptyIndex, null);
        Arrays.fill(myIndices, 0, hashLen + firstEmptyIndex, 0);
        counter = 0;
        firstEmptyIndex = 0;
        firstDeletedIndex = -1;
    }

    /**
     * Returns a shallow copy of this <tt>HashMap</tt> instance: the keys and
     * values themselves are not cloned.
     *
     * @return a shallow copy of this map
     */
    public Object clone() {
        FastHashMap<K,V> that = new FastHashMap<K,V>(hashLen, loadFactor);
        that.counter = counter;
        that.firstEmptyIndex = firstEmptyIndex;
        that.firstDeletedIndex = firstDeletedIndex;
        System.arraycopy(myKeys, 0, that.myKeys, 0, firstEmptyIndex);
        System.arraycopy(myValues, 0, that.myValues, 0, firstEmptyIndex);
        System.arraycopy(myIndices, 0, that.myIndices, 0, hashLen+firstEmptyIndex);
        return that;
    }

    /**
     * Returns the number of key-value mappings in this map.
     *
     * @return the number of key-value mappings in this map
     */
    public int size() {
        return counter;
    }

    /**
     * Returns <tt>true</tt> if this map contains no key-value mappings.
     *
     * @return <tt>true</tt> if this map contains no key-value mappings
     */
    public boolean isEmpty() {
        return counter == 0;
    }

    /**
     * Returns the value to which the specified key is mapped,
     * or {@code null} if this map contains no mapping for the key.
     *
     * <p>More formally, if this map contains a mapping from a key
     * {@code k} to a value {@code v} such that {@code (key==null ? k==null :
     * key.equals(k))}, then this method returns {@code v}; otherwise
     * it returns {@code null}.  (There can be at most one such mapping.)
     *
     * <p>A return value of {@code null} does not <i>necessarily</i>
     * indicate that the map contains no mapping for the key; it's also
     * possible that the map explicitly maps the key to {@code null}.
     * The {@link #containsKey containsKey} operation may be used to
     * distinguish these two cases.
     *
     * @see #put(Object, Object)
     */
    public V get(Object key) {
        int i = positionOf(key);
        return i >= 0 ? (V)myValues[i] : null;
    }

    /**
     * Returns <tt>true</tt> if this map contains a mapping for the
     * specified key.
     *
     * @param   key   The key whose presence in this map is to be tested
     * @return <tt>true</tt> if this map contains a mapping for the specified
     * key.
     */
    public boolean containsKey(Object key) {
        return positionOf(key) >= 0;
    }

    /**
     * Copies all of the mappings from the specified map to this map.
     * These mappings will replace any mappings that this map had for
     * any of the keys currently in the specified map.
     *
     * @param m mappings to be stored in this map
     * @throws NullPointerException if the specified map is null
     */
    public void putAll(Map<? extends K, ? extends V> m) {
        for (Iterator<? extends Map.Entry<? extends K, ? extends V>> i = m.entrySet().iterator(); i.hasNext(); ) {
            Map.Entry<? extends K, ? extends V> e = i.next();
            put(e.getKey(), e.getValue());
        }
    }

    /**
     * Returns <tt>true</tt> if this map maps one or more keys to the
     * specified value.
     *
     * @param value value whose presence in this map is to be tested
     * @return <tt>true</tt> if this map maps one or more keys to the
     *         specified value
     */
    public boolean containsValue(Object value) {
        for (int i = 0; i < firstEmptyIndex ; i++) {
            int next = myIndices[hashLen+i];
            if (next == 1 || next < 0) {
                Object o = myValues[i];
                if (o == value || o != null && o.equals(value))
                    return true;
            }
        }
        return false;
    }
}
