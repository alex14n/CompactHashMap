import FixedHashSet._

/** <p>
 *  This class implements mutable maps using a hashtable.
 *  </p>
 *  <p>
 *  Implementation is very memory-compact, especially on primitive types.
 *  CompactHashMap[Int,Int] with 1 million elements consumes ~16Mb of memory.
 *  Standard java/scala mutable HashMaps consumes ~46-60Mb on 32-bit platform
 *  and ~80-100Mb on 64-bit platform.
 *  </p>
 *  <p>
 *  <b>Not</b> thread-safe!
 *  </p>
 *  <p>
 *  Preserves iteration order until no elements are deleted.
 *  </p>
 *  <p>
 *  <code>null</code> is valid for both key and value.
 *  </p>
 *
 *  @author  Alex Yakovlev
 */
object CompactHashMap {

  /** Construct an empty CompactHashMap.
   */
  def apply[K,V] = new CompactHashMap[K,V]

  /** Construct an empty map with given elements classes.
   */
  def apply[K,V] (keyClass: Class[K], valueClass: Class[V]) =
    new CompactHashMap (keyClass, valueClass)

  /** Construct an empty map with given elements.
   */
  def apply[K,V] (elems: (K,V)*) =
    (new CompactHashMap[K,V] /: elems) {
      (m,p) => m update (p._1, p._2); m }
}

/**
 */
@serializable
class CompactHashMap[K,V] (
  private[this] final var keyClass: Class[K],
  private[this] final var valueClass: Class[V]
) extends scala.collection.mutable.Map[K,V] {

  def this () = this (null, null)

  private def this (
    keys: FixedHashSet[K],
    values: Array[V],
    valClass: Class[V]
  ) = {
    this (keys.elemClass, valClass)
    myKeys = keys
    myValues = values
  }

  /** FixedHashSet with this map's keys.
   */
  final private[this] var myKeys = EmptyHashSet.asInstanceOf[FixedHashSet[K]]

  /** Array with this map's values.
   */
  final private[this] var myValues: Array[V] = null

  /** Is the given key mapped to a value by this map?
   *
   *  @param   key  the key
   *  @return  <code>true</code> if there is a mapping for key in this map
   */
  override def contains (key: K) = myKeys.positionOf(key) >= 0

  /** Check if this map maps <code>key</code> to a value and return the
   *  value if it exists.
   *
   *  @param   key  the key of the mapping of interest
   *  @return  the value of the mapping, if it exists
   */
  def get (key: K): Option[V] = {
    val i = myKeys.positionOf(key)
    if (i >= 0) Some(myValues(i)) else None
  }

  /** Retrieve the value which is associated with the given key.
   *  If there is no mapping from the given key to a value,
   *  default(key) is returned (currenly throws an exception).
   *
   *  @param   key  the key
   *  @return  the value associated with the given key.
   */
  override def apply (key: K): V = {
    val i = myKeys.positionOf(key)
    if (i >= 0) myValues(i) else default(key)
  }

  /** Check if this map maps <code>key</code> to a value.
    *  Return that value if it exists, otherwise return <code>default</code>.
    */
  override def getOrElse[V2 >: V] (key: K, default: => V2): V2 = {
    val i = myKeys.positionOf(key)
    if (i >= 0) myValues(i) else default
  }

  /** Check if this map maps <code>key</code> to a value.
    *  Return that value if it exists, otherwise return <code>default</code>.
    */
  def getOrElseF[V2 >: V] (key: K, default: () => V2): V2 = {
    val i = myKeys.positionOf(key)
    if (i >= 0) myValues(i) else default()
  }

  /** Check if this map maps <code>key</code> to a value.
    *  Return that value if it exists, otherwise return <code>default</code>.
    */
  def getOrElseV[V2 >: V] (key: K, default: V2): V2 = {
    val i = myKeys.positionOf(key)
    if (i >= 0) myValues(i) else default
  }

  /** Returns the size of this hash map.
   */
  def size = myKeys.size

  /** Removes all elements from the map.
   *  After this operation is completed, the map will be empty.
   */
  override def clear {
    myKeys.clear
    if (myValues ne null) {
      var i = myValues.length
      while (i > 0) {
        i -= 1
        myValues(i) = null.asInstanceOf[V]
      }
    }
  }

  /** Resize map.
   */
  private[this] def resize (key: K, value: V) {
    // determine keys and values classes by first inserted objects
    // if they were not specified during map creation
    if (keyClass eq null) keyClass = (
      if (key.asInstanceOf[Object] eq null) classOf[Object]
      else key.asInstanceOf[Object].getClass
    ).asInstanceOf[Class[K]]
    if (valueClass eq null) valueClass = (
      if (value.asInstanceOf[Object] eq null) classOf[Object]
      else value.asInstanceOf[Object].getClass
    ).asInstanceOf[Class[V]]
    //
    val newKeys = FixedHashSet (myKeys.bits + 1, keyClass)
    val newValues = newArray (valueClass, newKeys.capacity)
    val keysArray = myKeys.getArray
    if (keysArray ne null) {
      val len = keysArray.size
      if (len == myKeys.size) {
        var i = 0
        while (i < len) {
          val j = newKeys.addNew (keysArray(i))
          newValues(j) = myValues(i)
          i += 1
        }
      } else
      myKeys.copyTo (newKeys, (i,j) => newValues(i) = myValues(j))
    }
    myKeys = newKeys
    myValues = newValues
  }

  /** This method allows one to add a new mapping from <code>key</code>
   *  to <code>value</code> to the map. If the map already contains a
   *  mapping for <code>key</code>, it will be overridden by this
   *  function.
   *
   * @param  key    The key to update
   * @param  value  The new value
   */
  def update (key: K, value: V) =
    try {
      val i = myKeys.add (key)
      myValues(i) = value
    } catch {
      case e: ResizeNeeded =>
        resize (key, value)
        val i2 = myKeys.addNew (key)
        myValues(i2) = value
    }

  /** Insert new key-value mapping or update existing with given function.
   *
   * @param  key  The key to update
   * @param  newValue  The new value
   * @param  updateFunction  Function to apply to existing value
   */
  def insertOrUpdate (key: K, newValue: => V, updateFunction: V => V) {
    val i = myKeys.positionOf(key)
    if (i >= 0) myValues(i) = updateFunction (myValues(i))
    else {
      val newV = newValue
      try {
        val j = myKeys.addNew (key)
        myValues(j) = newV
      } catch {
        case e: ResizeNeeded =>
          resize (key, newV)
          val j = myKeys.addNew (key)
          myValues(j) = newV
      }
    }
  }

  /** Insert new key-value mapping or update existing with given function.
   *
   * @param  key  The key to update
   * @param  newValue  Function to get new value
   * @param  updateFunction  Function to apply to existing value
   */
  def insertOrUpdateF (key: K, newValue: () => V, updateFunction: V => V) {
    val i = myKeys.positionOf(key)
    if (i >= 0) myValues(i) = updateFunction (myValues(i))
    else {
      val newV = newValue ()
      try {
        val j = myKeys.addNew (key)
        myValues(j) = newV
      } catch {
        case e: ResizeNeeded =>
          resize (key, newV)
          val j = myKeys.addNew (key)
          myValues(j) = newV
      }
    }
  }

  /** Insert new key-value mapping or update existing with given function.
   *
   * @param  key  The key to update
   * @param  newValue  Function to get new value
   * @param  updateFunction  Function to apply to existing value
   */
  def insertOrUpdateV (key: K, newValue: V, updateFunction: V => V) {
    val i = myKeys.positionOf(key)
    if (i >= 0) myValues(i) = updateFunction (myValues(i))
    else try {
      val j = myKeys.addNew (key)
      myValues(j) = newValue
    } catch {
      case e: ResizeNeeded =>
        resize (key, newValue)
        val j = myKeys.addNew (key)
        myValues(j) = newValue
    }
  }

  /** Remove a key from this map, noop if key is not present.
   *
   *  @param  key  the key to be removed
   */
  def -= (key: K) {
    val i = myKeys.delete (key)
    if (i >= 0) myValues(i) = null.asInstanceOf[V]
  }

  /** Creates an iterator for all key-value pairs.
   *
   *  @return  an iterator over all key-value pairs.
   */
  def elements = myKeys.elementsMap { (k,i) => (k -> myValues(i)) }

  /** Creates an iterator for a contained values.
   *
   *  @return  an iterator over all values.
   */
  override def values = myKeys.elementsMap { (k,i) => myValues(i) }

  /** Creates an iterator for all keys.
   *
   *  @return  an iterator over all keys.
   */
  override def keys = myKeys.elements

  /** Set of this map keys.
   *
   * @return the keys of this map as a set.
   */
  override def keySet: scala.collection.Set[K] = myKeys

  /** Return a clone of this map.
   *
   *  @return a map with the same elements.
   */
  override def clone = {
    val newKeys = FixedHashSet (myKeys.bits, keyClass)
    val newValues = newArray (valueClass, newKeys.capacity)
    myKeys.copyTo (newKeys, (i,j) => newValues(i) = myValues(j))
    new CompactHashMap (newKeys, newValues, valueClass)
  }

  /** Returns a new map containing all elements of this map that
   *  satisfy the predicate <code>p</code>.
   *
   *  @param   p  the predicate used to filter the map.
   *  @return  the elements of this map satisfying <code>p</code>.
   */
  override def filter (p: ((K,V)) => Boolean) = {
    var newValues: Array[V] = null
    val newKeys = myKeys.filter (
      (k,i) => p(k, myValues(i)),
      bits => if (bits >= 0) newValues = newArray (valueClass, 1 << bits),
      (i,j) => newValues(i) = myValues(j)
    )
    new CompactHashMap (newKeys, newValues, valueClass)
  }

  /** Converts this map to a fresh Array with elements.
    */
  def toArray = {
    val a = new Array[(K,V)] (myKeys.size)
    var i = 0
    elements foreach { x => a{i} = x; i += 1 }
    a
  }
}
