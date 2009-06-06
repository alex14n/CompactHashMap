import java.util.*;
import java.io.*;

/**
 * Hash table based implementation of the <tt>Map</tt> interface.  This
 * implementation provides all of the optional map operations, and permits
 * <tt>null</tt> values and the <tt>null</tt> key.  (The <tt>HashMap</tt>
 * class is roughly equivalent to <tt>Hashtable</tt>, except that it is
 * unsynchronized and permits nulls.)  This class makes no guarantees as to
 * the order of the map; in particular, it does not guarantee that the order
 * will remain constant over time.
 *
 * <p>This implementation provides constant-time performance for the basic
 * operations (<tt>get</tt> and <tt>put</tt>), assuming the hash function
 * disperses the elements properly among the buckets.  Iteration over
 * collection views requires time proportional to the "capacity" of the
 * <tt>HashMap</tt> instance (the number of buckets) plus its size (the number
 * of key-value mappings).  Thus, it's very important not to set the initial
 * capacity too high (or the load factor too low) if iteration performance is
 * important.
 *
 * <p>An instance of <tt>HashMap</tt> has two parameters that affect its
 * performance: <i>initial capacity</i> and <i>load factor</i>.  The
 * <i>capacity</i> is the number of buckets in the hash table, and the initial
 * capacity is simply the capacity at the time the hash table is created.  The
 * <i>load factor</i> is a measure of how full the hash table is allowed to
 * get before its capacity is automatically increased.  When the number of
 * entries in the hash table exceeds the product of the load factor and the
 * current capacity, the hash table is <i>rehashed</i> (that is, internal data
 * structures are rebuilt) so that the hash table has approximately twice the
 * number of buckets.
 *
 * <p>As a general rule, the default load factor (.75) offers a good tradeoff
 * between time and space costs.  Higher values decrease the space overhead
 * but increase the lookup cost (reflected in most of the operations of the
 * <tt>HashMap</tt> class, including <tt>get</tt> and <tt>put</tt>).  The
 * expected number of entries in the map and its load factor should be taken
 * into account when setting its initial capacity, so as to minimize the
 * number of rehash operations.  If the initial capacity is greater
 * than the maximum number of entries divided by the load factor, no
 * rehash operations will ever occur.
 *
 * <p>If many mappings are to be stored in a <tt>HashMap</tt> instance,
 * creating it with a sufficiently large capacity will allow the mappings to
 * be stored more efficiently than letting it perform automatic rehashing as
 * needed to grow the table.
 *
 * <p><strong>Note that this implementation is not synchronized.</strong>
 * If multiple threads access a hash map concurrently, and at least one of
 * the threads modifies the map structurally, it <i>must</i> be
 * synchronized externally.  (A structural modification is any operation
 * that adds or deletes one or more mappings; merely changing the value
 * associated with a key that an instance already contains is not a
 * structural modification.)  This is typically accomplished by
 * synchronizing on some object that naturally encapsulates the map.
 *
 * If no such object exists, the map should be "wrapped" using the
 * {@link Collections#synchronizedMap Collections.synchronizedMap}
 * method.  This is best done at creation time, to prevent accidental
 * unsynchronized access to the map:<pre>
 *   Map m = Collections.synchronizedMap(new HashMap(...));</pre>
 *
 * <p>The iterators returned by all of this class's "collection view methods"
 * are <i>fail-fast</i>: if the map is structurally modified at any time after
 * the iterator is created, in any way except through the iterator's own
 * <tt>remove</tt> method, the iterator will throw a
 * {@link ConcurrentModificationException}.  Thus, in the face of concurrent
 * modification, the iterator fails quickly and cleanly, rather than risking
 * arbitrary, non-deterministic behavior at an undetermined time in the
 * future.
 *
 * <p>Note that the fail-fast behavior of an iterator cannot be guaranteed
 * as it is, generally speaking, impossible to make any hard guarantees in the
 * presence of unsynchronized concurrent modification.  Fail-fast iterators
 * throw <tt>ConcurrentModificationException</tt> on a best-effort basis.
 * Therefore, it would be wrong to write a program that depended on this
 * exception for its correctness: <i>the fail-fast behavior of iterators
 * should be used only to detect bugs.</i>
 *
 * <p>This class is a member of the
 * <a href="{@docRoot}/../technotes/guides/collections/index.html">
 * Java Collections Framework</a>.
 *
 * @param <K> the type of keys maintained by this map
 * @param <V> the type of mapped values
 *
 * @author  Doug Lea
 * @author  Josh Bloch
 * @author  Arthur van Hoff
 * @author  Neal Gafter
 * @author  Alex Yakovlev
 * @see     Object#hashCode()
 * @see     Collection
 * @see     Map
 * @see     TreeMap
 * @see     Hashtable
 * @since   1.2
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
    final static int hash(int h) {
        // This function ensures that hashCodes that differ only by
        // constant multiples at each bit position have a bounded
        // number of collisions (approximately 8 at default load factor).
        return (h >>> 20) ^ (h >>> 12) ^ (h >>> 7) ^ (h >>> 4) ^ h;
    }

    /**
     * Return improved hash for object o.
     */
    final static int hash(Object o) {
        int h = o == null ? 0 : o.hashCode();
        return (h >>> 20) ^ (h >>> 12) ^ (h >>> 7) ^ (h >>> 4) ^ h;
    }

    /**
     * The number of key-value mappings contained in this map.
     */
    transient int size = 0;

    /**
     * Arrays with stored keys and values.
     * Storing them in one array in neighbour cells
     * is faster since it's reading adjacent memory addresses.
     */
    transient Object[] myKeyValues;

    /**
     * 1 if myKeyValues contains keys and values,
     * 0 if only keys (to save memory in HashSet).
     */
    transient int keyShift;

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
    transient int firstEmptyIndex = 0;

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
    int threshold;

    /**
     * The load factor for the hash table.
     * @serial
     */
    final float loadFactor;

    /**
     * The number of times this HashMap has been structurally modified
     * Structural modifications are those that change the number of mappings in
     * the HashMap or otherwise modify its internal structure (e.g.,
     * rehash).  This field is used to make iterators on Collection-views of
     * the HashMap fail-fast.  (See ConcurrentModificationException).
     */
    transient int modCount;

    /**
     * Constructs an empty <tt>HashMap</tt> with the default initial capacity
     * (16) and the default load factor (0.75).
     */
    public FastHashMap() {
       this(true);
    }

    /**
     * Constructor to be used in HashSet
     * containing only keys without values
     * thus saving some memory if withValues is false.
     */
    FastHashMap(boolean withValues) {
        loadFactor = DEFAULT_LOAD_FACTOR;
        hashLen = DEFAULT_INITIAL_CAPACITY;
        threshold = (int)(hashLen * loadFactor);
        keyShift = withValues ? 1 : 0;
        myKeyValues = new Object[threshold<<keyShift];
        myIndices = new int[hashLen+threshold];
        init();
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

    /**
     * Constructor to be used in HashSet
     * containing only keys without values
     * thus saving some memory if withValues is false.
     */
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
        init();
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

    /**
     * Constructor to be used in HashSet
     * containing only keys without values
     * thus saving some memory if withValues is false.
     */
    FastHashMap(int initialCapacity, boolean withValues) {
        this(initialCapacity, DEFAULT_LOAD_FACTOR, withValues);
    }

    /**
     * Constructs a new <tt>HashMap</tt> with the same mappings as the
     * specified <tt>Map</tt>.  The <tt>HashMap</tt> is created with
     * default load factor (0.75) and an initial capacity sufficient to
     * hold the mappings in the specified <tt>Map</tt>.
     *
     * @param   m the map whose mappings are to be placed in this map
     * @throws  NullPointerException if the specified map is null
     */
    public FastHashMap(Map<? extends K, ? extends V> m) {
        this(Math.max((int) (m.size() / DEFAULT_LOAD_FACTOR) + 1,
                      DEFAULT_INITIAL_CAPACITY), DEFAULT_LOAD_FACTOR);
        for (Map.Entry<? extends K, ? extends V> e : m.entrySet())
            put(e.getKey(), e.getValue(), false);
    }

    /**
     * Initialization hook for subclasses. This method is called
     * in all constructors and pseudo-constructors (clone, readObject)
     * after HashMap has been initialized but before any entries have
     * been inserted.  (In the absence of this method, readObject would
     * require explicit knowledge of subclasses.)
     */
    void init() {
    }

    /**
     * Increase size of internal arrays two times.
     */
    void resize() {
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
    final int positionOf(Object key) {
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
        return put(key, value, true);
    }

    /**
     * Create a new key/value mapping without looking
     * if such key is already present in this map
     * if searchForExistingKey is false.
     */
    @SuppressWarnings("unchecked")
    final V put(K key, V value, boolean searchForExistingKey) {
        int hc = hash(key);
        int i = hc & (hashLen - 1);
        int next = myIndices[i];
        int mask = AVAILABLE_BITS ^ (hashLen-1);
        int hcBits = hc & mask;
        // Look if key is already in this map
        if(searchForExistingKey) {
            int k;
            for (int j = ~next; j >= 0; j = ~myIndices[hashLen+k]) {
                k = j & (hashLen - 1);
                if (hcBits == (j & mask)) {
                    Object o = myKeyValues[k<<keyShift];
                    if (o == key || o != null && o.equals(key)) {
                        Object oldValue = keyShift > 0 ? myKeyValues[(k<<keyShift)+1] : DUMMY_VALUE;
                        if (keyShift > 0) myKeyValues[(k<<keyShift)+1] = value;
                        updateHook(k);
                        return (V)oldValue;
                    }
                }
                if ((j & END_OF_LIST) != 0) break;
            }
        }
        // Resize if needed
        if (size >= threshold) {
            resize();
            i = hc & (hashLen - 1);
            mask = AVAILABLE_BITS ^ (hashLen-1);
            hcBits = hc & mask;
            next = myIndices[i];
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
            modCount++;
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
        modCount++;
        addHook(newIndex);
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
    @SuppressWarnings("unchecked")
    final V removeKey(Object key) {
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
                    modCount++;
                    removeHook(j);
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
        modCount++;
    }

    /**
     * Returns a shallow copy of this <tt>HashMap</tt> instance:
     * the keys and values themselves are not cloned.
     *
     * @return a shallow copy of this map
     */
    @SuppressWarnings("unchecked")
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
        that.modCount = 0;
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
    @SuppressWarnings("unchecked")
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

    /**
     * This method defines this map iteration order.
     * This order can be changed in subclasses like LinkedHashMap.
     *
     * @return  index of the first element.
     */
    int iterateFirst() {
        if (size == 0) return -1;
        int i = 0;
        while(isEmpty(i)) i++;
        return i;
    }

    /**
     * This method defines this map iteration order.
     * This order can be changed in subclasses like LinkedHashMap.
     *
     * @param  i  index if the current element.
     * @return  index of the next element.
     */
    int iterateNext(int i) {
        do i++; while (i < firstEmptyIndex && isEmpty(i));
        return i < firstEmptyIndex ? i : -1;
    }

    /**
     * Generic iterator over this map.
     * value() method should return the real elements.
     */
    private abstract class HashIterator<E> implements Iterator<E> {
        int nextIndex = iterateFirst();
        int lastIndex = -1;
        int expectedModCount = modCount; // For fast-fail
        public final boolean hasNext() {
            return nextIndex >= 0;
        }
        public final E next() {
            if (modCount != expectedModCount)
                throw new ConcurrentModificationException();
            if (nextIndex < 0)
                throw new NoSuchElementException();
            lastIndex = nextIndex;
            nextIndex = iterateNext(nextIndex);
            return value();
        }
        public final void remove() {
            if (lastIndex < 0)
                throw new IllegalStateException();
            if (modCount != expectedModCount)
                throw new ConcurrentModificationException();
            removeKey(myKeyValues[lastIndex<<keyShift]);
            lastIndex = -1;
            expectedModCount = modCount;
        }
        abstract E value();
    }

    private final class KeyIterator extends HashIterator<K> {
        @SuppressWarnings("unchecked")
        K value() {
            return (K)myKeyValues[lastIndex<<keyShift];
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
        Map.Entry<K,V> value() {
            return new Entry(lastIndex);
        }
    }

    private final class EntrySet extends AbstractSet<Map.Entry<K,V>> {
        public Iterator<Map.Entry<K,V>> iterator() {
            return new EntryIterator();
        }
        public boolean contains(Object o) {
            if (!(o instanceof Map.Entry))
                return false;
            @SuppressWarnings("unchecked")
            Map.Entry<K,V> e = (Map.Entry<K,V>) o;
            int i = positionOf(e.getKey());
            if (i < 0) return false;
            Object v1 = keyShift > 0 ? myKeyValues[(i<<keyShift)+1] : DUMMY_VALUE;
            Object v2 = e.getValue();
            return v1 == v2 || v1 != null && v1.equals(v2);
        }
        @SuppressWarnings("unchecked")
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
        @SuppressWarnings("unchecked")
        V value() {
            return (V)(keyShift > 0 ? myKeyValues[(lastIndex<<keyShift)+1] : DUMMY_VALUE);
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
     * Save the state of the <tt>HashMap</tt> instance
     * to a stream (i.e., serialize it).
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
        for (int i = iterateFirst(); i >= 0; i = iterateNext(i)) {
            s.writeObject(myKeyValues[i<<keyShift]);
            s.writeObject(keyShift > 0 ? myKeyValues[(i<<keyShift)+1] : null);
        }
    }

    private static final long serialVersionUID = 362498820763181265L;

    /**
     * Reconstitute the <tt>HashMap</tt> instance
     * from a stream (i.e., deserialize it).
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

        init();  // Give subclass a chance to do its thing.

        // Read in size (number of Mappings)
        int size = s.readInt();

        // Read the keys and values, and put the mappings in the HashMap
        for (int i=0; i<size; i++) {
            @SuppressWarnings("unchecked")
            K key = (K) s.readObject();
            @SuppressWarnings("unchecked")
            V value = (V) s.readObject();
            put(key, value, false);
        }
    }

    // ToDo: check modCount and throw IllegalStateException?
    @SuppressWarnings("unchecked")
    final class Entry implements Map.Entry<K,V> {
        private final int index;
        Entry(int index) {
            this.index = index;
        }
        public final K getKey() {
            return (K)myKeyValues[index<<keyShift];
        }
        public final V getValue() {
            return (V)(keyShift > 0 ? myKeyValues[(index<<keyShift)+1] : DUMMY_VALUE);
        }
        public final V setValue(V newValue) {
            if(keyShift == 0) throw new UnsupportedOperationException();
            V oldValue = (V)myKeyValues[(index<<keyShift)+1];
            myKeyValues[(index<<keyShift)+1] = newValue;
            updateHook(index);
            return oldValue;
        }
        public boolean equals(Object o) {
            if (!(o instanceof Map.Entry))
                return false;
            Map.Entry<K,V> that = (Map.Entry<K,V>)o;
            K key1 = this.getKey();
            K key2 = that.getKey();
            if(key1 == key2 || (key1 != null && key1.equals(key2))) {
                V value1 = this.getValue();
                V value2 = that.getValue();
                return value1 == value2 || (value1 != null && value1.equals(value2));
            }
            return false;
        }
        public int hashCode() {
            K key = getKey();
            V value = getValue();
            return (key   == null ? 0 :   key.hashCode()) ^
                   (value == null ? 0 : value.hashCode());
        }
        public String toString() {
            return getKey() + "=" + getValue();
        }
    }

    // These methods are used when serializing HashSets
    int   capacity()     { return hashLen; }
    float loadFactor()   { return loadFactor; }

    // These hooks are needed for LinkedHashMap
    void addHook(int i) { }
    void updateHook(int i) { }
    void removeHook(int i) { }
}
