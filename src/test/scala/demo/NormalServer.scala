package demo

import java.nio.ByteBuffer

import lorance.rxsocket._
import lorance.rxsocket.session._
import monix.execution.Ack.Continue
import monix.reactive.Observable
import lorance.rxsocket.execution.global

/**
  * simplest Example
  */
object NormalServer extends App{
  val server = new ServerEntrance("localhost", 10002, () => new CommActiveParser())
  val socket: Observable[ConnectedSocket[CompletedProto]] = server.listen

  socket.subscribe{s => println(s"Hi, Mike, someone connected - "); Continue}
  socket.subscribe{s => println(s"Hi, John, someone connected - "); Continue}

  val protoStream = socket.flatMap(_.startReading)

  protoStream subscribe {info =>
    println(s"get info from stream - uuid: ${info.uuid}; length: ${info.length}; load: ${new String(info.loaded.array())}")
    Continue
  }

  val withContext = socket.flatMap{connection => connection.startReading.map( connection -> _)}

  withContext.subscribe{x =>
    val connection = x._1
    val info = x._2

    val load = new String(info.loaded.array())
    println(s"from - ${connection.addressPair.remote.toString} - info - uuid: ${info.uuid}; length: ${info.length}; load: $load")

    val response = s"Hi client, I'm get your info - $load"
    val protoType = 2.toByte //custom with your client
    connection.send(ByteBuffer.wrap(session.enCode(protoType, response)))

    Continue
  }

  Thread.currentThread().join()
}
/**
  * OUTPUT:
Thread-11:1471134131207 - connect - success
Hi, John, someone connected -
Hi, Mike, someone connected -
get info from stream - uuid: 2; length: 13; load: hello server!
get info from stream - uuid: 2; length: 14; load: 北京,你好!
from - /127.0.0.1:63962 - info - uuid: 2; length: 13; load: hello server!
from - /127.0.0.1:63962 - info - uuid: 2; length: 14; load: 北京,你好!
*/