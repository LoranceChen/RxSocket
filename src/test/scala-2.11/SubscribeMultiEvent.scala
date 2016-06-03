import rx.lang.scala.{Subscriber, Subscription, Observable}
import scala.collection.mutable
import lorance.rxscoket.rxsocketLogger

/**
  * construct loop to Observable stream
  */
object SubscribeMultiEvent extends App {
  val ss = mutable.Set[Subscriber[String]]()

  new Thread {
    rxsocketLogger.log("construct thread")

    override def run() {
      rxsocketLogger.log("new thread")
      printLoop
    }

    start()
  }

  val obv = getObv
  val obv2 = obv.map { x => boo; x }.cache
  val obv3 = obv2.map { x => foo; x }

  def getObv = {
    rxsocketLogger.log("getObv - ")
    val obv = Observable.apply[String] { s =>
      ss.synchronized(ss += s)
      s.add(Subscription(ss.synchronized(ss -= s)))
      //    printForever
    }
    obv
  } //.subscribeOn(NewThreadScheduler())
  //todo `NewThreadScheduler` : what's a means of new thread scheduler? which stage it put the subscribe on new thread?

  obv3.subscribe(s => rxsocketLogger.log(s"first observer1 - $s"))
  obv3.subscribe(s => rxsocketLogger.log(s"first observer2 - $s"))

  def boo = rxsocketLogger.log("boo - ")

  def foo = rxsocketLogger.log("foo - ")

  def printLoop = {
    def printForever: Unit = {
      for (s <- ss) {
        s.onNext("hi~")
      }
      Thread.sleep(1000)
      printForever
    }
    printForever
  }
}

/**
  * map will transfer its inner code - dangerous!!
  */
object ObvMapTest extends App {
  def boo = rxsocketLogger.log("boo - ")

  def foo = rxsocketLogger.log("foo - ")

  val obvTest = Observable[String]{s =>
    s.onNext("hi~")
  }

  val map1 = obvTest.map{o => boo; o}
  val map2 = map1.map{o => foo; o}

  map2.subscribe(s => println(s"observer - $s"))
  map2.subscribe(s => println(s"observer2 - $s"))
  map2.subscribe(s => println(s"observer3 - $s"))
  Thread.currentThread().join()
}

/**
  * amazing publish - its hot Observable
  */
object PublishTest extends App {
  val unshared = Observable[String]{s =>
    def printLoop = {
      def printForever: Unit = {
          s.onNext("hi~")
        Thread.sleep(1000)
        printForever
      }
      printForever
    }
    printLoop
  }

  val shared = unshared.publish
  shared.subscribe(n => println(s"subscriber 1 gets $n"))

  shared.subscribe(n => println(s"subscriber 2 gets $n"))
  shared.connect
}
