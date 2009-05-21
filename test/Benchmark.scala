final class HashCached (final val i: Int) {
  final override def hashCode = i
  final override def equals (other: Any) = other match {
    case that: HashCached => this.i == that.i
    case _ => false
  }
}

object Benchmark {
  private[this] val iterations = 0x180000
  final def intKey(i: Int) = i*123
/*
  type T = Object
  private[this] val values = (0 to iterations*2).toList map { x => new Object } toArray

  type T = String
  private[this] val values = (0 to iterations*2).toList map { x => "_test_"+x } toArray

  type T = HashCached
  private[this] val values = (0 to iterations*2).toList map { x => new HashCached(100000+x*123) } toArray
*/
  type T = Int
  final def values(i: Int) = intKey(i)

  private[this] var scalaMap: scala.collection.mutable.Map[T,T] = _
  private[this] var compactMap: CompactHashMap[T,T] = _
  private[this] var javaMap: java.util.Map[T,T] = _
  private[this] var troveIntMap: gnu.trove.TIntIntHashMap = _
  private[this] var fastutilIntMap: it.unimi.dsi.fastutil.ints.Int2IntMap = _

  private[this] val rt = Runtime.getRuntime()

  def doTest (name: String, proc: () => Unit) {
    val t0 = System.currentTimeMillis
    proc ()
    val t = System.currentTimeMillis - t0
    System.gc
    println (name + " - " + (t / 1000.).formatted("%.3f") + "s; Mem: " +
      (((rt.totalMemory() - rt.freeMemory()) / 1024. / 1024.) formatted "%.2f") + " Mb")
  }

  def scalaWrite (map: scala.collection.mutable.Map[T,T]) {
    scalaMap = map
    var i = 0
    while (i < iterations) {
      scalaMap (values(i)) = values(2*iterations-i)
      i += 1
    }
  }

  def compactIntWrite () {
    compactMap = new CompactHashMap (classOf[Int], classOf[Int], iterations)
    var i = 0
    while (i < iterations) {
      compactMap updateIntInt (values(i), values(2*iterations-i))
      i += 1
    }
  }

  def javaWrite (map: java.util.Map[T,T]) {
    javaMap = map
    var i = 0
    while (i < iterations) {
      javaMap put (values(i), values(2*iterations-i))
      i += 1
    }
  }

  def troveIntWrite {
    troveIntMap = new gnu.trove.TIntIntHashMap (16, 0.4f)
    var i = 0
    while (i < iterations) {
      troveIntMap put (intKey(i), intKey(2*iterations-i))
      i += 1
    }
  }

  def fastutilIntWrite {
    fastutilIntMap = new it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap (iterations, 0.5f)
    var i = 0
    while (i < iterations) {
      fastutilIntMap put (intKey(i), intKey(2*iterations-i))
      i += 1
    }
  }

  final def reoder(i: Int): Int = (i & 0xFFFF0000) | ((i & 0xFF00) >>> 8) | ((i & 0xFF) << 8)

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

  def compactIntReadFull {
    var i = 0
    while (i < iterations) {
      val j = reoder(i)
      assert (compactMap.applyIntInt(values(j)) == values(2*iterations-j))
      i += 1
    }
  }

  def compactIntReadEmpty {
    var i = iterations
    while (i < 2*iterations) {
      assert (! compactMap.containsInt(values(i)))
      i += 1
    }
    compactMap = null
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

  def troveIntReadFull {
    var i = 0
    while (i < iterations) {
      val j = reoder(i)
      assert (troveIntMap.get(intKey(j)) == intKey(2*iterations-j))
      i += 1
    }
  }

  def troveIntReadEmpty {
    var i = iterations
    while (i < 2*iterations) {
      assert (! troveIntMap.containsKey(intKey(i)))
      i += 1
    }
    troveIntMap = null
  }

  def fastutilIntReadFull {
    var i = 0
    while (i < iterations) {
      val j = reoder(i)
      assert (fastutilIntMap.get(intKey(j)) == intKey(2*iterations-j))
      i += 1
    }
  }

  def fastutilIntReadEmpty {
    var i = iterations
    while (i < 2*iterations) {
      assert (! fastutilIntMap.containsKey(intKey(i)))
      i += 1
    }
    fastutilIntMap = null
  }

  val tests: List[(String,()=>Unit)] = List(
/*
    "fastWrite" -> {() => javaWrite(new FastHashMap)},
    "fastReadFull" -> javaReadFull _,
    "fastReadEmpty" -> javaReadEmpty _,
    "javaWrite" -> {() => javaWrite(new java.util.HashMap)},
    "javaReadFull" -> javaReadFull _,
    "javaReadEmpty" -> javaReadEmpty _,
    "compactWrite" -> {() => scalaWrite (new CompactHashMap)},
    "compactReadFull" -> scalaReadFull _,
    "compactReadEmpty" -> scalaReadEmpty _,
    "fastutilWrite" -> {() => javaWrite(new it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap)},
    "fastutilReadFull" -> javaReadFull _,
    "fastutilReadEmpty" -> javaReadEmpty _,
*/
    "compactIntWrite" -> compactIntWrite _,
    "compactIntReadFull" -> compactIntReadFull _,
    "compactIntReadEmpty" -> compactIntReadEmpty _,
    "fastutilIntWrite" -> fastutilIntWrite _,
    "fastutilIntReadFull" -> fastutilIntReadFull _,
    "fastutilIntReadEmpty" -> fastutilIntReadEmpty _,
/*
    "troveIntWrite" -> troveIntWrite _,
    "troveIntReadFull" -> troveIntReadFull _,
    "troveIntReadEmpty" -> troveIntReadEmpty _,
    "troveWrite" -> {() => javaWrite(new gnu.trove.THashMap)},
    "troveReadFull" -> javaReadFull _,
    "troveReadEmpty" -> javaReadEmpty _,
    "scalaWrite" -> {() => scalaWrite (new scala.collection.mutable.HashMap)},
    "scalaReadFull" -> scalaReadFull _,
    "scalaReadEmpty" -> scalaReadEmpty _,
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
