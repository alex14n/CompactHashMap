import org.junit._
import org.junit.Assert._

class CompactHashMapTest {

  @Test def test0 {
    val map = CompactHashMap (classOf[Boolean], classOf[String])
    assertEquals (map.size, 0)

    assertEquals (map.getOrElse(true, "else"), "else")
    map update (true, "true")
    assertEquals (map.size, 1)
    assertEquals (map.getOrElse(true, "else"), "true")
    map update (true, "1")
    assertEquals (map.size, 1)
    assertEquals (map.getOrElse(true, "else"), "1")

    assertEquals (map.getOrElse(false, "else"), "else")
    map update (false, "false")
    assertEquals (map.size, 2)
    assertEquals (map.getOrElse(false, "else"), "false")

    map.clear
    assertEquals (map.size, 0)
    assertEquals (map.get(true), None)
    assertEquals (map.get(false), None)

    map update (true, "2")
    assertEquals (map.size, 1)
    assertEquals (map.get(true), Some("2"))
  }

  @Test def testNull {
    val map = new CompactHashMap [String,String] ()
    assertTrue (map.getOrElse(null, "else") == "else")
    map update (null, "null")
    assertTrue (map.getOrElse(null, "else") == "null")
  }

  @Test def testUntyped1 {
    val map = new CompactHashMap [Boolean,String] ()
    assertEquals (map.size, 0)

    assertEquals (map.getOrElse(true, "else"), "else")
    map update (true, "true")
    assertEquals (map.size, 1)
    assertEquals (map.getOrElse(true, "else"), "true")
    map update (true, "1")
    assertEquals (map.size, 1)
    assertEquals (map.getOrElse(true, "else"), "1")

    assertEquals (map.getOrElse(false, "else"), "else")
    map update (false, "false")
    assertEquals (map.size, 2)
    assertEquals (map.getOrElse(false, "else"), "false")
  }

  @Test def test1 {
    val map = CompactHashMap (classOf[Int], classOf[Int])
    for (i <- -20 to 200) {
      for (j <- -20 until i) {
        assertTrue (map.contains(j))
        assertEquals (map(j), j)
      }
      assertFalse (map.contains(i))
      val size0 = map.size
      map update (i, i)
      assertEquals (map.size, size0+1)
      assertTrue (map.contains(i))
      assertEquals (map(i), i)
      for (j <- i+1 to 220) assertFalse (map.contains(j))
    }
  }

  @Test def test3 {
    val map = CompactHashMap (classOf[Int], classOf[String])
    for (i <- 0 to 100) {
      for (j <- 0 until i) {
        assertTrue (map.contains(j*3))
        assertEquals (map(j*3), j.toString)
      }
      assertFalse (map.contains(i*3))
      map update (i*3, i.toString)
      assertTrue (map.contains(i*3))
      assertEquals (map(i*3), i.toString)
      for (j <- i+1 to 100) assertFalse (map.contains(j*3))
    }
  }

  @Test def testElements {
    val map = CompactHashMap (classOf[Int], classOf[Int])
    assertEquals (map.elements.toList, Nil)
    map.update(1,14)
    assertEquals (map.elements.toList, List(1 -> 14))
    map.update(2,11)
    map.update(4,44)
    assertEquals (map.elements.toList, List(1 -> 14, 2 -> 11, 4 -> 44))

    map -= 2
    assertEquals (map.elements.toList, List(1 -> 14, 4 -> 44))

    map += 10 -> 100
    assertEquals (map.elements.toList, List(1 -> 14, 10 -> 100, 4 -> 44))

    map += 20 -> 200
    assertEquals (map.elements.toList, List(1 -> 14, 10 -> 100, 4 -> 44, 20 -> 200))

    map -= 10
    map -= 4
    map += 5 -> 1
    map += 7 -> 2
    assertEquals (map.elements.toList, List(1 -> 14, 7 -> 2, 5 -> 1, 20 -> 200))
  }

  @Test def testObject {
    val map1 = CompactHashMap (1.4D -> 14)
    assertEquals (map1.toList, List(1.4D -> 14))
    val keys1 = map1.keySet.asInstanceOf[FixedHashSet[Double]]
    assertEquals (classOf[scala.runtime.BoxedDoubleArray], keys1.getArray.getClass)

    val map2 = CompactHashMap (1 -> 10, 2 -> 100, 4 -> -1)
    assertEquals (map2.toList, List(1 -> 10, 2 -> 100, 4 -> -1))
    val keys2 = map2.keySet.asInstanceOf[FixedHashSet[Int]]
    assertEquals (classOf[scala.runtime.BoxedIntArray], keys2.getArray.getClass)
  }

  @Test def testFilter {
    val map = CompactHashMap[Int,Int]
    for (i <- 0 to 300) map update (i, i/4)
    val map1 = map.filter {x => (x._1 & 3) == 0 && x._2 < 50}
    assertEquals ((0 until 50).map(x => (x*4,x)).toList, map1.toList)
    val map2 = map.filter {x => false}
    assertEquals (List(), map2.toList)
  }
}
