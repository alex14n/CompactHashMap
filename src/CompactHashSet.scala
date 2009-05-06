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
      fixedSet.copyTo (newFixedSet, null)
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
}

/** Hash set backed by fixed size array.
 */
@serializable
private abstract class FixedHashSet[T] (
  final val bits: Int,
  final val elemClass: Class[T]
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

  /** Array with this set elements.
   */
  final private[this] val array = newArray (elemClass, 1 << bits)

  /** Cache array length as local variable
   */
  final private[this] val arrayLength =
    if (array eq null) 0 else array.length

  /** Objects original (full) hash codes.
   */
  // final val hashCodes = new Array[Int] (arrayLength);

  /** Number of elements in this set
   */
  final private[this] var counter = 0

  /** Starting index of empty elements in array
   */
  final private[this] var firstEmptyIndex = 0

  /** Index of first deleted elements list in array
   */
  final private[this] var firstDeletedIndex = -1

  /** Number of elements in this set.
   */
  final def size = counter

  /** Maximum number of elements in this set's array.
   */
  final def capacity = arrayLength

  /** Array with this set elements.
   */
  final def getArray = array

  /** Hash code of given element.
  private[this] final def hashIndex (elem: T) = {
    var h = if (null eq elem.asInstanceOf[Object]) 0 else elem.hashCode
    h ^= (h >>> 20) ^ (h >>> 12) ^ (h >>> 7) ^ (h >>> 4)
    h & (arrayLength - 1)
  }
   */

  /** Return index of given element in this set's array.
   *  Negative values means that there is no such element.
   */
  def positionOf (elem: T): Int /* = {
    var i = firstIndex(hashIndex(elem))
    while (i >= 0 && array(i) != elem)
      i = nextIndex(i)
    i
  } */

  /** Return true if given array index is empty (or deleted).
   */
  def isEmpty (i: Int): Boolean /* = {
    val next = nextIndex(i)
    next == -1 || next < -2
  } */

  /** Return index in array to insert new element.
   */
  private[this] final def findEmptySpot =
    if (firstDeletedIndex >= 0) {
      val i = firstDeletedIndex
      firstDeletedIndex = nextIndex(firstDeletedIndex)
      if (firstDeletedIndex < -2) firstDeletedIndex = -3-firstDeletedIndex
      i
    } else firstEmptyIndex

  /** Adds element to set.
   *  Throws ResizeNeeded if set is full.
   */
  final def add (elem: T) =
    if (counter >= arrayLength)
      throw new ResizeNeeded
    else {
      // (inline hashCode) position in firstIndex table
      val h = if (null eq elem.asInstanceOf[Object]) 0 else elem.hashCode
      var i = ((h >>> 20) ^ (h >>> 12) ^ (h >>> 7) ^ (h >>> 4) ^ h) & (arrayLength - 1)
      var j = firstIndex (i)
      // there already is element(s) with this hash
      if (j >= 0) {
        var found = array(j) == elem
        while (!found && nextIndex(j) >= 0) {
          j = nextIndex(j)
          found = array(j) == elem
        }
        // This element already present in set?
        if (found) j else {
          // FirstIndex array is already set,
          // add element into NextIndex linked list
          val newIndex = findEmptySpot
          setNextIndex (j, newIndex)
          setNextIndex (newIndex, -2) // Mark as 'set', default -1 means 'empty'
          array(newIndex) = elem
          // hashCodes(newIndex) = h
          counter += 1
          if (newIndex == firstEmptyIndex) firstEmptyIndex += 1
          newIndex
        }
      } else {
        // this is the first elemens with that hash code,
        // insert it into FirstIndex array
        val newIndex = findEmptySpot
        setFirstIndex (i, newIndex)
        setNextIndex (newIndex, -2) // Mark as 'set', default -1 means 'empty'
        array(newIndex) = elem
        // hashCodes(newIndex) = h
        counter += 1
        if (newIndex == firstEmptyIndex) firstEmptyIndex += 1
        newIndex
      }
    }

  /** Copy all elements to another FixedHashSet.
   *
   *  @param  that  another set to copy elements to.
   *  @param  callback  function to call for each copied element
   *                    with its new and old indices in set's arrays.
   */
  final def copyTo (that: FixedHashSet[T], callback: (Int,Int) => Unit) {
    var i = 0
    while (i < firstEmptyIndex) {
      if (!isEmpty(i)) {
        val i2 = that.add(array(i))
        if (null ne callback) callback(i2, i)
      }
      i += 1
    }
  }

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
    var newBits = 4 // at least we should have 16 elements
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
        if ((bitSet(i >>> 6) >>> (i & 63) & 1) > 0) {
          val i2 = c.add(array(i))
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
    val c = FixedHashSet (bits, elemClass)
    copyTo (c, null)
    c
  }

  /** Removes all elements from the set.
   */
  def clear {
    counter = 0
    firstEmptyIndex = 0
    firstDeletedIndex = -1

    var i = firstEmptyIndex
    while (i > 0) {
      i -= 1
      array(i) = null.asInstanceOf[T]
    }
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
    while (i >= 0 && array(i) != elem) {
      prev = i
      i = nextIndex (i)
    }

    if (i >= 0) {
      counter -= 1
      array(i) = null.asInstanceOf[T]

      if (i0 == i) {
        var ni = nextIndex (i)
        setFirstIndex (h, ni)
      } else
      if (prev >= 0) setNextIndex(prev, nextIndex(i))

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
}

import scala.runtime._
import scala.runtime.ScalaRunTime.boxArray
import scala.compat.Platform.createArray
import java.util.Arrays.fill

/**
 */
private final object FixedHashSet {

  class ResizeNeeded extends Exception

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
    else new Array[V] (size)
  ).asInstanceOf[Array[V]]

  /** FixedHashSet implementation with byte-size index arrays.
   */
  @serializable
  final class ByteHashSet[T] (bits: Int, elemClass: Class[T])
  extends FixedHashSet[T] (bits, elemClass) {
    private[this] final val indexTable = new Array[Byte] (2 << bits)

    // make -1 default instead of 0
    protected final def firstIndex (i: Int) = -1-indexTable(i)
    protected final def nextIndex (i: Int) = -1-indexTable(len+i)
    protected final def setFirstIndex (i: Int, v: Int) =
      indexTable(i) = (-1-v).asInstanceOf[Byte]
    protected final def setNextIndex (i: Int, v: Int) =
      indexTable(len+i) = (-1-v).asInstanceOf[Byte]

    final override def clear {
      super.clear
      fill(indexTable, 0.asInstanceOf[Byte])
    }

    // 'inline' some methods for better performance

    final private[this] val len = 1 << bits
    final private[this] val localArray = getArray // scalac treat 'array' as method call :-(
    // final private[this] val localHashCodes = hashCodes
    final def positionOf (elem: T) = {
      val h = if (null eq elem.asInstanceOf[Object]) 0 else elem.hashCode
      var i = -1-indexTable(((h >>> 20) ^ (h >>> 12) ^ (h >>> 7) ^ (h >>> 4) ^ h) & (len-1))
      while (i >= 0 /* && localHashCodes(i) != h */ && {
        val x = localArray(i)
        (x.asInstanceOf[Object] ne elem.asInstanceOf[Object]) && x != elem
      })
        i = -1-indexTable(len+i)
      i
    }
    final def isEmpty (i: Int) = {
      val next = indexTable(len+i)
      next == 0 || next > 1
    }
  }

  /** FixedHashSet implementation with short-size index arrays.
   */
  @serializable
  final class ShortHashSet[T] (bits: Int, elemClass: Class[T])
  extends FixedHashSet[T] (bits, elemClass) {
    private[this] final val indexTable = new Array[Short] (2 << bits)

    protected final def firstIndex (i: Int) = -1-indexTable(i)
    protected final def nextIndex (i: Int) = -1-indexTable(len+i)
    protected final def setFirstIndex (i: Int, v: Int) =
      indexTable(i) = (-1-v).asInstanceOf[Short]
    protected final def setNextIndex (i: Int, v: Int) =
      indexTable(len+i) = (-1-v).asInstanceOf[Short]

    final override def clear {
      super.clear
      fill(indexTable, 0.asInstanceOf[Short])
    }

    // 'inline' some methods for better performance

    final private[this] val len = 1 << bits
    final private[this] val localArray = getArray
    // final private[this] val localHashCodes = hashCodes
    final def positionOf (elem: T) = {
      val h = if (null eq elem.asInstanceOf[Object]) 0 else elem.hashCode
      var i = -1-indexTable(((h >>> 20) ^ (h >>> 12) ^ (h >>> 7) ^ (h >>> 4) ^ h) & (len-1))
      while (i >= 0 /* && localHashCodes(i) != h */ && {
        val x = localArray(i)
        (x.asInstanceOf[Object] ne elem.asInstanceOf[Object]) && x != elem
      })
        i = -1-indexTable(len+i)
      i
    }
    final def isEmpty (i: Int) = {
      val next = indexTable(len+i)
      next == 0 || next > 1
    }
  }

  /** FixedHashSet implementation with int-size index arrays.
   */
  @serializable
  final class IntHashSet[T] (bits: Int, elemClass: Class[T])
  extends FixedHashSet[T] (bits, elemClass) {
    private[this] final val indexTable = new Array[Int] (2 << bits)

    protected final def firstIndex (i: Int) = -1-indexTable(i)
    protected final def nextIndex (i: Int) = -1-indexTable(len+i)
    protected final def setFirstIndex (i: Int, v: Int) = indexTable(i) = -1-v
    protected final def setNextIndex (i: Int, v: Int) = indexTable(len+i) = -1-v

    final override def clear {
      super.clear
      fill(indexTable, 0)
    }

    // 'inline' some methods for better performance

    final private[this] val len = 1 << bits
    final private[this] val localArray = getArray
    // final private[this] val localHashCodes = hashCodes
    final def positionOf (elem: T) = {
      val h = if (null eq elem.asInstanceOf[Object]) 0 else elem.hashCode
      var i = -1-indexTable(((h >>> 20) ^ (h >>> 12) ^ (h >>> 7) ^ (h >>> 4) ^ h) & (len-1))
      while (i >= 0 /* && localHashCodes(i) != h */ && {
        val x = localArray(i)
        (x.asInstanceOf[Object] ne elem.asInstanceOf[Object]) && x != elem
      })
        i = -1-indexTable(len+i)
      i
    }
    final def isEmpty (i: Int) = {
      val next = indexTable(len+i)
      next == 0 || next > 1
    }
  }

  /** Empty FixedHashSet implementation.
   */
  @serializable
  final object EmptyHashSet extends FixedHashSet[Any] (3, null) {
    final def positionOf (elem: Any) = -1
    final def isEmpty (i: Int) = true
    protected final def firstIndex (i: Int) = -1
    protected final def nextIndex (i: Int) = -1
    protected final def setFirstIndex (i: Int, v: Int) { }
    protected final def setNextIndex (i: Int, v: Int) { }
  }

  /** Construct FixedHashSet implementation with given parameters.
   */
  final def apply[T] (bits: Int, elemClass: Class[T]): FixedHashSet[T] =
    if (bits <= 0 || (elemClass eq null))
      EmptyHashSet.asInstanceOf[FixedHashSet[T]] else
    if (bits <  8) new ByteHashSet (bits, elemClass) else
    if (bits < 16) new ShortHashSet (bits, elemClass) else
    new IntHashSet (bits, elemClass)
}
