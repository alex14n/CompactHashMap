import org.junit._
import org.junit.Assert._

class CompactHashSetTest {

  @Test def testInt {
    val set = new CompactHashSet(classOf[Int])
    for (i <- -20 to 200) {
      for (j <- -20 until i) assertTrue (set.contains(j))
      assertFalse (set.contains(i))
      val size0 = set.size
      set += i
      assertEquals (size0+1, set.size)
      assertTrue (set.contains(i))
      for (j <- i+1 to 200) assertFalse (set.contains(j))
    }
  }

  @Test def testNull {
    val set = new CompactHashSet[String]()
    assertEquals (0, set.size)

    assertFalse (set.contains("test"))
    set += "test"
    assertTrue (set.contains("test"))

    assertFalse (set.contains(null))
    set += null
    assertTrue (set.contains(null))
  }

  @Test def testElements {
    val set = new CompactHashSet(classOf[Int])
    assertEquals (Nil, set.elements.toList)
    set += 1
    assertEquals (List(1), set.elements.toList)
    set += 2
    set += 3
    val set2 = Set() ++ set.elements
    assertEquals (Set(1,2,3), set2)
    set += 0
    val set3 = Set() ++ set.elements
    assertEquals (Set(0,1,2,3), set3)
  }

  @Test def testFixedDel {
    val set = FixedHashSet (2, classOf[Int])
    for (i <- 100000 to 100003) set add i

    assertEquals ( 0, set.positionOf(100000))
    assertEquals ( 1, set.positionOf(100001))
    assertEquals ( 2, set.positionOf(100002))
    assertEquals ( 3, set.positionOf(100003))

    assertFalse (set.isEmpty(0))
    assertFalse (set.isEmpty(1))
    assertFalse (set.isEmpty(2))
    assertFalse (set.isEmpty(3))

    assertEquals ( 2, set.delete (100002))
    assertEquals ( 3, set.size)
    // assertEquals(4, set.firstEmptyIndex)
    // assertEquals(2, set.firstDeletedIndex)

    assertEquals ( 0, set.positionOf(100000))
    assertEquals ( 1, set.positionOf(100001))
    assertEquals (-1, set.positionOf(100002))
    assertEquals ( 3, set.positionOf(100003))

    assertEquals ( 1, set.delete (100001))
    assertEquals ( 2, set.size)
    // assertEquals(2, set.firstDeletedIndex)

    assertEquals ( 0, set.positionOf(100000))
    assertEquals (-1, set.positionOf(100001))
    assertEquals (-1, set.positionOf(100002))
    assertEquals ( 3, set.positionOf(100003))

    assertEquals ( 0, set.delete (100000))
    assertEquals ( 1, set.size)
    // assertEquals(2, set.firstDeletedIndex)

    assertEquals (-1, set.positionOf(100000))
    assertEquals (-1, set.positionOf(100001))
    assertEquals (-1, set.positionOf(100002))
    assertEquals ( 3, set.positionOf(100003))

    assertEquals ( 3, set.delete (100003))
    assertEquals ( 0, set.size)
    // assertEquals(3, set.firstEmptyIndex)
    // assertEquals(2, set.firstDeletedIndex)

    assertTrue (set.isEmpty(0))
    assertTrue (set.isEmpty(1))
    assertTrue (set.isEmpty(2))
    assertTrue (set.isEmpty(3))

    assertEquals (Nil, set.elements.toList)

    assertEquals ( 2, set.add(1000010))
    // assertEquals ( 0, set.firstDeletedIndex)
    assertEquals ( 0, set.add(1000011))
    // assertEquals ( 1, set.firstDeletedIndex)
    assertEquals ( 1, set.add(1000012))
    // assertEquals (-1, set.firstDeletedIndex)
    assertEquals ( 3, set.add(1000013))

    val set2 = Set() ++ set.elements
    assertEquals (Set(1000010,1000011,1000012,1000013), set2)

    set.delete (1000011)
    // assertEquals ( 0, set.firstDeletedIndex)
    set.delete (1000012)
    // assertEquals ( 1, set.firstDeletedIndex)
    val set3 = Set() ++ set.elements
    assertEquals (Set(1000010,1000013), set3)
    assertEquals ( 1, set.add(1000012))
    assertEquals ( 0, set.add(1000011))
  }

  @Test def testFixedDel2 {
    val set = FixedHashSet (2, classOf[Int])
    for (i <- 0 to 3) set.add (i)
    set.clear
    for (i <- 11 to 14) set.add (i)
    assertEquals (List(11,12,13,14), set.toList)
  }

  @Test def testFixedDel3 {
    val set = FixedHashSet (2, classOf[Int])
    for (i <- 1 to 4) set.add (i)
    for (i <- 4 until(0,-1)) set.delete (i)
    assertEquals (0, set.size)
    for (i <- 11 to 14) set.add (i)
    assertEquals (List(11,12,13,14), set.toList)
  }

  @Test def testObject {
    val set1 = CompactHashSet (1)
    assertEquals (1, set1.size)
    val set2 = CompactHashSet (1, 2, 3, 4)
    assertEquals (List(1,2,3,4), set2.toList)
  }

  @Test def testFilter {
    val set = FixedHashSet (8, classOf[Int])
    for (i <- 0 to 200) set add i
    val set1 = set.filter ((x,i) => (x&3) == 0, null, null)
    assertEquals (6, set1.bits)
    assertEquals ((0 until (201,4)).toList, set1.toList)
  }

  @Test def testClear {
    val elements = List ("1", "2", "test", "14", "0", null, "77")
    val set = FixedHashSet (4, classOf[String])
    elements foreach { x => set addNew x }
    assertEquals (elements, set.toList)
    set.clear
    assertTrue (set.isEmpty)
    set.getArray foreach { x => assertEquals (null, x) }
  }

  @Test def testToList {
    val list1 = 1 to 1000 toList
    val set = CompactHashSet (list1: _*)
    val list2 = set.toList
    assertEquals (list1, list2)
  }

  @Test def testClone {
    val set = CompactHashSet[String]
    set += "a"
    set += "b"
    set += "c"
    val setClone = set.clone
    assertTrue (setClone.isInstanceOf[CompactHashSet[_]])

    set -= "b"
    assertEquals(3, setClone.size)
    set += "d"
    assertTrue(setClone.contains("b"))

    assertEquals(List("a", "b", "c"), setClone.toList)
    assertEquals(List("a", "d", "c"), set.toList)
  }
}
