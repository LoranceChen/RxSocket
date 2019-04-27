package reactivex

import java.util.concurrent.TimeUnit

import lorance.rxsocket.execution.global
import monix.eval.Task
import monix.execution.Ack.Continue
import monix.execution.atomic.Atomic
import monix.reactive.{Observable, OverflowStrategy}
import org.junit.Test


class MonixParallelNoGroup {
//  val log = LoggerFactory.getLogger(getClass)
  val intAtom = Atomic(0)
  @Test
  def test(): Unit = {
    val source = Observable.range(0,130)

    val processed = source.map(x => {
      println("first map - " + (x * 2))
      x * 2
    }).asyncBoundary(OverflowStrategy.BackPressure(5))
//      .mapParallelUnordered(parallelism = 5) { i =>
      .map { i =>

      //concurrrent 5 thread
//      Thread.sleep(1000)
      println("thread - " + Thread.currentThread().getName + ", " + i.toString)
//      Task((i % 3 toString) -> i * 2)
      (i % 3 toString) -> i * 2
    }.groupBy{case (k, v) => k}//??group 是否在乎顺序性？期望不在乎
    .mapParallelUnordered(parallelism = 5) {case a =>
      Thread.sleep(2000)
//        println("")
      println("22222 - " + Thread.currentThread().getName + " " + intAtom.addAndGet(1))
//        Task(a)
      Task(a)
    }.share.subscribe(x => Continue)
//      .share
//    log.info("aaa")
//    processed.subscribe{_ =>
//      Thread.sleep(1000)
//      println("-----| 33333 - " + Thread.currentThread().getName + " " + intAtom)
//      Continue
//    }
//
////    Thread.sleep(5000)
//    processed.subscribe{_ =>
//      Thread.sleep(1000)
//      println("-----| 44444 - " + Thread.currentThread().getName + " " + intAtom)
//      Continue
//    }
    global.scheduleWithFixedDelay(0L, 1000L, TimeUnit.MILLISECONDS, () => {
      println("=================")
    })
    Thread.currentThread().join()

  }

}
