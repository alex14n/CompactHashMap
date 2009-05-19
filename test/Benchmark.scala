final class HashCached (final val i: Int) {
  final override def hashCode = i
  final override def equals (other: Any) = other match {
    case that: HashCached => this.i == that.i
    case _ => false
  }
}

object Benchmark {
  private[this] val iterations = 1500000

  type T = String
  private[this] val values = (1 to iterations*2).toList map { x => "_test_"+x } toArray
/*
  type T = HashCached
  private[this] val values = (1 to iterations*2).toList map { x => new HashCached(100000+x*123) } toArray

  type T = Int
  private[this] val values = (1 to iterations*2).toList.toArray
*/
  private[this] var compactMap: CompactHashMap[T,T] = _ // CompactHashMap (classOf[T], classOf[T], iterations)
  private[this] var scalaMap: scala.collection.mutable.HashMap[T,T] = _
  private[this] var javaMap: java.util.HashMap[T,T] = _ // new java.util.HashMap (iterations)
  private[this] var fastMap: FastHashMap[T,T] = _
  private[this] var troveMap: gnu.trove.THashMap[T,T] = _

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
    compactMap update (null.asInstanceOf[T], null.asInstanceOf[T])
    var i = 1
    while (i < iterations) {
      compactMap update (values(i), values(2*iterations-i))
      i += 1
    }
  }

  def scalaWrite {
    scalaMap = new scala.collection.mutable.HashMap
    var i = 1
    while (i < iterations) {
      scalaMap update (values(i), values(2*iterations-i))
      i += 1
    }
  }

  def javaWrite {
    javaMap = new java.util.HashMap // (iterations)
    var i = 1
    while (i < iterations) {
      javaMap put (values(i), values(2*iterations-i))
      i += 1
    }
  }

  def fastWrite {
    fastMap = new FastHashMap // (iterations)
    var i = 1
    while (i < iterations) {
      fastMap put (values(i), values(2*iterations-i))
      i += 1
    }
  }

  def troveWrite {
    troveMap = new gnu.trove.THashMap[T,T] // (iterations)
    var i = 1
    while (i < iterations) {
      troveMap put (values(i), values(2*iterations-i))
      i += 1
    }
  }

  def compactReadFull {
    var i = 1
    assert (compactMap(null.asInstanceOf[T]) == null.asInstanceOf[T])
    while (i < iterations) {
      assert (compactMap(values(i)) == values(2*iterations-i))
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
    var i = 1
    while (i < iterations) {
      assert (scalaMap(values(i)) == values(2*iterations-i))
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
    var i = 1
    while (i < iterations) {
      assert (javaMap.get(values(i)) == values(2*iterations-i))
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
    var i = 1
    while (i < iterations) {
      assert (fastMap.get(values(i)) == values(2*iterations-i))
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
    var i = 1
    while (i < iterations) {
      assert (troveMap.get(values(i)) == values(2*iterations-i))
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
