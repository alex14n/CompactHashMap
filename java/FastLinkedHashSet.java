import java.util.*;

public class FastLinkedHashSet<E>
    extends FastHashSet<E>
{
    private static final long serialVersionUID = -2851667679971038690L;

    public FastLinkedHashSet(int initialCapacity, float loadFactor) {
        super(initialCapacity, loadFactor, true);
    }

    public FastLinkedHashSet(int initialCapacity) {
        super(initialCapacity, FastHashMap.DEFAULT_LOAD_FACTOR, true);
    }

    public FastLinkedHashSet() {
        super(FastHashMap.DEFAULT_INITIAL_CAPACITY, FastHashMap.DEFAULT_LOAD_FACTOR, true);
    }

    public FastLinkedHashSet(Collection<? extends E> c) {
        super(
            Math.max((int)(c.size()/FastHashMap.DEFAULT_LOAD_FACTOR) + 1,
                     FastHashMap.DEFAULT_INITIAL_CAPACITY),
            FastHashMap.DEFAULT_LOAD_FACTOR, true);
        addAll(c);
    }
}
