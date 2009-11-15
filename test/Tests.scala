import java.util._
import java.util.Map.Entry
import java.lang.reflect.Method
import junit.framework._
import com.google.common.collect.testing._
import com.google.common.collect.testing.features._

// JUnit3 suite to run all tests

object Tests {
  def suppressForAnything: Collection[Method] = Collections.emptySet()
  def suite: Test = {
    val suite = new TestSuite()
    // Scala collections
    suite.addTest(new JUnit4TestAdapter(classOf[CompactHashSetTest]))
    suite.addTest(new JUnit4TestAdapter(classOf[CompactHashMapTest]))
    // Java collections
    suite.addTest(new JUnit4TestAdapter(classOf[FastHashMapTest]))
    suite.addTest(new JUnit4TestAdapter(classOf[FastHashSetTest]))
    suite.addTest(new JUnit4TestAdapter(classOf[FastLinkedHashMapTest]))
    suite.addTest(new JUnit4TestAdapter(classOf[FastLinkedHashSetTest]))
    suite.addTest(new JUnit4TestAdapter(classOf[FastHashMap2Test]))
    // Google tests
    suite.addTest(MapTestSuiteBuilder
        .using(new TestStringMapGenerator() {
            override def create(entries: Array[Entry[String,String]]): Map[String,String] = {
              val map = new FastHashMap[String,String]
              for(e <- entries) map.put(e.getKey, e.getValue)
              map
          }})
        .named("FastHashMap")
        .withFeatures(
            MapFeature.GENERAL_PURPOSE,
            MapFeature.ALLOWS_NULL_KEYS,
            MapFeature.ALLOWS_NULL_VALUES,
            CollectionFeature.KNOWN_ORDER,
            CollectionSize.ANY)
        .suppressing(suppressForAnything)
        .createTestSuite());
    suite.addTest(MapTestSuiteBuilder
        .using(new TestStringMapGenerator() {
            override def create(entries: Array[Entry[String,String]]): Map[String,String] = {
              val map = new FastLinkedHashMap[String,String]
              for(e <- entries) map.put(e.getKey, e.getValue)
              map
          }})
        .named("FastLinkedHashMap")
        .withFeatures(
            MapFeature.GENERAL_PURPOSE,
            MapFeature.ALLOWS_NULL_KEYS,
            MapFeature.ALLOWS_NULL_VALUES,
            CollectionFeature.KNOWN_ORDER,
            CollectionSize.ANY)
        .suppressing(suppressForAnything)
        .createTestSuite());
    suite.addTest(SetTestSuiteBuilder
        .using(new TestStringSetGenerator() {
            override def create(entries: Array[String]): Set[String] =
              new FastHashSet[String](MinimalCollection.of(entries: _*))
          })
        .named("FastHashSet")
        .withFeatures(
            SetFeature.GENERAL_PURPOSE,
            CollectionFeature.ALLOWS_NULL_VALUES,
            CollectionFeature.KNOWN_ORDER,
            CollectionSize.ANY)
        .suppressing(suppressForAnything)
        .createTestSuite());
    suite.addTest(SetTestSuiteBuilder
        .using(new TestStringSetGenerator() {
            override def create(entries: Array[String]): Set[String] =
              new FastLinkedHashSet[String](MinimalCollection.of(entries: _*))
          })
        .named("FastLinkedHashSet")
        .withFeatures(
            SetFeature.GENERAL_PURPOSE,
            CollectionFeature.ALLOWS_NULL_VALUES,
            CollectionFeature.KNOWN_ORDER,
            CollectionSize.ANY)
        .suppressing(suppressForAnything)
        .createTestSuite());
    suite.addTest(MapTestSuiteBuilder
        .using(new TestStringMapGenerator() {
            override def create(entries: Array[Entry[String,String]]): Map[String,String] = {
              val map = new FastHashMap2[String,String]
              for(e <- entries) map.put(e.getKey, e.getValue)
              map
          }})
        .named("FastHashMap2")
        .withFeatures(
            MapFeature.GENERAL_PURPOSE,
            MapFeature.ALLOWS_NULL_KEYS,
            MapFeature.ALLOWS_NULL_VALUES,
            CollectionSize.ANY)
        .suppressing(suppressForAnything)
        .createTestSuite());
    //
    suite
  }
}
