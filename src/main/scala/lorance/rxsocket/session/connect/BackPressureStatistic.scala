package lorance.rxsocket.session.connect

import java.util.concurrent.atomic.LongAdder

import lorance.rxsocket.session.Configration
import monix.execution.atomic.AtomicLong
import monix.reactive.Observable
import monix.reactive.subjects.PublishSubject

object BackPressureStatistic {
  private val counter: LongAdder = new LongAdder()
//  private val counter2: AtomicLong = AtomicLong.apply(0L)
  private val pRateLimiter = PublishSubject[Boolean]()

  val rateLimiter: Observable[Boolean] = pRateLimiter

  def increment(): Unit = {
    counter.increment()

//    if(counter.sum > Configration.BACKPRESSURE * 1.2) {
//      pRateLimiter.onNext(true)
//    }
  }

  def decrement(): Unit = {
    counter.decrement()
    if(counter.sum < Configration.BACKPRESSURE * 0.8) {

      //todo ：会出现传说的惊群问题
      pRateLimiter.onNext(false)
    }
  }

  def sum() = {
    counter.sum
//    counter2.get
  }



}
