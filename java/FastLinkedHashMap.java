import java.util.*;

public class FastLinkedHashMap<K,V>
    extends FastHashMap<K,V>
{
    private static final long serialVersionUID = 3801124242820219131L;
    private final boolean accessOrder;
    transient private int[] beforeAfter;
    private transient int headIndex;

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
        // Arrays.fill(beforeAfter, 0, firstEmptyIndex<<1, 0);
        super.clear();
        headIndex = -1;
    }

    protected void resize() {
        super.resize();
        beforeAfter = Arrays.copyOf(beforeAfter, threshold<<1);
    }

    public FastLinkedHashMap<K,V> clone() {
        FastLinkedHashMap<K,V> that = (FastLinkedHashMap<K,V>)super.clone();
        that.beforeAfter = beforeAfter.clone();
        return that;
    }

    protected boolean removeEldestEntry(Map.Entry<K,V> eldest) {
        return false;
    }

    protected void init() {
        // super.init();
        beforeAfter = new int[threshold<<1];
        headIndex = -1;
    }

    protected void beforeAdditionHook() {
        if(headIndex >= 0) {
            K key = (K)myKeyValues[headIndex<<keyShift];
            V value = (V)(keyShift > 0 ? myKeyValues[(headIndex<<keyShift)+1] : DUMMY_VALUE);
            Map.Entry<K,V> eldest = new AbstractMap.SimpleEntry<K,V>(key, value); // ToDo: cache it?
            if (removeEldestEntry(eldest))
                removeKey(key);
        }
    }

    protected void afterAdditionHook(int i) {
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

    protected void removeHook(int i) {
        if (size == 0) {
            headIndex = -1;
        } else {
            int prev = beforeAfter[i<<1];
            int next = beforeAfter[(i<<1)+1];
            beforeAfter[next<<1] = prev;
            beforeAfter[(prev<<1)+1] = next;
            if(headIndex == i) headIndex = next;
        }
    }

    protected void accessHook(int i) {
        if(accessOrder) {
            removeHook(i);
            afterAdditionHook(i);
        }
    }

    protected int iterateFirst() {
        return headIndex;
    }

    protected int iterateNext(int i) {
        i = beforeAfter[(i<<1)+1];
        return i == headIndex ? -1 : i;
    }
}
