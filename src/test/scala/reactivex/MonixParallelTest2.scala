package reactivex

import java.util.concurrent.TimeUnit

import monix.execution.Scheduler.Implicits.global
import monix.execution.Ack.Continue
import monix.reactive.{Observable, OverflowStrategy}
import org.junit.Test


class MonixBackpressureWithGroupByTest2 {
  @Test
  def test(): Unit = {
    val source = Observable.range(0,130)

    val backPressuredStream = source.map(x => {
        println("simple log first  map - " + x)
        x
      })
      .asyncBoundary(OverflowStrategy.BackPressure(5))
      .map { i =>

        println("after backpressure map, and Rim 3 operation of source - " + ((i % 3) toString) -> i)
        ((i % 3) toString) -> i
      }
      .groupBy{case (k, v) => k}
      .flatMap(x => {
        val mapWithSleep = x.map{case groupedMsg@(key, value) =>
          Thread.sleep(2000)
          println("inner Observable after group by rim 3. sleep 2 second for every message - " + groupedMsg)
          groupedMsg
        }

        mapWithSleep

      })

    backPressuredStream.share.subscribe(
      (keyAndValue: (String, Long)) => Continue
    )

    global.scheduleWithFixedDelay(0L, 1000L, TimeUnit.MILLISECONDS, () => {
      println("========sleep 1 second ============")
    })

    Thread.currentThread().join()

  }

}
