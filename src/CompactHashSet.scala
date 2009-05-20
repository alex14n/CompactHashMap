import FixedHashSet._

/** <p>
 *  This class implements mutable sets using a hashtable.
 *  </p>
 *  <p>
 *  Implementation is very memory-compact, especially on primitive types.
 *  </p>
 *  <p>
 *  <b>Not</b> thread-safe!
 *  </p>
 *  <p>
 *  Preserves iteration order until no elements are deleted.
 *  </p>
 *  <p>
 *  <code>null</code> is a valid element value.
 *  </p>
 *
 *  @author  Alex Yakovlev
 */
object CompactHashSet {

  /** Construct an empty CompactHashSet.
   */
  def apply[T] = new CompactHashSet[T]

  /** Construct an empty set with given elements class.
   */
  def apply[T] (elemClass: Class[T]) =
    new CompactHashSet (elemClass)

  /** Construct an empty set with given elements class
   *  and initial capacity.
   */
  def apply[T] (elemClass: Class[T], capacity: Int) =
    new CompactHashSet (elemClass, capacity)

  /** Construct an empty set with given elements.
   */
  def apply[T] (elems: T*) =
    (new CompactHashSet[T] /: elems) {
      (s,p) => s += p; s }
}

/**
 */
@serializable
class CompactHashSet[T] (elemClass: Class[T])
extends scala.collection.mutable.Set[T] {

  def this () = this (null.asInstanceOf[Class[T]])

  def this (set: FixedHashSet[T]) = {
    this (set.elemClass)
    fixedSet = set
  }

  def this (elemClass: Class[T], capacity: Int) = {
    this (elemClass)
    var bits = initialBits
    while ((1 << bits) < capacity) bits += 1
    fixedSet = FixedHashSet (bits, elemClass)
  }

  /** Array to hold this set elements.
   */
  private[this] var fixedSet = EmptyHashSet.asInstanceOf[FixedHashSet[T]]

  /** Check if this set contains element <code>elem</code>.
   *
   *  @param  elem  the element to check for membership.
   *  @return  <code>true</code> if <code>elem</code> is contained in this set.
   */
  def contains (elem: T) = fixedSet.positionOf (elem) >= 0

  /** Add a new element to the set.
   *
   *  @param  elem  the element to be added
   */
  def += (elem: T): Unit = try {
    fixedSet.add (elem)
  } catch {
    case e: ResizeNeeded =>
      val newFixedSet = FixedHashSet (
        fixedSet.bits + 1,
        // determine elements class by first inserted object
        // if it was not specified during set creation
        if (elemClass ne null) elemClass else
          if (fixedSet.elemClass ne null) fixedSet.elemClass else (
            if (elem.asInstanceOf[Object] eq null) classOf[Object] else
              elem.asInstanceOf[Object].getClass
          ).asInstanceOf[Class[T]]
      )
      newFixedSet.copyFrom (fixedSet, null)
      fixedSet = newFixedSet
      fixedSet.add (elem)
  }

  /** Returns the size of this hash set.
   */
  def size = fixedSet.size

  /** Creates an iterator for all set elements.
   *
   *  @return  an iterator over all set elements.
   */
  def elements = fixedSet.elements

  /** Removes a single element from a set.
   *
   *  @param  elem  The element to be removed.
   */
  def -= (elem: T) { fixedSet.delete (elem) }

  /** Return a clone of this set.
   *
   *  @return  a set with the same elements.
   */
  override def clone = new CompactHashSet (fixedSet.clone)

  /** Returns a new set containing all elements of this set that
   *  satisfy the predicate <code>p</code>.
   *
   *  @param   p  the predicate used to filter the set.
   *  @return  the elements of this set satisfying <code>p</code>.
   */
  override def filter (p: T => Boolean) =
    new CompactHashSet (fixedSet.filter ((e,i) => p(e), null, null))

  /** Removes all elements from the set.
   *  After this operation is completed, the set will be empty.
   */
  override def clear { fixedSet.clear }

  /** New List with this set elements.
   */
  override def toList = fixedSet.toList
}

/** Hash set backed by fixed size array.
 */
@serializable
private abstract class FixedHashSet[T] (
  final val bits: Int,
  final val elemClass: Class[T],
  a: Array[T]
) extends scala.collection.Set[T] {

  /** Index of the first element in array with given hash.
   */
  protected def firstIndex (i: Int): Int

  /** Method to access a linked list of elements
   *  in array with the same hash code.
   *
   *  Non-negative values are indices of the next element,
   *  -1 (default) is empty spot (or end of deleted list),
   *  -2 is 'end of list',
   *  other negative values are indices of next deleted element.
   */
  protected def nextIndex (i: Int): Int

  /** Set index of the first element in array with given hash.
   */
  protected def setFirstIndex (i: Int, v: Int)

  /** Update linked list of elements with equal hash.
   */
  protected def setNextIndex (i: Int, v: Int)

  /** Mask for hashcode to store in empty index bits.
   */
  def hcBitmask: Int

  /** Return index of elem in array or -1 if it does not exists.
   */
  def positionOf[B >: T] (elem: B): Int

  /** Array with this set elements.
   */
  final private[this] val array = a

  /** Cache array length as local variable
   */
  final private[this] val arrayLength =
    if (array eq null) 0 else array.length

  /** Number of elements in this set
   */
  private[this] var counter = 0

  /** Starting index of empty elements in array
   */
  private[this] var firstEmptyIndex = 0

  /** Index of first deleted elements list in array
   */
  private[this] var firstDeletedIndex = -1

  /** Number of elements in this set.
   */
  final def size = counter

  /** Maximum number of elements in this set's array.
   */
  final def capacity = arrayLength

  /** Set size (for internal use only).
   */
  final protected def setSize (newSize: Int) {
    counter = newSize
    firstEmptyIndex = newSize
    firstDeletedIndex = -1
  }

  /** Array with this set elements.
   */
  final def getArray = array

  /** Array with this linked list indices.
   */
  protected def getIndexArray: AnyRef

  /** Return true if given array index is empty (or deleted).
   */
  def isEmpty (i: Int): Boolean

  /** Return index in array to insert new element.
   */
  protected final def findEmptySpot = {
    if (counter >= arrayLength) throw new ResizeNeeded
    counter += 1
    if (firstDeletedIndex >= 0) {
      val i = firstDeletedIndex
      firstDeletedIndex = nextIndex (firstDeletedIndex)
      if (firstDeletedIndex < -2) firstDeletedIndex = -3-firstDeletedIndex
      i
    } else {
      val i = firstEmptyIndex
      firstEmptyIndex += 1
      i
    }
  }

  /** Adds element to set.
   *  Throws ResizeNeeded if set is full.
   *
   * @return  index of inserted element in array
   */
  def add (elem: T): Int

  /** Adds element to set that does not already exist in this set.
   *  Throws ResizeNeeded if set is full.
   *
   * @return  index of inserted element in array
   */
  final def addNew (elem: T) = {
    val newIndex = findEmptySpot
    // (inline hashCode) position in firstIndex table
    val h = if (null eq elem.asInstanceOf[Object]) 0 else elem.hashCode
    val hc = (h >>> 20) ^ (h >>> 12) ^ (h >>> 7) ^ (h >>> 4) ^ h
    val i = hc & (arrayLength - 1)
    // Sorry, it was a test if inheritance instead of association will work...
    getIndexArray match {
      case ia: Array[Int] =>
        val next = ia (i)
        ia (i) = ~(newIndex | (hc & hcBitmask))
        ia (arrayLength + newIndex) = if (next < 0) next else 1
      case ia: Array[Short] =>
        val next = ia (i)
        ia (i) = (~newIndex ^ (hc & hcBitmask)).asInstanceOf[Short]
        ia (arrayLength + newIndex) = if (next < 0) next else 1
      case ia: Array[Byte] =>
        val next = ia (i)
        ia (i) = (~newIndex ^ (hc & hcBitmask)).asInstanceOf[Byte]
        ia (arrayLength + newIndex) = if (next < 0) next else 1
    }
    array(newIndex) = elem
    newIndex
  }

  /** Copy all elements from another FixedHashSet.
   *
   *  @param  that  another set to copy elements from.
   *  @param  callback  function to call for each copied element
   *                    with its new and old indices in set's arrays.
   */
  def copyFrom (that: FixedHashSet[T], callback: (Int,Int) => Unit)

  /** Make copy of this set filtered by predicate.
   *
   *  @param  p  Predicate to test elements.
   *  @param  newCallback  function to call when new set is created
   *                       with number of bits in new set's array.
   *  @param  copyCallback  function to call for each copied element
   *                    with its new and old indices in set's arrays.
   */
  final def filter (
    p: (T,Int) => Boolean,
    newCallback: Int => Unit,
    copyCallback: (Int,Int) => Unit
  ) = {
    // First, test all element with predicate,
    // count, and store test results in a bit set.
    val bitSet = new Array[Long] (1 max (arrayLength >>> 6))
    var count = 0
    var newBits = initialBits
    var i = 0
    while (i < firstEmptyIndex) {
      if (!isEmpty(i) && p(array(i),i)) {
        bitSet(i >>> 6) |= 1L << (i & 63)
        count += 1
        if (count > (1 << newBits)) newBits += 1
      }
      i += 1
    }
    // Now we can allocate set with exact size.
    if (count == 0) {
      if (null ne newCallback) newCallback (-1)
      EmptyHashSet.asInstanceOf[FixedHashSet[T]]
    } else {
      val c = FixedHashSet (newBits, elemClass)
      if (null ne newCallback) newCallback (newBits)
      i = 0
      while (i < firstEmptyIndex) {
        if ((bitSet(i >>> 6) >>> (i & 63) & 1) != 0) {
          val i2 = c.addNew (array(i))
          if (null ne copyCallback) copyCallback (i2, i)
        }
        i += 1
      }
      c
    }
  }

  /** Make complete copy of this set.
   */
  override final def clone = {
    // ToDo: -> arraycopy
    val c = FixedHashSet (bits, elemClass)
    c.copyFrom (this, null)
    c
  }

  /** Removes all elements from the set.
   */
  def clear {
    var i = firstEmptyIndex
    while (i > 0) {
      i -= 1
      array(i) = null.asInstanceOf[T]
    }
    counter = 0
    firstEmptyIndex = 0
    firstDeletedIndex = -1
  }

  /** Delete element from set.
   *
   * @return  index of deleted element in array
   *          or negative values if it was not present in set.
   */
  final def delete (elem: T) = {
    var h = if (null eq elem.asInstanceOf[Object]) 0 else elem.hashCode
    h = ((h >>> 20) ^ (h >>> 12) ^ (h >>> 7) ^ (h >>> 4) ^ h) & (arrayLength - 1)
    val i0 = firstIndex (h)
    var i = i0
    var prev = -1
    while (i >= 0 && array(i & (arrayLength - 1)) != elem) {
      prev = i & (arrayLength - 1)
      i = nextIndex (prev)
    }
    if (i >= 0) {
      i &= arrayLength - 1
      counter -= 1
      array(i) = null.asInstanceOf[T]

      if (prev >= 0) setNextIndex (prev, nextIndex(i))
      else setFirstIndex (h, nextIndex (i))

      /* We are limited in index domain, e.g. for Byte:
           0-127 = next element indices
           -1 = empty (array default)
           -2 = end of list
           -128 to -3 = next empty (deleted) index
         So we handle 2 last positions specially */
      if (i == firstEmptyIndex-1) {
        firstEmptyIndex = i
        setNextIndex (i, -1)
      } else {
        if (firstDeletedIndex == arrayLength-2) {
          // arrayLength-2 is out of NextIndex range
          // and can only be pointed to with firstDeletedIndex,
          // so we need to update next deleted list element
          setNextIndex (i, nextIndex(firstDeletedIndex))
          setNextIndex (firstDeletedIndex, -3-i)
        } else {
          val ni = if (firstDeletedIndex < 0) -1 else -3-firstDeletedIndex
          setNextIndex (i, ni)
          firstDeletedIndex = i
        }
      }
    }
    i
  }

  /** Iterate through this set elements.
   */
  final def elements = new Iterator[T] {
    private[this] var i = 0
    def hasNext = {
      while (i < firstEmptyIndex && isEmpty(i)) i += 1
      i < firstEmptyIndex
    }
    def next = {
      while (i < firstEmptyIndex && isEmpty(i)) i += 1
      if (i < firstEmptyIndex) { i += 1; array(i-1) }
      else Iterator.empty.next
    }
  }

  /** Iterate through this set elements with function.
   */
  final def elementsMap[F] (f: (T,Int) => F) = new Iterator[F] {
    private[this] var i = 0
    def hasNext = {
      while (i < firstEmptyIndex && isEmpty(i)) i += 1
      i < firstEmptyIndex
    }
    def next = {
      while (i < firstEmptyIndex && isEmpty(i)) i += 1
      if (i < firstEmptyIndex) { i += 1; f (array(i-1), i-1) }
      else Iterator.empty.next
    }
  }

  /** Return <code>true</code> if set contains element <code>elem</code>.
   */
  final def contains (elem: T) = positionOf (elem) >= 0

  /** List of this set elements.
   */
  final override def toList = {
    var list = List[T] ()
    var i = firstEmptyIndex
    while (i > 0) {
      i -= 1
      if (!isEmpty(i))
        list = array(i) :: list
    }
    list
  }

  /** List of this set elements.
   */
  final def toListMap[F] (f: (T,Int) => F) = {
    var list = List[F] ()
    var i = firstEmptyIndex
    while (i > 0) {
      i -= 1
      if (!isEmpty(i))
        list = f(array(i),i) :: list
    }
    list
  }
}

import scala.runtime._
import scala.runtime.ScalaRunTime.boxArray
import scala.compat.Platform.createArray
import java.util.Arrays.fill

/**
 */
private final object FixedHashSet {

  final class ResizeNeeded extends Exception

  final val initialBits = 2 // 4 elements

  final val INT_NEXT_IS_EOL = 0x40000000;
  final val INT_AVAILABLE_BITS = 0x3FFFFFFF;
  final val SHORT_AVAILABLE_BITS = 0x7FFF;
  final val BYTE_AVAILABLE_BITS = 0x7F;

  /** Create new array to hold set or map elements.
   */
  final def newArray[V] (valueClass: Class[V], size: Int): Array[V] = (
    // empty array if class is undefined
    if (size <= 0 || (valueClass eq null)) null
    // primitive types
    else if (valueClass.isPrimitive)
      boxArray(createArray(valueClass,size))
    // boxed primitive types
    else if (valueClass == classOf[java.lang.Boolean])
      new BoxedBooleanArray(new Array[Boolean](size))
    else if (valueClass == classOf[java.lang.Character])
      new BoxedCharArray(new Array[Char](size))
    else if (valueClass == classOf[java.lang.Byte])
      new BoxedByteArray(new Array[Byte](size))
    else if (valueClass == classOf[java.lang.Short])
      new BoxedShortArray(new Array[Short](size))
    else if (valueClass == classOf[java.lang.Integer])
      new BoxedIntArray(new Array[Int](size))
    else if (valueClass == classOf[java.lang.Long])
      new BoxedLongArray(new Array[Long](size))
    else if (valueClass == classOf[java.lang.Float])
      new BoxedFloatArray(new Array[Float](size))
    else if (valueClass == classOf[java.lang.Double])
      new BoxedDoubleArray(new Array[Double](size))
    // general array of Object types
    else new BoxedObjectArray(new Array[Object] (size))
  ).asInstanceOf[Array[V]]

  /** FixedHashSet implementation with byte-size index arrays.
   */
  @serializable
  final class ByteHashSet[T] (bits: Int, elemClass: Class[T], a: Array[T])
  extends FixedHashSet[T] (bits, elemClass, a) {
    private[this] final val indexTable = new Array[Byte] (2 << bits)
    final def getIndexArray = indexTable

    // make -1 default instead of 0
    protected final def firstIndex (i: Int) = ~indexTable(i)
    protected final def nextIndex (i: Int) = ~indexTable(len+i)
    protected final def setFirstIndex (i: Int, v: Int) =
      indexTable(i) = (~v).asInstanceOf[Byte]
    protected final def setNextIndex (i: Int, v: Int) =
      indexTable(len+i) = (~v).asInstanceOf[Byte]

    final override def clear {
      super.clear
      fill(indexTable, 0.asInstanceOf[Byte])
    }
    final def hcBitmask = BYTE_AVAILABLE_BITS ^ (len-1)

    // 'inline' some methods for better performance

    final private[this] val len = 1 << bits
    // scalac treat 'array' as method call :-(
    // until it's private[this] so make a local copy
    final private[this] val localArray = a
    final override def positionOf[B >: T] (elem: B) = {
      val h = if (null eq elem.asInstanceOf[Object]) 0 else elem.hashCode
      val hc = (h >>> 20) ^ (h >>> 12) ^ (h >>> 7) ^ (h >>> 4) ^ h
      val mask = BYTE_AVAILABLE_BITS ^ (len-1)
      val hcBits = hc & mask
      var i = ~indexTable(hc & (len-1))
      while (i >= 0 && ((hcBits != (i & mask)) || {
        val x = localArray(i & (len-1))
        (x.asInstanceOf[Object] ne elem.asInstanceOf[Object]) && x != elem
      }))
        i = ~indexTable(len + (i & (len-1)))
      if (i >= 0) i & (len-1) else -1
    }
    final def isEmpty (i: Int) = {
      val next = indexTable(len+i)
      next == 0 || next > 1
    }
    // ToDo: Check if System.arraycopy() is slow on small arrays
    final override def copyFrom (that: FixedHashSet[T], callback: (Int,Int) => Unit) {
      val array2 = that.getArray
      val size2 = if (array2 eq null) 0 else array2.length
      var i = 0
      if (size2 == that.size) {
        fill (indexTable, len, len+size2, 1.asInstanceOf[Byte])
        val mask = BYTE_AVAILABLE_BITS ^ (len-1)
        val mask2 = that.hcBitmask
        if ((mask2 & mask) == mask) {
          val index2 = that.getIndexArray.asInstanceOf[Array[Byte]]
          while (i < size2) {
            var j = ~index2(i)
            var next1: Byte = 0
            var next2: Byte = 0
            while (j >= 0) {
              val arrayIndex = j & (size2-1)
              val hashIndex = i | (j & (mask ^ mask2))
              if (hashIndex == i) {
                if (next1 < 0) indexTable (len + arrayIndex) = next1
                next1 = (~arrayIndex ^ (j & mask)).asInstanceOf[Byte]
              } else {
                if (next2 < 0) indexTable (len + arrayIndex) = next2
                next2 = (~arrayIndex ^ (j & mask)).asInstanceOf[Byte]
              }
              // if (null ne callback) callback(arrayIndex, arrayIndex)
              j = ~index2(size2 + arrayIndex)
            }
            if (next1 < 0) indexTable (i) = next1
            if (next2 < 0) indexTable (i + size2) = next2
            i += 1
          }
        } else
          while (i < size2) {
          val e = array2(i)
          // inline addNew
          val h = if (null eq e.asInstanceOf[Object]) 0 else e.hashCode
          val hc = (h >>> 20) ^ (h >>> 12) ^ (h >>> 7) ^ (h >>> 4) ^ h
          val j = hc & (len - 1)
          val next = indexTable(j)
          indexTable (j) = (~i ^ (hc & mask)).asInstanceOf[Byte]
          if (next < 0) indexTable (len+i) = next
          if (null ne callback) callback(i, i)
          i += 1
        }
        if (array2 ne null) array2.copyToArray (localArray, 0)
        setSize (size2)
      } else {
        while (i < size2) {
          if (!that.isEmpty(i)) {
            val i2 = addNew (array2(i))
            if (null ne callback) callback(i2, i)
          }
          i += 1
        }
      }
    }
    final override def add (elem: T): Int = {
      // (inline hashCode) position in firstIndex table
      val h = if (null eq elem.asInstanceOf[Object]) 0 else elem.hashCode
      val hc = (h >>> 20) ^ (h >>> 12) ^ (h >>> 7) ^ (h >>> 4) ^ h
      val i = hc & (len - 1)
      val next = indexTable (i)
      // Check if elem already present
      val mask = BYTE_AVAILABLE_BITS ^ (len-1)
      val hcBits = hc & mask
      var j = ~next
      while (j >= 0) {
        val k = j & (len - 1)
        if (hcBits == (j & mask)) {
          val o = localArray (k)
          if ((o.asInstanceOf[Object] eq elem.asInstanceOf[Object]) || o == elem)
            return k
        }
        j = ~indexTable (len+k)
      }
      val newIndex = findEmptySpot
      indexTable (i) = (~newIndex ^ hcBits).asInstanceOf[Byte]
      indexTable (len + newIndex) = if (next < 0) next else 1
      localArray (newIndex) = elem
      newIndex
    }
  }

  /** FixedHashSet implementation with short-size index arrays.
   */
  @serializable
  final class ShortHashSet[T] (bits: Int, elemClass: Class[T], a: Array[T])
  extends FixedHashSet[T] (bits, elemClass, a) {
    private[this] final val indexTable = new Array[Short] (2 << bits)
    final def getIndexArray = indexTable

    protected final def firstIndex (i: Int) = ~indexTable(i)
    protected final def nextIndex (i: Int) = ~indexTable(len+i)
    protected final def setFirstIndex (i: Int, v: Int) =
      indexTable(i) = (~v).asInstanceOf[Short]
    protected final def setNextIndex (i: Int, v: Int) =
      indexTable(len+i) = (~v).asInstanceOf[Short]

    final override def clear {
      super.clear
      fill(indexTable, 0.asInstanceOf[Short])
    }
    final def hcBitmask = SHORT_AVAILABLE_BITS ^ (len-1)

    // 'inline' some methods for better performance

    final private[this] val len = 1 << bits
    final private[this] val localArray = a
    final override def positionOf[B >: T] (elem: B) = {
      val h = if (null eq elem.asInstanceOf[Object]) 0 else elem.hashCode
      val hc = (h >>> 20) ^ (h >>> 12) ^ (h >>> 7) ^ (h >>> 4) ^ h
      val mask = SHORT_AVAILABLE_BITS ^ (len-1)
      val hcBits = hc & mask
      var i = ~indexTable(hc & (len-1))
      while (i >= 0 && ((hcBits != (i & mask)) || {
        val x = localArray(i & (len-1))
        (x.asInstanceOf[Object] ne elem.asInstanceOf[Object]) && x != elem
      }))
        i = ~indexTable(len + (i & (len-1)))
      if (i >= 0) i & (len-1) else -1
    }
    final def isEmpty (i: Int) = {
      val next = indexTable(len+i)
      next == 0 || next > 1
    }
    final override def copyFrom (that: FixedHashSet[T], callback: (Int,Int) => Unit) {
      val array2 = that.getArray
      val size2 = if (array2 eq null) 0 else array2.length
      var i = 0
      if (size2 == that.size) {
        fill (indexTable, len, len+size2, 1.asInstanceOf[Short])
        val mask = SHORT_AVAILABLE_BITS ^ (len-1)
        val mask2 = that.hcBitmask
        if ((mask2 & mask) == mask) {
          val index2 = that.getIndexArray.asInstanceOf[Array[Short]]
          while (i < size2) {
            var j = ~index2(i)
            var next1: Short = 0
            var next2: Short = 0
            while (j >= 0) {
              val arrayIndex = j & (size2-1)
              val hashIndex = i | (j & (mask ^ mask2))
              if (hashIndex == i) {
                if (next1 < 0) indexTable (len + arrayIndex) = next1
                next1 = (~arrayIndex ^ (j & mask)).asInstanceOf[Short]
              } else {
                if (next2 < 0) indexTable (len + arrayIndex) = next2
                next2 = (~arrayIndex ^ (j & mask)).asInstanceOf[Short]
              }
              // if (null ne callback) callback(arrayIndex, arrayIndex)
              j = ~index2(size2 + arrayIndex)
            }
            if (next1 < 0) indexTable (i) = next1
            if (next2 < 0) indexTable (i + size2) = next2
            i += 1
          }
        } else
          while (i < size2) {
          val e = array2(i)
          // inline addNew
          val h = if (null eq e.asInstanceOf[Object]) 0 else e.hashCode
          val hc = (h >>> 20) ^ (h >>> 12) ^ (h >>> 7) ^ (h >>> 4) ^ h
          val j = hc & (len - 1)
          val next = indexTable(j)
          indexTable (j) = (~i ^ (hc & mask)).asInstanceOf[Short]
          if (next < 0) indexTable (len+i) = next
          if (null ne callback) callback(i, i)
          i += 1
        }
        if (array2 ne null) array2.copyToArray (localArray, 0)
        setSize (size2)
      } else {
        while (i < size2) {
          if (!that.isEmpty(i)) {
            val i2 = addNew (array2(i))
            if (null ne callback) callback(i2, i)
          }
          i += 1
        }
      }
    }
    final override def add (elem: T): Int = {
      // (inline hashCode) position in firstIndex table
      val h = if (null eq elem.asInstanceOf[Object]) 0 else elem.hashCode
      val hc = (h >>> 20) ^ (h >>> 12) ^ (h >>> 7) ^ (h >>> 4) ^ h
      val i = hc & (len - 1)
      val next = indexTable (i)
      // Check if elem already present
      val mask = SHORT_AVAILABLE_BITS ^ (len-1)
      val hcBits = hc & mask
      var j = ~next
      while (j >= 0) {
        val k = j & (len - 1)
        if (hcBits == (j & mask)) {
          val o = localArray (k)
          if ((o.asInstanceOf[Object] eq elem.asInstanceOf[Object]) || o == elem)
            return k
        }
        j = ~indexTable (len+k)
      }
      val newIndex = findEmptySpot
      indexTable (i) = (~newIndex ^ hcBits).asInstanceOf[Short]
      indexTable (len + newIndex) = if (next < 0) next else 1
      localArray (newIndex) = elem
      newIndex
    }
  }

  /** FixedHashSet implementation with int-size index arrays.
   */
  @serializable
  final class IntHashSet[T] (bits: Int, elemClass: Class[T], a: Array[T])
  extends FixedHashSet[T] (bits, elemClass, a) {
    private[this] final val indexTable = new Array[Int] (2 << bits)
    final def getIndexArray = indexTable

    protected final def firstIndex (i: Int) = ~indexTable(i)
    protected final def nextIndex (i: Int) = ~indexTable(len+i)
    protected final def setFirstIndex (i: Int, v: Int) = indexTable(i) = ~v
    protected final def setNextIndex (i: Int, v: Int) = indexTable(len+i) = ~v

    final override def clear {
      super.clear
      fill(indexTable, 0)
    }
    final def hcBitmask = INT_AVAILABLE_BITS ^ (len-1)

    // 'inline' some methods for better performance

    final private[this] val len = 1 << bits
    final private[this] val localArray = a
    final override def positionOf[B >: T] (elem: B): Int = {
      val h = if (null eq elem.asInstanceOf[Object]) 0 else elem.hashCode
      val hc = (h >>> 20) ^ (h >>> 12) ^ (h >>> 7) ^ (h >>> 4) ^ h
      val mask = INT_AVAILABLE_BITS ^ (len-1)
      val hcBits = hc & mask
      var prev = -1
      var curr = hc & (len-1)
      var i = ~indexTable(curr)
      while (i >= 0) {
        prev = curr
        curr = i & (len-1)
        if (hcBits == (i & mask)) {
          val x = localArray(curr)
          if ((x.asInstanceOf[Object] eq elem.asInstanceOf[Object]) || x == elem)
            return curr;
        }
        if ((i & INT_NEXT_IS_EOL) != 0) return -1
        curr += len
        i = ~indexTable(curr)
      }
      if (prev >= 0) indexTable(prev) ^= INT_NEXT_IS_EOL;
      -1
    }
    final def isEmpty (i: Int) = {
      val next = indexTable(len+i)
      next == 0 || next > 1
    }
    final override def copyFrom (that: FixedHashSet[T], callback: (Int,Int) => Unit) {
      val array2 = that.getArray
      val size2 = if (array2 eq null) 0 else array2.length
      var i = 0
      if (size2 == that.size) {
        fill (indexTable, len, len+size2, 1)
        val mask = INT_AVAILABLE_BITS ^ (len-1)
        val mask2 = that.hcBitmask
        if ((mask2 & mask) == mask) {
          val index2 = that.getIndexArray.asInstanceOf[Array[Int]]
          while (i < size2) {
            var j = ~index2(i)
            var next1 = 0
            var next2 = 0
            while (j >= 0) {
              val arrayIndex = j & (size2-1)
              val hashIndex = i | (j & (mask ^ mask2))
              if (hashIndex == i) {
                if (next1 < 0) indexTable (len + arrayIndex) = next1
                next1 = ~(arrayIndex | (j & mask) | (if (next1 < 0) 0 else INT_NEXT_IS_EOL))
              } else {
                if (next2 < 0) indexTable (len + arrayIndex) = next2
                next2 = ~(arrayIndex | (j & mask) | (if (next2 < 0) 0 else INT_NEXT_IS_EOL))
              }
              // if (null ne callback) callback(arrayIndex, arrayIndex)
              j = if ((j & INT_NEXT_IS_EOL) != 0) -1 else ~index2(size2 + arrayIndex)
            }
            if (next1 < 0) indexTable (i) = next1
            if (next2 < 0) indexTable (i + size2) = next2
            i += 1
          }
        } else
          while (i < size2) {
          val e = array2(i)
          // inline addNew
          val h = if (null eq e.asInstanceOf[Object]) 0 else e.hashCode
          val hc = (h >>> 20) ^ (h >>> 12) ^ (h >>> 7) ^ (h >>> 4) ^ h
          val j = hc & (len - 1)
          val next = indexTable(j)
          indexTable (j) = ~(i | (hc & mask))
          if (next < 0) indexTable (len+i) = next
          if (null ne callback) callback(i, i)
          i += 1
        }
        if (array2 ne null) array2.copyToArray (localArray, 0)
        setSize (size2)
      } else {
        while (i < size2) {
          if (!that.isEmpty(i)) {
            val i2 = addNew (array2(i))
            if (null ne callback) callback(i2, i)
          }
          i += 1
        }
      }
    }
    final override def add (elem: T): Int = {
      // (inline hashCode) position in firstIndex table
      val h = if (null eq elem.asInstanceOf[Object]) 0 else elem.hashCode
      val hc = (h >>> 20) ^ (h >>> 12) ^ (h >>> 7) ^ (h >>> 4) ^ h
      val i = hc & (len - 1)
      val next = indexTable (i)
      // Check if elem already present
      val mask = INT_AVAILABLE_BITS ^ (len-1)
      val hcBits = hc & mask
      var j = ~next
      while (j >= 0) {
        val k = j & (len - 1)
        if (hcBits == (j & mask)) {
          val o = localArray (k)
          if ((o.asInstanceOf[Object] eq elem.asInstanceOf[Object]) || o == elem)
            return k
        }
        j = if ((j & INT_NEXT_IS_EOL) != 0) -1 else ~indexTable (len+k)
      }
      val newIndex = findEmptySpot
      indexTable (i) = ~(newIndex | hcBits | (if (next < 0) 0 else INT_NEXT_IS_EOL))
      indexTable (len + newIndex) = if (next < 0) next else 1
      localArray (newIndex) = elem
      newIndex
    }
  }

  /** FixedHashSet implementation with int-size index arrays.
   */
  @serializable
  final class IntObjectHashSet[T] (bits: Int, elemClass: Class[T], a: Array[T])
  extends FixedHashSet[T] (bits, elemClass, a) {
    private[this] final val indexTable = new Array[Int] (2 << bits)
    final def getIndexArray = indexTable

    protected final def firstIndex (i: Int) = ~indexTable(i)
    protected final def nextIndex (i: Int) = ~indexTable(len+i)
    protected final def setFirstIndex (i: Int, v: Int) = indexTable(i) = ~v
    protected final def setNextIndex (i: Int, v: Int) = indexTable(len+i) = ~v

    final override def clear {
      super.clear
      fill(indexTable, 0)
    }
    final def hcBitmask = INT_AVAILABLE_BITS ^ (len-1)

    // 'inline' some methods for better performance

    final private[this] val len = 1 << bits
    final private[this] val localArray: Array[Object] = a.asInstanceOf[BoxedObjectArray].value
    final override def positionOf[B >: T] (elem: B): Int = {
      val h = if (null eq elem.asInstanceOf[Object]) 0 else elem.hashCode
      val hc = (h >>> 20) ^ (h >>> 12) ^ (h >>> 7) ^ (h >>> 4) ^ h
      val mask = INT_AVAILABLE_BITS ^ (len-1)
      val hcBits = hc & mask
      var prev = -1
      var curr = hc & (len-1)
      var i = ~indexTable(curr)
      while (i >= 0) {
        prev = curr
        curr = i & (len-1)
        if (hcBits == (i & mask)) {
          val x = localArray(curr)
          if ((x eq elem.asInstanceOf[Object]) || (x ne null) && (x equals elem))
            return curr;
        }
        if ((i & INT_NEXT_IS_EOL) != 0) return -1
        curr += len
        i = ~indexTable(curr)
      }
      if (prev >= 0) indexTable(prev) ^= INT_NEXT_IS_EOL;
      -1
    }
    final def isEmpty (i: Int) = {
      val next = indexTable(len+i)
      next == 0 || next > 1
    }
    final override def copyFrom (that: FixedHashSet[T], callback: (Int,Int) => Unit) {
      val array2 = that.getArray
      val size2 = if (array2 eq null) 0 else array2.length
      var i = 0
      if (size2 == that.size) {
        fill (indexTable, len, len+size2, 1)
        val mask = INT_AVAILABLE_BITS ^ (len-1)
        val mask2 = that.hcBitmask
        if ((mask2 & mask) == mask) {
          val index2 = that.getIndexArray.asInstanceOf[Array[Int]]
          while (i < size2) {
            var j = ~index2(i)
            var next1 = 0
            var next2 = 0
            while (j >= 0) {
              val arrayIndex = j & (size2-1)
              val hashIndex = i | (j & (mask ^ mask2))
              if (hashIndex == i) {
                if (next1 < 0) indexTable (len + arrayIndex) = next1
                next1 = ~(arrayIndex | (j & mask) | (if (next1 < 0) 0 else INT_NEXT_IS_EOL))
              } else {
                if (next2 < 0) indexTable (len + arrayIndex) = next2
                next2 = ~(arrayIndex | (j & mask) | (if (next2 < 0) 0 else INT_NEXT_IS_EOL))
              }
              // if (null ne callback) callback(arrayIndex, arrayIndex)
              j = if ((j & INT_NEXT_IS_EOL) != 0) -1 else ~index2(size2 + arrayIndex)
            }
            if (next1 < 0) indexTable (i) = next1
            if (next2 < 0) indexTable (i + size2) = next2
            i += 1
          }
        } else if (array2.isInstanceOf[BoxedObjectArray]) {
          val array: Array[Object] = array2.asInstanceOf[BoxedObjectArray].value
          while (i < size2) {
            val e = array(i)
            // inline addNew
            val h = if (null eq e) 0 else e.hashCode
            val hc = (h >>> 20) ^ (h >>> 12) ^ (h >>> 7) ^ (h >>> 4) ^ h
            val j = hc & (len - 1)
            val next = indexTable(j)
            indexTable (j) = ~(i | (hc & mask))
            if (next < 0) indexTable (len+i) = next
            if (null ne callback) callback(i, i)
            i += 1
          }
        } else {
          while (i < size2) {
            val e = array2(i)
            // inline addNew
            val h = if (null eq e.asInstanceOf[Object]) 0 else e.hashCode
            val hc = (h >>> 20) ^ (h >>> 12) ^ (h >>> 7) ^ (h >>> 4) ^ h
            val j = hc & (len - 1)
            val next = indexTable(j)
            indexTable (j) = ~(i | (hc & mask))
            if (next < 0) indexTable (len+i) = next
            if (null ne callback) callback(i, i)
            i += 1
          }
        }
        if (array2 ne null) array2.copyToArray (getArray, 0)
        setSize (size2)
      } else {
        while (i < size2) {
          if (!that.isEmpty(i)) {
            val i2 = addNew (array2(i))
            if (null ne callback) callback(i2, i)
          }
          i += 1
        }
      }
    }
    final override def add (elem: T): Int = {
      // (inline hashCode) position in firstIndex table
      val h = if (null eq elem.asInstanceOf[Object]) 0 else elem.hashCode
      val hc = (h >>> 20) ^ (h >>> 12) ^ (h >>> 7) ^ (h >>> 4) ^ h
      val i = hc & (len - 1)
      val next = indexTable (i)
      // Check if elem already present
      val mask = INT_AVAILABLE_BITS ^ (len-1)
      val hcBits = hc & mask
      var j = ~next
      while (j >= 0) {
        val k = j & (len - 1)
        if (hcBits == (j & mask)) {
          val o = localArray (k)
          if ((o eq elem.asInstanceOf[Object]) || ((o ne null) && (o equals elem)))
            return k
        }
        j = if ((j & INT_NEXT_IS_EOL) != 0) -1 else ~indexTable (len+k)
      }
      val newIndex = findEmptySpot
      indexTable (i) = ~(newIndex | hcBits | (if (next < 0) 0 else INT_NEXT_IS_EOL))
      indexTable (len + newIndex) = if (next < 0) next else 1
      localArray (newIndex) = elem.asInstanceOf[Object]
      newIndex
    }
  }

  /** Empty FixedHashSet implementation.
   */
  @serializable
  final object EmptyHashSet extends FixedHashSet[Any] (initialBits - 1, null, null) {
    final override def positionOf[B >: Any] (elem: B) = -1
    final def isEmpty (i: Int) = true
    protected final def firstIndex (i: Int) = -1
    protected final def nextIndex (i: Int) = -1
    protected final def setFirstIndex (i: Int, v: Int) { }
    protected final def setNextIndex (i: Int, v: Int) { }
    final def hcBitmask = 0
    final def getIndexArray = null
    final def copyFrom (that: FixedHashSet[Any], callback: (Int,Int) => Unit) { }
    final override def add (elem: Any): Int = { throw new ResizeNeeded }
  }

  /** Construct FixedHashSet implementation with given parameters.
   */
  final def apply[T] (bits: Int, elemClass: Class[T]): FixedHashSet[T] = {
    if (bits <= 0 || (elemClass eq null))
      EmptyHashSet.asInstanceOf[FixedHashSet[T]]
    else {
      val a = newArray (elemClass, 1 << bits)
      if (bits <  8)
        new ByteHashSet (bits, elemClass, a) else
      if (bits < 16)
        new ShortHashSet (bits, elemClass, a) else
      if (a.isInstanceOf[BoxedObjectArray])
        new IntObjectHashSet (bits, elemClass, a)
      else
        new IntHashSet (bits, elemClass, a)
    }
  }
}
