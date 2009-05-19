final class HashCached (final val i: Int) {
  final override def hashCode = i
  final override def equals (other: Any) = other match {
    case that: HashCached => this.i == that.i
    case _ => false
  }
}

object Benchmark {
  private[this] val iterations = 0x170000 // 1500000

  type T = String
  private[this] val values = (0 to iterations*2).toList map { x => "_test_"+x } toArray
/*
  type T = HashCached
  private[this] val values = (0 to iterations*2).toList map { x => new HashCached(100000+x*123) } toArray

  type T = Int
  private[this] val values = (0 to iterations*2).toList.toArray
*/
  private[this] var compactMap: CompactHashMap[T,T] = _ // CompactHashMap (classOf[T], classOf[T], iterations)
  private[this] var scalaMap: scala.collection.mutable.HashMap[T,T] = _
  private[this] var javaMap: java.util.HashMap[T,T] = _ // new java.util.HashMap (iterations)
  private[this] var fastMap: FastHashMap[T,T] = _
  private[this] var troveMap: gnu.trove.THashMap[T,T] = _
  private[this] var troveIntMap: gnu.trove.TIntIntHashMap = _

  private[this] val rt = Runtime.getRuntime()

  def doTest (name: String, proc: () => Unit) {
    val t0 = System.currentTimeMillis
    proc ()
    val t = System.currentTimeMillis - t0
    System.gc
    println (name + " - " + (t / 1000.).formatted("%.3f") + "; Mem: " +
      (((rt.totalMemory() - rt.freeMemory()) / 1024. / 1024.) formatted "%.2f") + " Mb")
  }

  def compactWrite {
    compactMap = new CompactHashMap // (classOf[T], classOf[T], iterations)
    var i = 0
    while (i < iterations) {
      compactMap update (values(i), values(2*iterations-i))
      i += 1
    }
  }

  def scalaWrite {
    scalaMap = new scala.collection.mutable.HashMap
    var i = 0
    while (i < iterations) {
      scalaMap update (values(i), values(2*iterations-i))
      i += 1
    }
  }

  def javaWrite {
    javaMap = new java.util.HashMap // (iterations)
    var i = 0
    while (i < iterations) {
      javaMap put (values(i), values(2*iterations-i))
      i += 1
    }
  }

  def fastWrite {
    fastMap = new FastHashMap // (iterations)
    var i = 0
    while (i < iterations) {
      fastMap put (values(i), values(2*iterations-i))
      i += 1
    }
  }

  def troveWrite {
    troveMap = new gnu.trove.THashMap[T,T] // (iterations)
    var i = 0
    while (i < iterations) {
      troveMap put (values(i), values(2*iterations-i))
      i += 1
    }
  }
/*
  def troveIntWrite {
    troveIntMap = new gnu.trove.TIntIntHashMap // (iterations)
    var i = 0
    while (i < iterations) {
      troveIntMap put (values(i), values(2*iterations-i))
      i += 1
    }
  }
*/
  final def reoder(i: Int): Int = (i & 0xFFFF0000) | ((i & 0xFF00) >>> 8) | ((i & 0xFF) << 8)

  def compactReadFull {
    var i = 0
    while (i < iterations) {
      val j = reoder(i)
      assert (compactMap(values(j)) == values(2*iterations-j))
      i += 1
    }
  }

  def compactReadEmpty {
    var i = iterations
    while (i < 2*iterations) {
      assert (! compactMap.contains(values(i)))
      i += 1
    }
    compactMap = null
  }

  def scalaReadFull {
    var i = 0
    while (i < iterations) {
      val j = reoder(i)
      assert (scalaMap(values(j)) == values(2*iterations-j))
      i += 1
    }
  }

  def scalaReadEmpty {
    var i = iterations
    while (i < 2*iterations) {
      assert (! scalaMap.contains(values(i)))
      i += 1
    }
    scalaMap = null
  }

  def javaReadFull {
    var i = 0
    while (i < iterations) {
      val j = reoder(i)
      assert (javaMap.get(values(j)) == values(2*iterations-j))
      i += 1
    }
  }

  def javaReadEmpty {
    var i = iterations
    while (i < 2*iterations) {
      assert (! javaMap.containsKey(values(i)))
      i += 1
    }
    javaMap = null
  }

  def fastReadFull {
    var i = 0
    while (i < iterations) {
      val j = reoder(i)
      assert (fastMap.get(values(j)) == values(2*iterations-j))
      i += 1
    }
  }

  def fastReadEmpty {
    var i = iterations
    while (i < 2*iterations) {
      assert (! fastMap.containsKey(values(i)))
      i += 1
    }
    fastMap = null
  }

  def troveReadFull {
    var i = 0
    while (i < iterations) {
      val j = reoder(i)
      assert (troveMap.get(values(j)) == values(2*iterations-j))
      i += 1
    }
  }

  def troveReadEmpty {
    var i = iterations
    while (i < 2*iterations) {
      assert (! troveMap.containsKey(values(i)))
      i += 1
    }
    troveMap = null
  }
/*
  def troveIntReadFull {
    var i = 0
    while (i < iterations) {
      val j = reoder(i)
      assert (troveIntMap.get(values(j)) == values(2*iterations-j))
      i += 1
    }
  }

  def troveIntReadEmpty {
    var i = iterations
    while (i < 2*iterations) {
      assert (! troveIntMap.containsKey(values(i)))
      i += 1
    }
    troveIntMap = null
  }

  def oldFilter {
    CompactHashMap (classOf[Int], classOf[Int]) ++
      compactMap.elements.filter {x => (x._1 & 3) == 0 && x._2 < 500000}
  }

  def newFilter {
    compactMap.filter {x => (x._1 & 3) == 0 && x._2 < 500000}
  }
*/
  val tests: List[(String,()=>Unit)] = List(
    "fastWrite" -> fastWrite _,
    "fastReadFull" -> fastReadFull _,
    "fastReadEmpty" -> fastReadEmpty _,
    "javaWrite" -> javaWrite _,
    "javaReadFull" -> javaReadFull _,
    "javaReadEmpty" -> javaReadEmpty _,
/*
    "troveWrite" -> troveWrite _,
    "troveReadFull" -> troveReadFull _,
    "troveReadEmpty" -> troveReadEmpty _,
    "troveIntWrite" -> troveIntWrite _,
    "troveIntReadFull" -> troveIntReadFull _,
    "troveIntReadEmpty" -> troveIntReadEmpty _,
    "compactWrite" -> compactWrite _,
    "compactReadFull" -> compactReadFull _,
    "compactReadEmpty" -> compactReadEmpty _,
    "scalaWrite" -> scalaWrite _,
    "scalaReadFull" -> scalaReadFull _,
    "scalaReadEmpty" -> scalaReadEmpty _,
    "oldFilter" -> oldFilter _,
    "newFilter" -> newFilter _,
*/
  )

  def main (args: Array[String]) {
    while (true) {
      for ((name, proc) <- tests) {
        doTest (name, proc)
      }
    }
  }
}
