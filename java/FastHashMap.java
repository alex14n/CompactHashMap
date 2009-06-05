import java.util.*;
import java.io.*;

/**
 * Fast HashMap implementation.
 *
 * Some benchmark results:
 * http://forums.sun.com/thread.jspa?threadID=5387181
 *
 * @author  Alex Yakovlev
 */
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
     * Maximum allowed load factor, since element index bits
     * cannot exceed number of hash bits (other bits are used to store hashcode).
     */
    static final float MAXIMUM_LOAD_FACTOR = 1f;

    /**
     * Bits available to store indices and hashcode bits.
     * Now the highest (31st) bit (negative/inverted values) is used as deleted flag,
     * 30th bit is used to mark end of list, thus 30 bits are available.
     */
    private final static int AVAILABLE_BITS = 0x3FFFFFFF;

    /**
     * Bit flag marking the end of list of elements with the same hashcode,
     * or end of deleted list.
     */
    private final static int END_OF_LIST = 0x40000000;

    /**
     * The maximum capacity, used if a higher value is implicitly specified
     * by either of the constructors with arguments.
     * MUST be a power of two <= 1<<30.
     */
    static final int MAXIMUM_CAPACITY = 1 << 30;

    /**
     * Applies a supplemental hash function to a given object's hashCode,
     * which defends against poor quality hash functions. This is critical
     * because HashMap uses power-of-two length hash tables, that
     * otherwise encounter collisions for hashCodes that do not differ
     * in lower bits. Note: Null keys always map to hash 0, thus index 0.
     */
    final static int hash(Object o) {
        // This function ensures that hashCodes that differ only by
        // constant multiples at each bit position have a bounded
        // number of collisions (approximately 8 at default load factor).
        int h = o == null ? 0 : o.hashCode();
        return (h >>> 20) ^ (h >>> 12) ^ (h >>> 7) ^ (h >>> 4) ^ h;
    }

    /**
     * The number of key-value mappings contained in this map.
     */
    transient private int size = 0;

    /**
     * Arrays with stored keys and values.
     * Storing them in one array in neighbour cells
     * is faster since it's reading adjacent memory addresses.
     */
    transient private Object[] myKeyValues;

    /**
     * 1 if myKeyValues contains keys and values,
     * 0 if only keys (to save memory in HashSet).
     */
    transient private int keyShift;

    /**
     * Value to use if keyShift is 0.
     */
    final static Object DUMMY_VALUE = new Object();

    /**
     * Array of complex indices.
     *
     * First <tt>hashLen</tt> are hashcode-to-array maps,
     * next <tt>threshold</tt> maps to next element with the same hashcode.
     * Highest index bit (negative/inverted values) is used as deleted flag,
     * 30th bit is used to mark last element in list,
     * lowest bits are real index in array,
     * and in the middle hashcode bits is stored.
     *
     * Because of new arrays are initialised with zeroes,
     * and we want to minimise number of memory writes,
     * we leave 0 as value of 'unoccupied' entry,
     * and invert real indices values.
     * We also need to store deleted entries list,
     * and to easily check if entry is occupied or not during iteration
     * deleted indices are not inverted and stored as positive,
     * but to separate them from default zero value we add 1 to them.
     */
    transient private int[] myIndices;

    /**
     * Index of the first not occupied position in array.
     * All elements starting with this index are free.
     */
    transient private int firstEmptyIndex = 0;

    /**
     * Index of first element in deleted list,
     * or -1 if no elements are deleted.
     */
    transient private int firstDeletedIndex = -1;

    /**
     * Number of hash baskets, power of 2.
     */
    transient private int hashLen;

    /**
     * The next size value at which to resize (capacity * load factor).
     * @serial
     */
    private int threshold;

    /**
     * The load factor for the hash table.
     * @serial
     */
    final float loadFactor;

    /**
     * Constructs an empty <tt>HashMap</tt> with the default initial capacity
     * (16) and the default load factor (0.75).
     */
    public FastHashMap() {
       this(true);
    }
    FastHashMap(boolean withValues) {
        loadFactor = DEFAULT_LOAD_FACTOR;
        hashLen = DEFAULT_INITIAL_CAPACITY;
        threshold = (int)(hashLen * loadFactor);
        keyShift = withValues ? 1 : 0;
        myKeyValues = new Object[threshold<<keyShift];
        myIndices = new int[hashLen+threshold];
    }

    /**
     * Constructs an empty <tt>HashMap</tt> with the specified initial
     * capacity and load factor.
     *
     * @param  initialCapacity the initial capacity
     * @param  loadFactor      the load factor
     * @throws IllegalArgumentException if the initial capacity is negative
     *         or the load factor is greater than one or is too low
     */
    public FastHashMap(int initialCapacity, float loadFactor) {
        this(initialCapacity, loadFactor, true);
    }
    FastHashMap(int initialCapacity, float loadFactor, boolean withValues) {
        if (initialCapacity < 0)
            throw new IllegalArgumentException(
                "Illegal initial capacity: " + initialCapacity);
        if (initialCapacity > MAXIMUM_CAPACITY)
            initialCapacity = MAXIMUM_CAPACITY;
        if (Float.isNaN(loadFactor))
            throw new IllegalArgumentException(
                "Illegal load factor: " + loadFactor);
        this.loadFactor = loadFactor > MAXIMUM_LOAD_FACTOR ? MAXIMUM_LOAD_FACTOR : loadFactor;
        // Find a power of 2 >= initialCapacity
        for (hashLen = DEFAULT_INITIAL_CAPACITY; hashLen < initialCapacity; hashLen <<= 1);
        threshold = (int)(hashLen * loadFactor);
        if (threshold < 1)
            throw new IllegalArgumentException(
                "Illegal load factor: " + loadFactor);
        keyShift = withValues ? 1 : 0;
        myKeyValues = new Object[threshold<<keyShift];
        myIndices = new int[hashLen+threshold];
    }

    /**
     * Constructs an empty <tt>HashMap</tt> with the specified initial
     * capacity and the default load factor (0.75).
     *
     * @param  initialCapacity the initial capacity.
     * @throws IllegalArgumentException if the initial capacity is negative.
     */
    public FastHashMap(int initialCapacity) {
        this(initialCapacity, DEFAULT_LOAD_FACTOR, true);
    }
    FastHashMap(int initialCapacity, boolean withValues) {
        this(initialCapacity, DEFAULT_LOAD_FACTOR, withValues);
    }

    /**
     * Increase size of internal arrays two times.
     */
    final private void resize() {
        int newHashLen = hashLen << 1;
        int newValueLen = (int)(newHashLen * loadFactor);
        Object[] newKeyValues = Arrays.copyOf(myKeyValues,newValueLen<<keyShift);
        int[] newIndices = new int[newHashLen+newValueLen];
        int mask = AVAILABLE_BITS ^ (hashLen-1);
        int newMask = AVAILABLE_BITS ^ (newHashLen-1);
        for (int i = 0; i < hashLen; i++) {
            int next1 = 0;
            int next2 = 0;
            int arrayIndex;
            for (int j = ~myIndices[i]; j >= 0; j = ~myIndices[hashLen + arrayIndex]) {
                arrayIndex = j & (hashLen-1);
                int newHashIndex = i | (j & (newMask ^ mask));
                // Each old element from the old hash basket may go
                // either to the same basket in increased hash table
                // (if new highest bit is zero)
                // or to (i + hashLen) if new highest bit is 1.
                if (newHashIndex == i) {
                    if (next1 < 0) newIndices[newHashLen + arrayIndex] = next1;
                    next1 = ~(arrayIndex | (j & newMask) | (next1 < 0 ? 0 : END_OF_LIST));
                } else {
                    if (next2 < 0) newIndices[newHashLen + arrayIndex] = next2;
                    next2 = ~(arrayIndex | (j & newMask) | (next2 < 0 ? 0 : END_OF_LIST));
                }
                if ((j & END_OF_LIST) != 0) break;
            }
            if (next1 < 0) newIndices[i] = next1;
            if (next2 < 0) newIndices[i + hashLen] = next2;
        }
        hashLen = newHashLen;
        threshold = newValueLen;
        myKeyValues = newKeyValues;
        myIndices = newIndices;
    }

    /**
     * Returns the index of key in internal arrays if it is present.
     *
     * @param key key
     * @return index of key in array or -1 if it was not found
     */
    final private int positionOf(Object key) {
        int hc = hash(key);
        int mask = AVAILABLE_BITS ^ (hashLen-1);
        int hcBits = hc & mask;
        int curr = hc & (hashLen-1);
        for (int i = ~myIndices[curr]; i >= 0; i = ~myIndices[curr]) {
            curr = i & (hashLen-1);
            // Check if stored hashcode bits are equal
            // to hashcode of the key we are looking for
            if (hcBits == (i & mask)) {
                Object x = myKeyValues[curr<<keyShift];
                if (x == key || x != null && x.equals(key))
                    return curr;
            }
            if ((i & END_OF_LIST) != 0) return -1;
            curr += hashLen;
        }
        return -1;
    }

    /**
     * Returns <tt>true</tt> if i-th array position
     * is not occupied (is in deleted elements list).
     *
     * @param i index in array, must be less than firstEmptyIndex
     * @return <tt>true</tt> if i-th is empty (was deleted)
     */
    final private boolean isEmpty(int i) {
        return /* i >= firstEmptyIndex || */ i == firstDeletedIndex || myIndices[hashLen+i] > 0;
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
        int mask = AVAILABLE_BITS ^ (hashLen-1);
        int hcBits = hc & mask;
        // Look if key is already in this map
        int k;
        for (int j = ~next; j >= 0; j = ~myIndices[hashLen+k]) {
            k = j & (hashLen - 1);
            if (hcBits == (j & mask)) {
                Object o = myKeyValues[k<<keyShift];
                if (o == key || o != null && o.equals(key)) {
                    Object oldValue = keyShift > 0 ? myKeyValues[(k<<keyShift)+1] : DUMMY_VALUE;
                    if (keyShift > 0) myKeyValues[(k<<keyShift)+1] = value;
                    return (V)oldValue;
                }
            }
            if ((j & END_OF_LIST) != 0) break;
        }
        // Resize if needed
        if (size >= threshold) {
            resize();
            i = hc & (hashLen - 1);
            next = myIndices[i];
            mask = AVAILABLE_BITS ^ (hashLen-1);
            hcBits = hc & mask;
        }
        // Find a place for new element
        int newIndex;
        // First we reuse deleted positions
        if (firstDeletedIndex >= 0) {
            newIndex = firstDeletedIndex;
            int di = myIndices[hashLen+firstDeletedIndex];
            if (di == END_OF_LIST)
                firstDeletedIndex = -1;
            else
                firstDeletedIndex = di-1;
            if (next >= 0) myIndices[hashLen+newIndex] = 0;
        } else {
            newIndex = firstEmptyIndex;
            firstEmptyIndex++;
        }
        // Insert it
        myKeyValues[newIndex<<keyShift] = key;
        if (keyShift > 0) myKeyValues[(newIndex<<keyShift)+1] = value;
        if (next < 0) myIndices[hashLen + newIndex] = next;
        myIndices[i] = ~(newIndex | hcBits | (next < 0 ? 0 : END_OF_LIST));
        size++;
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

    /**
     * Value to distinguish null as 'key not found' from null as real value.
     */
    private final static Object NOT_FOUND = new Object();

    /**
     * Removes the mapping for the specified key from this map if present.
     *
     * @param key key whose mapping is to be removed from the map
     * @return NOT_FOUND or old value
     */
    final private V removeKey(Object key) {
        int hc = hash(key);
        int mask = AVAILABLE_BITS ^ (hashLen-1);
        int hcBits = hc & mask;
        int prev = -1;
        int curr = hc & (hashLen-1);
        for (int i = ~myIndices[curr]; i >= 0; i = ~myIndices[curr]) {
            int j = i & (hashLen-1);
            int k = hashLen + j;
            if (hcBits == (i & mask)) {
                Object o = myKeyValues[j<<keyShift];
                if (o == key || o != null && o.equals(key)) {
                    size--;
                    if((i & END_OF_LIST) != 0) {
                        if (prev >= 0)
                            myIndices[prev] ^= END_OF_LIST;
                        else
                            myIndices[curr] = 0;
                    } else {
                        myIndices[curr] = myIndices[k];
                    }
                    if (j == firstEmptyIndex-1) {
                        firstEmptyIndex = j;
                    } else {
                        myIndices[k] = firstDeletedIndex < 0 ? END_OF_LIST : firstDeletedIndex+1;
                        firstDeletedIndex = j;
                    }
                    Object oldValue = keyShift > 0 ? myKeyValues[(j<<keyShift)+1] : DUMMY_VALUE;
                    myKeyValues[j<<keyShift] = null;
                    if (keyShift > 0) myKeyValues[(j<<keyShift)+1] = null;
                    return (V)oldValue;
                }
            }
            if ((i & END_OF_LIST) != 0) break;
            prev = curr;
            curr = k;
        }
        return (V)NOT_FOUND;
    }

    /**
     * Removes all of the mappings from this map.
     * The map will be empty after this call returns.
     */
    public void clear() {
        Arrays.fill(myKeyValues, 0, firstEmptyIndex<<keyShift, null);
        Arrays.fill(myIndices, 0, hashLen + firstEmptyIndex, 0);
        size = 0;
        firstEmptyIndex = 0;
        firstDeletedIndex = -1;
    }

    /**
     * Returns a shallow copy of this <tt>HashMap</tt> instance: the keys and
     * values themselves are not cloned.
     *
     * @return a shallow copy of this map
     */
    public FastHashMap<K,V> clone() {
        FastHashMap<K,V> that = null;
        try {
            that = (FastHashMap<K,V>)super.clone();
        } catch (CloneNotSupportedException e) {
        }
        that.myKeyValues = myKeyValues.clone();
        that.myIndices = myIndices.clone();
        that.keySet = null;
        that.values = null;
        that.entrySet = null;
        return that;
    }

    /**
     * Returns the number of key-value mappings in this map.
     *
     * @return the number of key-value mappings in this map
     */
    public int size() {
        return size;
    }

    /**
     * Returns <tt>true</tt> if this map contains no key-value mappings.
     *
     * @return <tt>true</tt> if this map contains no key-value mappings
     */
    public boolean isEmpty() {
        return size == 0;
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
        return i < 0 ? null : (V)(keyShift > 0 ? myKeyValues[(i<<keyShift)+1] : DUMMY_VALUE);
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
        if (keyShift == 0) return size > 0 && value == DUMMY_VALUE;
        for (int i = 0; i < firstEmptyIndex ; i++)
            if (!isEmpty(i)) { // Not deleted
                Object o = myKeyValues[(i<<keyShift)+1];
                if (o == value || o != null && o.equals(value))
                    return true;
            }
        return false;
    }

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
                lastKey = myKeyValues[i<<keyShift];
                lastValue = keyShift > 0 ? myKeyValues[(i<<keyShift)+1] : DUMMY_VALUE;
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
            return size;
        }
        public boolean isEmpty() {
            return size == 0;
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
            return new AbstractMap.SimpleEntry<K,V>((K)lastKey, (V)lastValue);
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
            Object v1 = keyShift > 0 ? myKeyValues[(i<<keyShift)+1] : DUMMY_VALUE;
            Object v2 = e.getValue();
            return v1 == v2 || v1 != null && v1.equals(v2);
        }
        public boolean remove(Object o) {
            if (!contains(o)) return false;
            removeKey(((Map.Entry<K,V>)o).getKey());
            return true;
        }
        public int size() {
            return size;
        }
        public boolean isEmpty() {
            return size == 0;
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
            return size;
        }
        public boolean isEmpty() {
            return size == 0;
        }
        public boolean contains(Object o) {
            return containsValue(o);
        }
        public void clear() {
            FastHashMap.this.clear();
        }
    }

    /**
     * Save the state of the <tt>HashMap</tt> instance to a stream (i.e.,
     * serialize it).
     *
     * @serialData The <i>capacity</i> of the HashMap (the length of the
     *             bucket array) is emitted (int), followed by the
     *             <i>size</i> (an int, the number of key-value
     *             mappings), followed by the key (Object) and value (Object)
     *             for each key-value mapping.  The key-value mappings are
     *             emitted in no particular order.
     */
    private void writeObject(ObjectOutputStream s)
        throws IOException
    {
        // Write out the threshold, loadfactor, and any hidden stuff
        s.defaultWriteObject();

        // Write out number of buckets
        s.writeInt(hashLen);

        // Write out size (number of Mappings)
        s.writeInt(size);

        // Write out keys and values (alternating)
        for (int i = 0; i < firstEmptyIndex; i++) {
            if (!isEmpty(i)) {
                s.writeObject(myKeyValues[i<<keyShift]);
                s.writeObject(keyShift > 0 ? myKeyValues[(i<<keyShift)+1] : null);
            }
        }
    }

    private static final long serialVersionUID = 362498820763181265L;

    /**
     * Reconstitute the <tt>HashMap</tt> instance from a stream (i.e.,
     * deserialize it).
     */
    private void readObject(ObjectInputStream s)
         throws IOException, ClassNotFoundException
    {
        // Read in the threshold, loadfactor, and any hidden stuff
        s.defaultReadObject();

        // Read in number of buckets and allocate the bucket array;
        hashLen = s.readInt();
        keyShift = 1;
        myKeyValues = new Object[threshold<<keyShift];
        myIndices = new int[hashLen+threshold];
        firstDeletedIndex = -1;

        // Read in size (number of Mappings)
        int size = s.readInt();

        // Read the keys and values, and put the mappings in the HashMap
        for (int i=0; i<size; i++) {
            K key = (K) s.readObject();
            V value = (V) s.readObject();
            put(key, value); // ToDo: putNew
        }
    }

    // These methods are used when serializing HashSets
    int   capacity()     { return hashLen; }
    float loadFactor()   { return loadFactor;   }
}
