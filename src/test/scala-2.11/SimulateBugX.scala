import java.util.concurrent.TimeUnit

import rx.lang.scala.{Subscription, Observable, Subscriber}

import scala.collection.mutable
import lorance.rxscoket._

import scala.concurrent.duration.Duration

/**
  * it's not bug at all
  */
object SimulateBugX extends App {
  val obv = CreateObservable.get

  val obv2 = obv.map(x => x)
  obv2.subscribe(x => rxsocketLogger.log(s"Sub obv2 - $x"))

  //JProtocol.sendWithResult
  def sendWithTag(t: String) = {
    obv2.map {x =>
      rxsocketLogger.log(s"get ptorotocol - $t")
      x
    }
  }
  Thread.sleep(1000)
  println("-===================")

  //input loop
  val temp1 = sendWithTag("t1").takeUntil(x => x == "event04").timeout(Duration(2, TimeUnit.SECONDS))
  val sber1 = temp1.subscribe(s => rxsocketLogger.log(s"teskId -$s"))

  //socket read message
  for(s <- CreateObservable.subscirbers) s.onNext("event01")
//  sber1.unsubscribe()

  Thread.sleep(1000)
  println("-===================")
  val temp2 = sendWithTag("t2")
  val sber2 = temp2.subscribe(s => rxsocketLogger.log(s"teskId -$s"))

  for(s <- CreateObservable.subscirbers) s.onNext("event02")

//  sber2.unsubscribe()

  Thread.sleep(2000)
  println("-===================")

  val temp3 = sendWithTag("t3")
  val sber3 = temp3.subscribe(s => rxsocketLogger.log(s"teskId -$s"))

  for(s <- CreateObservable.subscirbers) s.onNext("event03")
//  sber3.unsubscribe()

  Thread.currentThread().join()
}

object CreateObservable {
  val subscirbers = mutable.Set[Subscriber[String]]()
  lazy val get = {
    val b = Observable[String]{s =>
      rxsocketLogger.log(s"add sber - $s")
      subscirbers += s
      s.add(Subscription(subscirbers -= s))
    }.doOnCompleted(rxsocketLogger.log(s" completed")).publish
    b.connect
    b
  }
}