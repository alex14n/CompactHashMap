import org.junit._
import org.junit.Assert._

class CompactHashMapTest {

  @Test def test0 {
    val map = CompactHashMap (classOf[Boolean], classOf[String])
    assertEquals (0, map.size)

    assertEquals ("else", map.getOrElse(true, "else"))
    map update (true, "true")
    assertEquals (1, map.size)
    assertEquals ("true", map.getOrElse(true, "else"))
    map update (true, "1")
    assertEquals (1, map.size)
    assertEquals ("1", map.getOrElse(true, "else"))

    assertEquals ("else", map.getOrElse(false, "else"))
    map update (false, "false")
    assertEquals (2, map.size)
    assertEquals ("false", map.getOrElse(false, "else"))

    map.clear
    assertEquals (0, map.size)
    assertEquals (None, map.get(true))
    assertEquals (None, map.get(false))

    map update (true, "2")
    assertEquals (1, map.size)
    assertEquals (Some("2"), map.get(true))
  }

  @Test def testNull {
    val map = new CompactHashMap [String,String] ()
    assertEquals ("else", map.getOrElse(null, "else"))
    map update (null, "null")
    assertEquals ("null", map.getOrElse(null, "else"))

    assertEquals ("else", map.getOrElse("null", "else"))
    map update ("null", null)
    assertEquals (null, map.getOrElse("null", "else"))
  }

  @Test def testUntyped1 {
    val map = new CompactHashMap [Boolean,String] ()
    assertEquals (0, map.size)

    assertEquals ("else", map.getOrElse(true, "else"))
    map update (true, "true")
    assertEquals (1, map.size)
    assertEquals ("true", map.getOrElse(true, "else"))
    map update (true, "1")
    assertEquals (1, map.size)
    assertEquals ("1", map.getOrElse(true, "else"))

    assertEquals ("else", map.getOrElse(false, "else"))
    map update (false, "false")
    assertEquals (2, map.size)
    assertEquals ("false", map.getOrElse(false, "else"))
  }

  @Test def test1 {
    val map = CompactHashMap (classOf[Int], classOf[Int])
    for (i <- -20 to 200) {
      for (j <- -20 until i) {
        assertTrue (map.contains(j))
        assertEquals (j, map(j))
      }
      assertFalse (map.contains(i))
      val size0 = map.size
      map update (i, i)
      assertEquals (size0+1, map.size)
      assertTrue (map.contains(i))
      assertEquals (i, map(i))
      for (j <- i+1 to 220) assertFalse (map.contains(j))
    }
  }

  @Test def test3 {
    val map = CompactHashMap (classOf[Int], classOf[String])
    for (i <- 0 to 100) {
      for (j <- 0 until i) {
        assertTrue (map.contains(j*3))
        assertEquals (j.toString, map(j*3))
      }
      assertFalse (map.contains(i*3))
      map update (i*3, i.toString)
      assertTrue (map.contains(i*3))
      assertEquals (i.toString, map(i*3))
      for (j <- i+1 to 100) assertFalse (map.contains(j*3))
    }
  }

  @Test def testElements {
    val map = CompactHashMap (classOf[Int], classOf[Int])
    assertEquals (Nil, map.elements.toList)
    map.update(1,14)
    assertEquals (List(1 -> 14), map.elements.toList)
    map.update(2,11)
    map.update(4,44)
    assertEquals (List(1 -> 14, 2 -> 11, 4 -> 44), map.elements.toList)

    map -= 2
    assertEquals (List(1 -> 14, 4 -> 44), map.elements.toList)

    map += 10 -> 100
    assertEquals (List(1 -> 14, 10 -> 100, 4 -> 44), map.elements.toList)

    map += 20 -> 200
    assertEquals (List(1 -> 14, 10 -> 100, 4 -> 44, 20 -> 200), map.elements.toList)

    map -= 10
    map -= 4
    map += 5 -> 1
    map += 7 -> 2
    assertEquals (List(1 -> 14, 7 -> 2, 5 -> 1, 20 -> 200), map.elements.toList)
  }

  @Test def testObject {
    val map1 = CompactHashMap (1.4D -> 14)
    assertEquals (List(1.4D -> 14), map1.toList)
    val keys1 = map1.keySet.asInstanceOf[FixedHashSet[Double]]
    assertEquals (classOf[scala.runtime.BoxedDoubleArray], keys1.getArray.getClass)

    val map2 = CompactHashMap (1 -> 10, 2 -> 100, 4 -> -1)
    assertEquals (List(1 -> 10, 2 -> 100, 4 -> -1), map2.toList)
    val keys2 = map2.keySet.asInstanceOf[FixedHashSet[Int]]
    assertEquals (classOf[scala.runtime.BoxedIntArray], keys2.getArray.getClass)
  }

  @Test def testFilter {
    val map = CompactHashMap[Int,Int]
    for (i <- 0 to 300) map update (i, i/4)
    val map1 = map.filter {x => (x._1 & 3) == 0 && x._2 < 50}
    assertEquals ((0 until 50).map(x => (x*4,x)).toList, map1.toList)
    val map2 = map.filter {x => false}
    assertEquals (Nil, map2.toList)
  }
}
