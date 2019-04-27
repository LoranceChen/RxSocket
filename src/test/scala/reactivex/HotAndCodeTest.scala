package reactivex

import java.util.concurrent.{Executors, TimeUnit}

import monix.eval.Task
import monix.execution.Ack.{Continue, Stop}
import monix.execution.{Ack, Scheduler}
import monix.reactive.{Observable, OverflowStrategy}
import monix.reactive.observers.Subscriber
import monix.reactive.subjects.PublishSubject
import org.junit.Test
import org.slf4j.LoggerFactory

import concurrent.duration._
import lorance.rxsocket.execution.global

import scala.concurrent.{Future, Promise}
/**
  *
  */
class HotAndCodeTest {
  val logger = LoggerFactory.getLogger(getClass)

  @Test
  def coldToHot(): Unit = {
    val obv = Observable.interval(1 second)

    val maped = obv.map{x => println(s"a map $x");x}.publish
    maped.connect

    maped.doOnNext(l =>
      println(s"get - $l")
    ).subscribe()
    maped.doOnNext(l =>
      println(s"get2 - $l")
    ).subscribe()

    def subscribe[T](onNext: T => Unit, onError: Throwable => Unit = _ => Unit, onComplete: () => Unit = () => Unit) = {
      Subscriber
    }

    Thread.sleep(1000 * 5)
    maped.subscribe { l =>
      println(s"get3 - $l")
      Continue
    }

    Thread.currentThread().join()
  }

  @Test
  def subject(): Unit = {
    val obv = PublishSubject[Long]

    new Thread(() => {var i = 0;while(true){Thread.sleep(1 * 1000);obv.onNext(i);i+=1}}).start()

    val maped = obv.map{x => println(s"a map $x");x}
      .share

    val x = maped//.share

    /**
      * map will make hot observable to hot, use share make it to hot (ugly design, make it just hot after
      * map is fine.)
      */
    val mapOnHot = maped.map{x => println(s"map on hot - $x");x}.share

    mapOnHot.doOnNext(l =>
      println(s"get - $l")
    )
    mapOnHot.doOnNext(l =>
      println(s"get2 - $l")
    )


    Thread.sleep(1000 * 5)
    mapOnHot.doOnNext(l =>
      println(s"get3 - $l")
    )

    Thread.currentThread().join()
  }

  /**
    * good practice: return a hot at last with `share`
    */
  @Test
  def shareAtLast(): Unit = {
    val obv = PublishSubject[Long]

    new Thread(() => {var i = 0;while(true){Thread.sleep(1 * 1000);obv.onNext(i);i+=1}}).start()

    val maped = obv.map{x => println(s"a map1 $x");x}
    val maped2 = maped.map{x => println(s"a map2 $x");x}
    val maped3 = maped2.map{x => println(s"a map3 $x");x}

    /**
      * map will make hot observable to hot, use share make it to hot (ugly design, make it just hot after
      * map is fine.)
      */
    val mapOnHot = maped3.map{x => println(s"map to hot - $x");x}.share

    mapOnHot.doOnNext(l =>
      println(s"get - $l")
    )
    mapOnHot.doOnNext(l =>
      println(s"get2 - $l")
    )


    Thread.sleep(1000 * 5)
    mapOnHot.doOnNext(l =>
      println(s"get3 - $l")
    )

    Thread.currentThread().join()
  }


  /**
    * error or completed, just one happen
    */
  @Test
  def errorNotDoCompleted(): Unit = {
    val obv = PublishSubject[Long]

    new Thread(() => {
      var i = 0
      while(i < 3){
        Thread.sleep(1 * 1000)
        obv.onNext(i)
        i+=1
      }

      obv.onError(new Exception("error!!!"))
    }).start()

    obv.doOnNext(x => println("msg - " + x))
    obv.doOnError(x => println("error - " + x))
    obv.doOnComplete(() => println("complete - "))

    Thread.currentThread().join()
  }


  /**
    * observeOn
    * subscribeOn
    */
  @Test
  def MultiThread(): Unit = {
    val obv = PublishSubject[Long]

    logger.info("begin in main thread")
    new Thread(() => {
      logger.info("onNext in new thread")

      var i = 0
      while(true){
        Thread.sleep(1 * 100)

        obv.onNext({
          logger.info("on next - " + i)
          i
        })
        i+=1
      }
    }).start()

    val observeOn = obv
//      .observeOn(ExecutionContextScheduler(
//      concurrent.ExecutionContext.global))
      .subscribeOn(Scheduler(
      Executors.newFixedThreadPool(2)
    ))

    val maped = observeOn.map{x => logger.info("do a map");x}
    observeOn.doOnNext(x => logger.info(s"${Thread.currentThread().getName}: obv msg - " + x))
    observeOn.doOnError(x => logger.info("obv error - " + x))
    observeOn.doOnComplete(() => logger.info("obv complete - "))

    maped.doOnNext(x => logger.info("maped msg - " + x))
    maped.doOnError(x => logger.info("maped error - " + x))
    maped.doOnComplete(() => logger.info("maped complete - "))

    Thread.currentThread().join()
  }

  @Test
  def subscribe(): Unit = {
    val obv = PublishSubject[Int]()
    var a = 1
    Scheduler.global.scheduleAtFixedRate(
      1000 * 3L,
      1000 * 3L,
      TimeUnit.MILLISECONDS,
      () => {
        obv.onNext(a)
        a = a + 1
      }
    )

    Scheduler.global.scheduleOnce(
      1000 * 10L,
      TimeUnit.MILLISECONDS,
      () => {
        obv.subscribe{a =>
          println("aaa - " + a)
          Stop
        }
      }
    )

    Thread.currentThread().join()
  }

  //test asyncBoundary
  @Test
  def testAsyncBoundary(): Unit = {
    val obv = PublishSubject[Int]()
    Scheduler.global.scheduleOnce(5000L, TimeUnit.MILLISECONDS,
      () => {
        var i = 0
        while(i < 1000) {
          obv.onNext(i)
          i=i+1
        }
      })

    val backed = obv.whileBusyBuffer(OverflowStrategy.DropNew(10))

    //1s后完成，查看是否有10个并发
    backed.subscribe(x => {
      println("do work - " + x)
      val p = Promise[Ack]
      Scheduler.global.scheduleOnce(1000, TimeUnit.MILLISECONDS, () => {
        p.completeWith(Ack.Continue)
      })
      p.future
    })

    Thread.currentThread().join()
  }
}
