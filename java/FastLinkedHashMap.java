import java.util.*;

public class FastLinkedHashMap<K,V>
    extends FastHashMap<K,V>
{
    private static final long serialVersionUID = 3801124242820219131L;
    private final boolean accessOrder;

    /**
     * Index array:
     * it's even elements are 'before' indices
     * (index of previous element in linked list),
     * odd elements are 'after' (next element index).
     */
    private transient int[] beforeAfter;

    /**
     * Index of the eldest map element, head of the linked list.
     */
    private transient int headIndex;

    /**
     * Cached Entry of the eldest map element
     * for removeEldestEntry speedup.
     */
    private transient Map.Entry<K,V> headEntry;

    public FastLinkedHashMap(int initialCapacity, float loadFactor) {
        super(initialCapacity, loadFactor);
        accessOrder = false;
   }

    public FastLinkedHashMap(int initialCapacity) {
        super(initialCapacity);
        accessOrder = false;
    }

    public FastLinkedHashMap() {
        super();
        accessOrder = false;
    }

    public FastLinkedHashMap(Map<? extends K, ? extends V> m) {
        super(m);
        accessOrder = false;
    }

    public FastLinkedHashMap(int initialCapacity,
        float loadFactor,
        boolean accessOrder) {
        super(initialCapacity, loadFactor);
        this.accessOrder = accessOrder;
    }

    public void clear() {
        super.clear();
        headIndex = -1;
        headEntry = null;
    }

    protected void resize() {
        super.resize();
        beforeAfter = Arrays.copyOf(beforeAfter, threshold<<1);
    }

    public FastLinkedHashMap<K,V> clone() {
        FastLinkedHashMap<K,V> that = (FastLinkedHashMap<K,V>)super.clone();
        that.beforeAfter = beforeAfter.clone();
        that.headEntry = null;
        return that;
    }

    protected boolean removeEldestEntry(Map.Entry<K,V> eldest) {
        return false;
    }

    protected void init() {
        beforeAfter = new int[threshold<<1];
        headIndex = -1;
        headEntry = null;
    }

    /**
     * This method is called before addition of new key/value pair.
     *
     * Here we check removeEldestEntry and remove map eldest pair
     * if it returns true. Doing this check after addition can
     * cause unnecessary resize.
     */
    protected void beforeAdditionHook() {
        if(headIndex < 0) return;
        if(headEntry == null) {
            K key = (K)myKeyValues[headIndex<<keyShift];
            V value = (V)(keyShift > 0 ? myKeyValues[(headIndex<<keyShift)+1] : DUMMY_VALUE);
            headEntry = new AbstractMap.SimpleEntry<K,V>(key, value);
        }
        if(removeEldestEntry(headEntry)) {
            removeKey(headEntry.getKey());
        }
    }

    /**
     * This method is called after addition of new key/value pair.
     *
     * Here we insert its index to the very end of the linked list.
     */
    protected void afterAdditionHook(int i) {
        insertIndex(i);
    }

    /**
     * This method is called when key/value pair is removed from the map.
     *
     * Here we remove its index from the linked list.
     */
    protected void removeHook(int i) {
        removeIndex(i);
    }

    /**
     * This method is called when existing key's value is modified.
     *
     * Here we move its index to the end of linked list
     * if accessOrder is true and update cached HeadEntry.
     */
    protected void updateHook(int i) {
        updateIndex(i);
        if(i == headIndex && headEntry != null) {
            V value = (V)(keyShift > 0 ? myKeyValues[(headIndex<<keyShift)+1] : DUMMY_VALUE);
            headEntry.setValue(value);
        }
    }

    // Iteration order based on the linked list.

    protected int iterateFirst() {
        return headIndex;
    }

    protected int iterateNext(int i) {
        i = beforeAfter[(i<<1)+1];
        return i == headIndex ? -1 : i;
    }

    //

    public V get(Object key) {
        int i = positionOf(key);
        if(i < 0) return null;
        updateIndex(i);
        return (V)(keyShift > 0 ? myKeyValues[(i<<keyShift)+1] : DUMMY_VALUE);
    }

    //

    /**
     * Remove index from the linked list.
     */
    private final void removeIndex(int i) {
        if (size == 0) {
            headIndex = -1;
            headEntry = null;
        } else {
            int prev = beforeAfter[i<<1];
            int next = beforeAfter[(i<<1)+1];
            beforeAfter[next<<1] = prev;
            beforeAfter[(prev<<1)+1] = next;
            if(headIndex == i) {
                headIndex = next;
                headEntry = null;
            }
        }
    }

    /**
     * Add index to the linked list.
     *
     * @param  i  index
     */
    private final void insertIndex(int i) {
        if (headIndex < 0) {
            beforeAfter[i<<1] =
            beforeAfter[(i<<1)+1] =
            headIndex = i;
        } else {
            int last = beforeAfter[headIndex<<1];
            beforeAfter[i<<1] = last;
            beforeAfter[(i<<1)+1] = headIndex;
            beforeAfter[headIndex<<1] = i;
            beforeAfter[(last<<1)+1] = i;
        }
        modCount++;
    }

    /**
     * Move specified index to the end of the list
     * if accessOrder is true.
     *
     * @param  i  index
     */
    private final void updateIndex(int i) {
        if(accessOrder) {
            removeIndex(i);
            insertIndex(i);
        }
    }
}
