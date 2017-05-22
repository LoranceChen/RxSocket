package demo

import java.nio.ByteBuffer
import lorance.rxscoket.session._
import rx.lang.scala.Observable
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * simplest Example
  */
object NormalClient extends App{

  val client = new ClientEntrance("localhost", 10002)
  val socket = client.connect

  val reading = Observable.from(socket).flatMap(_.startReading)

  reading.subscribe { proto =>
    println(
      s"get info from server - " +
        s"uuid: ${proto.uuid}, " +
        s"length: ${proto.length}, " +
        s"load: ${new String(proto.loaded.array())}")
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