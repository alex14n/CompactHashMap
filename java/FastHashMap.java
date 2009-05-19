import java.util.*;
import java.io.Serializable;

public class FastHashMap<K,V>
    extends AbstractMap<K,V>
    implements Cloneable, Serializable, Map<K,V>
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
     * Applies a supplemental hash function to a given object's hashCode,
     * which defends against poor quality hash functions. This is critical
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
    private Object[] myKeyValues;
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
        myKeyValues = new Object[valueLen<<1];
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
        myKeyValues = new Object[valueLen<<1];
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

    final private void resize() {
        int newHashLen = hashLen << 1;
        int newValueLen = (int)(newHashLen * loadFactor);
        Object[] newKeyValues = new Object[newValueLen<<1];
        System.arraycopy(myKeyValues, 0, newKeyValues, 0, counter<<1);
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
        myKeyValues = newKeyValues;
        myIndices = newIndices;
    }

    final private int positionOf(Object elem) {
        int hc = hash(elem);
        int mask = 0x7FFFFFFF ^ (hashLen-1);
        int hcBits = hc & mask;
        for (int i = ~myIndices[hc & (hashLen-1)];
             i >= 0;
             i = ~myIndices[hashLen + (i & (hashLen-1))])
        {
            if (hcBits != (i & mask)) continue;
            Object x = myKeyValues[(i & (hashLen-1))<<1];
            if (x == elem || x != null && x.equals(elem))
                return i & (hashLen-1);
        }
        return -1;
    }

    final private boolean isEmpty(int i) {
        int next = myIndices[hashLen+i];
        return next == 0 || next >= 2;
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
        // Look if key is already in this map
        int k;
        for (int j = ~next; j >= 0; j = ~myIndices[hashLen+k]) {
            k = j & (hashLen - 1);
            if (hcBits == (j & mask)) {
                Object o = myKeyValues[k<<1];
                if (o == key || o != null && o.equals(key)) {
                    Object oldValue = myKeyValues[(k<<1)+1];
                    myKeyValues[(k<<1)+1] = value;
                    return (V)oldValue;
                }
            }
        }
        // Resize if needed
        if (counter >= valueLen) {
            resize();
            i = hc & (hashLen - 1);
            next = myIndices[i];
            mask = 0x7FFFFFFF ^ (hashLen-1);
            hcBits = hc & mask;
        }
        // Find a place for new element
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
        // Insert it
        myKeyValues[newIndex<<1] = key;
        myKeyValues[(newIndex<<1)+1] = value;
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
      V result = removeKey(key);
      return result == NOT_FOUND ? null : result;
    }

    private static Object NOT_FOUND = new Object();

    final private V removeKey(Object key) {
        int hc = hash(key);
        int h = hc & (hashLen-1);
        int i0 = ~myIndices[h];
        int mask = 0x7FFFFFFF ^ (hashLen-1);
        int hcBits = hc & mask;
        int prev = -1;
        for (int i = i0; i >= 0; i = ~myIndices[hashLen+prev]) {
            if (hcBits == (i & mask)) {
                Object o = myKeyValues[(i & (hashLen-1))<<1];
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
                    Object oldValue = myKeyValues[(i<<1)+1];
                    myKeyValues[i<<1] = null;
                    myKeyValues[(i<<1)+1] = null;
                    return (V)oldValue;
                }
            }
            prev = i & (hashLen - 1);
        }
        return (V)NOT_FOUND;
    }

    /**
     * Removes all of the mappings from this map.
     * The map will be empty after this call returns.
     */
    public void clear() {
        Arrays.fill(myKeyValues, 0, firstEmptyIndex<<1, null);
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
        System.arraycopy(myKeyValues, 0, that.myKeyValues, 0, firstEmptyIndex<<1);
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
        return i >= 0 ? (V)myKeyValues[(i<<1)+1] : null;
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
        // ToDo: resize if needed
        for (Map.Entry<? extends K, ? extends V> e : m.entrySet())
            put(e.getKey(), e.getValue());
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
                Object o = myKeyValues[(i<<1)+1];
                if (o == value || o != null && o.equals(value))
                    return true;
            }
        }
        return false;
    }

    private static final long serialVersionUID = 14L;

    /**
     * Each of these fields are initialized to contain an instance of the
     * appropriate view the first time this view is requested.  The views are
     * stateless, so there's no reason to create more than one of each.
     */
    private transient volatile Set<K> keySet = null;
    private transient volatile Collection<V> values = null;
    private transient volatile Set<Map.Entry<K,V>> entrySet = null;

    /**
     * Returns a {@link Set} view of the keys contained in this map.
     * The set is backed by the map, so changes to the map are
     * reflected in the set, and vice-versa.  If the map is modified
     * while an iteration over the set is in progress (except through
     * the iterator's own <tt>remove</tt> operation), the results of
     * the iteration are undefined.  The set supports element removal,
     * which removes the corresponding mapping from the map, via the
     * <tt>Iterator.remove</tt>, <tt>Set.remove</tt>,
     * <tt>removeAll</tt>, <tt>retainAll</tt>, and <tt>clear</tt>
     * operations.  It does not support the <tt>add</tt> or <tt>addAll</tt>
     * operations.
     */
    public Set<K> keySet() {
        Set<K> ks = keySet;
        return (ks != null ? ks : (keySet = new KeySet()));
    }

    private abstract class HashIterator<E> implements Iterator<E> {
        protected int i = 0;
        protected Object lastKey = NOT_FOUND;
        protected Object lastValue = NOT_FOUND;
        public final boolean hasNext() {
            while (i < firstEmptyIndex && isEmpty(i)) i++;
            return i < firstEmptyIndex;
        }
        public final E next() {
            while (i < firstEmptyIndex && isEmpty(i)) i++;
            if (i < firstEmptyIndex) {
                lastKey = myKeyValues[i<<1];
                lastValue = myKeyValues[(i<<1)+1];
                i++;
                return value();
            }
            else throw new NoSuchElementException();
        }
        public final void remove() {
            if (lastKey == NOT_FOUND)
                throw new IllegalStateException();
            else {
                removeKey(lastKey);
                lastKey = NOT_FOUND;
                lastValue = NOT_FOUND;
            }
        }
        protected abstract E value();
    }

    private final class KeyIterator extends HashIterator<K> {
        protected K value() {
            return (K)lastKey;
        }
    }

    private final class KeySet extends AbstractSet<K> {
        public Iterator<K> iterator() {
            return new KeyIterator();
        }
        public int size() {
            return counter;
        }
        public boolean isEmpty() {
            return counter == 0;
        }
        public boolean contains(Object o) {
            return containsKey(o);
        }
        public boolean remove(Object o) {
            return FastHashMap.this.removeKey(o) != NOT_FOUND;
        }
        public void clear() {
          FastHashMap.this.clear();
        }
    }

    /**
     * Returns a {@link Set} view of the mappings contained in this map.
     * The set is backed by the map, so changes to the map are
     * reflected in the set, and vice-versa.  If the map is modified
     * while an iteration over the set is in progress (except through
     * the iterator's own <tt>remove</tt> operation, or through the
     * <tt>setValue</tt> operation on a map entry returned by the
     * iterator) the results of the iteration are undefined.  The set
     * supports element removal, which removes the corresponding
     * mapping from the map, via the <tt>Iterator.remove</tt>,
     * <tt>Set.remove</tt>, <tt>removeAll</tt>, <tt>retainAll</tt> and
     * <tt>clear</tt> operations.  It does not support the
     * <tt>add</tt> or <tt>addAll</tt> operations.
     *
     * @return a set view of the mappings contained in this map
     */
    public Set<Map.Entry<K,V>> entrySet() {
        Set<Map.Entry<K,V>> es = entrySet;
        return es != null ? es : (entrySet = new EntrySet());
    }

    private final class EntryIterator extends HashIterator<Map.Entry<K,V>> {
        protected Map.Entry<K,V> value() {
            return new AbstractMap.SimpleEntry((K)lastKey, (V)lastValue);
        }
    }

    private final class EntrySet extends AbstractSet<Map.Entry<K,V>> {
        public Iterator<Map.Entry<K,V>> iterator() {
            return new EntryIterator();
        }
        public boolean contains(Object o) {
            if (!(o instanceof Map.Entry))
                return false;
            Map.Entry<K,V> e = (Map.Entry<K,V>) o;
            int i = positionOf(e.getKey());
            if (i < 0) return false;
            Object v1 = myKeyValues[(i<<1)+1];
            Object v2 = e.getValue();
            return v1 == v2 || v1 != null && v1.equals(v2);
        }
        public boolean remove(Object o) {
            if (!contains(o)) return false;
            removeKey(((Map.Entry<K,V>)o).getKey());
            return true;
        }
        public int size() {
            return counter;
        }
        public boolean isEmpty() {
            return counter == 0;
        }
        public void clear() {
            FastHashMap.this.clear();
        }
    }

    /**
     * Returns a {@link Collection} view of the values contained in this map.
     * The collection is backed by the map, so changes to the map are
     * reflected in the collection, and vice-versa.  If the map is
     * modified while an iteration over the collection is in progress
     * (except through the iterator's own <tt>remove</tt> operation),
     * the results of the iteration are undefined.  The collection
     * supports element removal, which removes the corresponding
     * mapping from the map, via the <tt>Iterator.remove</tt>,
     * <tt>Collection.remove</tt>, <tt>removeAll</tt>,
     * <tt>retainAll</tt> and <tt>clear</tt> operations.  It does not
     * support the <tt>add</tt> or <tt>addAll</tt> operations.
     */
    public Collection<V> values() {
        Collection<V> vs = values;
        return (vs != null ? vs : (values = new Values()));
    }

    private final class ValueIterator extends HashIterator<V> {
        protected V value() {
            return (V)lastValue;
        }
    }

    private final class Values extends AbstractCollection<V> {
        public Iterator<V> iterator() {
            return new ValueIterator();
        }
        public int size() {
            return counter;
        }
        public boolean isEmpty() {
            return counter == 0;
        }
        public boolean contains(Object o) {
            return containsValue(o);
        }
        public void clear() {
            FastHashMap.this.clear();
        }
    }

    // ToDo: readObject
    // ToDo: writeObject
}
