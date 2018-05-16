package demo

import java.nio.ByteBuffer

import lorance.rxsocket.session._
import monix.execution.Ack.Continue
import monix.reactive.Observable
import monix.execution.Scheduler.Implicits.global

/**
  * simplest Example
  */
object NormalClient extends App{

  val client = new ClientEntrance("localhost", 10002, new CommActiveParser())
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

    val data = ByteBuffer.wrap(firstMsg ++ secondMsg)
    s.send(data)
  }

  Thread.currentThread().join()
}

/**
OUTPUT:
get info from server - uuid: 2, length: 44, load: Hi client, I'm get your info - hello server!
get info from server - uuid: 2, length: 45, load: Hi client, I'm get your info - 北京,你好!
*/