package demo

import java.nio.ByteBuffer

import lorance.rxsocket.session._
import monix.execution.Ack.Continue
import monix.execution.Scheduler.Implicits.global
import monix.reactive.Observable

/**
  * simplest Example
  */
object NormalClientTest extends App{

//  lorance.rxsocket.session.Configration.CHECK_HEART_BEAT_BREAKTIME = Int.MaxValue
//  lorance.rxsocket.session.Configration.SEND_HEART_BEAT_BREAKTIME = Int.MaxValue

  val client = new ClientEntrance("localhost", 8001)
  val socket = client.connect


  val reading = Observable.fromFuture(socket).flatMap(_.startReading)

  reading.subscribe { proto =>
    println(
      s"get info from server - " +
        s"uuid: ${proto.uuid}, " +
        s"length: ${proto.length}, " +
        s"load: ${new String(proto.loaded.array())}")
    Continue
  }

  socket.foreach{s =>
    val firstMsg = enCode(2.toByte, "hello server!")
    val secondMsg = enCode(2.toByte, "北京,你好!")

    val data = ByteBuffer.wrap(firstMsg)// ++ secondMsg)
    s.send(data)
  }

  Thread.currentThread().join()
}
