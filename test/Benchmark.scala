final class Pos(final val x:Int, final val y:Int) {
  final override def hashCode = x ^ (y << 16) ^ (y >>> 16)
  final override def equals (other: Any) = other match {
    case that: Pos => this.x == that.x && this.y == that.y
    case _ => false
  }
}

final class BadHash {
  override def hashCode = super.hashCode & 0xFFFFF
}

object Benchmark {
  final val rnd = new java.util.Random
  final case class Result (var count: Int, var total: Long, var min: Long, var minMem: Long)
  final private[this] val results = CompactHashMap[String,Result]

  private[this] val iterations = 0x180000
  private[this] val tries = 1
  private[this] val maps = new Array[java.util.Map[T,T]](tries)
  final def intKey(i: Int) = i*123
/*
  type T = Pos
  private[this] val values = (0 to iterations*2).toList map { x => new Pos(rnd.nextInt, rnd.nextInt) } toArray

  type T = BadHash
  private[this] val values = (0 to iterations*2).toList map { x => new BadHash } toArray
*/
  type T = Object
  private[this] val values = (0 to iterations*2).toList map { x => new Object } toArray
/*
  type T = String
  // private[this] val values = (0 to iterations*2).toList map { x => "_test_"+x } toArray
  final def values(x: Int) = "_test_"+x

  type T = Int
  final def values(i: Int) = intKey(i)
*/
  private[this] var scalaMap: scala.collection.mutable.Map[T,T] = _
  private[this] var compactMap: CompactHashMap[T,T] = _
  private[this] var javaMap: java.util.Map[T,T] = _
/*
  private[this] var troveIntMap: gnu.trove.TIntIntHashMap = _
  private[this] var fastutilIntMap: it.unimi.dsi.fastutil.ints.Int2IntMap = _
*/
  final private[this] val rt = Runtime.getRuntime()

  // final def reoder(i: Int): Int = (i & 0xFFFF0000) | ((i & 0xFF00) >>> 8) | ((i & 0xFF) << 8)
  final val reorder = new Array[Int](iterations)
  for (i <- 1 until iterations) {
    val j = i + rnd.nextInt(iterations-i)
    val a = reorder(i)
    val b = reorder(j)
    reorder(j) = if (a > 0) a else i
    reorder(i) = if (b > 0) b else j
  }

  def doTest (name: String, proc: () => Unit) {
    val t0 = System.currentTimeMillis
    proc ()
    val t = System.currentTimeMillis - t0
    System.gc
    val mem = rt.totalMemory() - rt.freeMemory()
    val result = results.getOrElse (name, {
      val r = Result(0, 0L, t, mem)
      results(name) = r
      r } )
    result.count += 1
    result.total += t
    if (t < result.min) result.min = t
    if (mem < result.minMem) result.minMem = mem
    println (name + " - " + (t / 1000.).formatted("%.3f") + "s; Mem: " +
      ((mem / 1048576.) formatted "%.2f") + " Mb; avg " +
      (result.total / 1000. / result.count).formatted("%.3f") + "s; min " +
      (result.min / 1000.).formatted("%.3f") + "s; minMem " +
      ((result.minMem / 1048576.) formatted "%.2f") + " Mb")
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
    compactMap = new CompactHashMap (classOf[T], classOf[T], 16, 0.75f)
    var i = 0
    while (i < iterations) {
      compactMap updateIntInt (intKey(i), intKey(2*iterations-i))
      i += 1
    }
  }

  def compactWrite {
    var t = 0
    while (t < tries) {
      scalaMap = new CompactHashMap (classOf[T], classOf[T])
      var i = 0
      while (i < iterations) {
        scalaMap (values(i)) = values(2*iterations-i)
        i += 1
      }
      t += 1
    }
  }

  def scalaReadFull {
    var t = 0
    while (t < tries) {
      var i = 0
      while (i < iterations) {
        val j = reorder(i)
        assert (scalaMap(values(j)) == values(2*iterations-j))
        i += 1
      }
      t += 1
    }
  }

  def scalaReadEmpty {
    var t = 0
    while (t < tries) {
      var i = 0
      while (i < iterations) {
        val j = iterations+reorder(i)
        assert (! scalaMap.contains(values(j)))
        i += 1
      }
      t += 1
    }
    scalaMap = null
  }

  def compactIntReadFull {
    var i = 0
    while (i < iterations) {
      val j = reorder(i)
      assert (compactMap.applyIntInt(intKey(j)) == intKey(2*iterations-j))
      i += 1
    }
  }

  def compactIntReadEmpty {
    var i = iterations
    while (i < 2*iterations) {
      assert (! compactMap.containsInt(intKey(i)))
      i += 1
    }
    compactMap = null
  }

  def javaWrite {
    var map: java.util.Map[T,T] = null
    var i = 0
    while (i < iterations) {
      val key = values(i)
      val value = values(2*iterations-i)
      var t = 0
      while (t < tries) {
        if (i == 0) {
          map = new java.util.HashMap[T,T]
          maps(t) = map
        } else {
          map = maps(t)
        }
        map put (key, value)
        t += 1
      }
      i += 1
    }
  }

  def fastWrite {
    var map: java.util.Map[T,T] = null
    var i = 0
    while (i < iterations) {
      val key = values(i)
      val value = values(2*iterations-i)
      var t = 0
      while (t < tries) {
        if (i == 0) {
          map = new FastHashMap[T,T]
          maps(t) = map
        } else {
          map = maps(t)
        }
        map put (key, value)
        t += 1
      }
      i += 1
    }
  }

  def fast2Write {
    var map: java.util.Map[T,T] = null
    var i = 0
    while (i < iterations) {
      val key = values(i)
      val value = values(2*iterations-i)
      var t = 0
      while (t < tries) {
        if (i == 0) {
          map = new FastHashMap2[T,T]
          maps(t) = map
        } else {
          map = maps(t)
        }
        map put (key, value)
        t += 1
      }
      i += 1
    }
  }

  def javaReadFull {
    var i = 0
    while (i < iterations) {
      val j = reorder(i)
      val key = values(j)
      val value = values(2*iterations-j)
      var t = 0
      while (t < tries) {
        val map = maps(t)
        assert (map.get(key) == value)
        t += 1
      }
      i += 1
    }
  }

  def javaReadEmpty {
    var i = 0
    while (i < iterations) {
      val j = iterations+reorder(i)
      val key = values(j)
      var t = 0
      while (t < tries) {
        val map = maps(t)
        assert (! map.containsKey(key))
        if (i == iterations-1) maps(t) = null
        t += 1
      }
      i += 1
    }
  }
/*
  def troveIntWrite {
    troveIntMap = new gnu.trove.TIntIntHashMap (12, 0.5f)
    var i = 0
    while (i < iterations) {
      troveIntMap put (intKey(i), intKey(2*iterations-i))
      i += 1
    }
  }

  def fastutilIntWrite {
    fastutilIntMap = new it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap (24, 0.6f)
    var i = 0
    while (i < iterations) {
      fastutilIntMap put (intKey(i), intKey(2*iterations-i))
      i += 1
    }
  }

  def troveIntReadFull {
    var i = 0
    while (i < iterations) {
      val j = reorder(i)
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
      val j = reorder(i)
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
*/
  val tests: List[(String,()=>Unit)] = List(
    "fastWrite       " -> fastWrite _,
    "fastReadFull    " -> javaReadFull _,
    "fastReadEmpty   " -> javaReadEmpty _,
    "fast2Write      " -> fast2Write _,
    "fast2ReadFull   " -> javaReadFull _,
    "fast2ReadEmpty  " -> javaReadEmpty _,
    "javaWrite       " -> javaWrite _,
    "javaReadFull    " -> javaReadFull _,
    "javaReadEmpty   " -> javaReadEmpty _,
/*
    "compactWrite    " -> compactWrite _,
    "compactReadFull " -> scalaReadFull _,
    "compactReadEmpty" -> scalaReadEmpty _,
    "javolutionWrite" -> {() => javaWrite(new javolution.util.FastMap)},
    "javolutionReadFull" -> javaReadFull _,
    "javolutionReadEmpty" -> javaReadEmpty _,
    "scalaWrite" -> {() => scalaWrite (new scala.collection.mutable.HashMap)},
    "scalaReadFull" -> scalaReadFull _,
    "scalaReadEmpty" -> scalaReadEmpty _,
    "compactIntWrite" -> compactIntWrite _,
    "compactIntReadFull" -> compactIntReadFull _,
    "compactIntReadEmpty" -> compactIntReadEmpty _,
    "fastutilIntWrite" -> fastutilIntWrite _,
    "fastutilIntReadFull" -> fastutilIntReadFull _,
    "fastutilIntReadEmpty" -> fastutilIntReadEmpty _,
    "troveIntWrite" -> troveIntWrite _,
    "troveIntReadFull" -> troveIntReadFull _,
    "troveIntReadEmpty" -> troveIntReadEmpty _,
    "troveWrite" -> {() => javaWrite(new gnu.trove.THashMap)},
    "troveReadFull" -> javaReadFull _,
    "troveReadEmpty" -> javaReadEmpty _,
    "fastutilWrite" -> {() => javaWrite(new it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap)},
    "fastutilReadFull" -> javaReadFull _,
    "fastutilReadEmpty" -> javaReadEmpty _,
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
