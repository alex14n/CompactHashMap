import java.util.*;
import java.io.*;

public class FastHashSet<E>
    extends AbstractSet<E>
    implements Set<E>, Cloneable, Serializable
{
    static final long serialVersionUID = -5024744406713321676L;

    private transient FastHashMap<E,Object> map;

    public FastHashSet() {
        map = new FastHashMap<E,Object>(false);
    }

    public FastHashSet(Collection<? extends E> c) {
        map = new FastHashMap<E,Object>(
            Math.max((int)(c.size()/FastHashMap.DEFAULT_LOAD_FACTOR) + 1,
                     FastHashMap.DEFAULT_INITIAL_CAPACITY),
            false);
        addAll(c);
    }

    public FastHashSet(int initialCapacity, float loadFactor) {
        map = new FastHashMap<E,Object>(initialCapacity, loadFactor, false);
    }

    public FastHashSet(int initialCapacity) {
        map = new FastHashMap<E,Object>(initialCapacity, false);
    }

    FastHashSet(int initialCapacity, float loadFactor, boolean dummy) {
        map = new FastLinkedHashMap<E,Object>(initialCapacity, loadFactor, false, false);
    }

    public Iterator<E> iterator() {
        return map.keySet().iterator();
    }

    public int size() {
        return map.size();
    }

    public boolean isEmpty() {
        return map.isEmpty();
    }

    public boolean contains(Object o) {
        return map.containsKey(o);
    }

    public boolean add(E e) {
        return map.put(e, null) == null;
    }

    public boolean remove(Object o) {
        return map.remove(o) == FastHashMap.DUMMY_VALUE;
    }

    public void clear() {
        map.clear();
    }

    @SuppressWarnings("unchecked")
    public FastHashSet<E> clone() {
        FastHashSet<E> newSet = null;
        try {
            newSet = (FastHashSet<E>) super.clone();
        } catch (CloneNotSupportedException e) {
        }
        newSet.map = (FastHashMap<E, Object>) map.clone();
        return newSet;
    }

    private void writeObject(java.io.ObjectOutputStream s)
        throws IOException {
        // Write out any hidden serialization magic
        s.defaultWriteObject();

        // Write out HashMap capacity and load factor
        s.writeInt(map.capacity());
        s.writeFloat(map.loadFactor());

        // Write out size
        s.writeInt(map.size());

        // Write out all elements in the proper order.
        for (Iterator<E> i=map.keySet().iterator(); i.hasNext(); )
            s.writeObject(i.next());
    }

    private void readObject(java.io.ObjectInputStream s)
        throws IOException, ClassNotFoundException {
        // Read in any hidden serialization magic
        s.defaultReadObject();

        // Read in HashMap capacity and load factor and create backing HashMap
        int capacity = s.readInt();
        float loadFactor = s.readFloat();
        map =  this instanceof FastLinkedHashSet ?
               new FastLinkedHashMap<E,Object>(capacity, loadFactor, false, false) :
               new FastHashMap<E,Object>(capacity, loadFactor, false);

        // Read in size
        int size = s.readInt();

        // Read in all elements in the proper order.
        for (int i=0; i<size; i++) {
            @SuppressWarnings("unchecked")
            E e = (E) s.readObject();
            map.put(e, null);
        }
    }
}
