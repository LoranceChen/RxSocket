package jproto

import java.nio.ByteBuffer

import lorance.rxscoket._
import lorance.rxscoket.presentation.json.{IdentityTask, JsonParse}
import lorance.rxscoket.session.{ConnectedSocket, ServerEntrance}
import lorance.rxscoket.session.implicitpkg._
import rx.lang.scala.Observable

/**
  *
  */
object DemoServerMain extends App {
  val server = new ServerEntrance("localhost", 10002)
  val socket: Observable[ConnectedSocket] = server.listen

  /**
    * read obs
    * NOTE: use `.publish` ensure the production of Observable is hot - a hot observable make a independence of a map chain,
    * it also important to avoid multi execute `startReading`.
    * Why use hot: It's really sad, the `map` make subscribe exec map body every times.so `stratReading` caused failure of
    */
  val read = socket.map(l => (l, l.startReading))

  case class Req(taskId: String, req: String) extends IdentityTask
  case class Rst(taskId: String, data: Option[String]) extends IdentityTask

  read.flatMap(x => x._2).subscribe(x => rxsocketLogger.log(s"x - ${x.loaded.array().string}"))

  var seq = 1
  val x = read.map{ x =>
    rxsocketLogger.log(s"some one connect - ${x}")

    //pong
    val sk = x._1
    x._2.subscribe{ x =>
      val b1 = JsonParse.enCode(Rst("thread-time", Some("content" + seq)))
      val b2 = JsonParse.enCode(Rst("thread-time", Some("content" + seq * 10)))
      val b5 = JsonParse.enCode(Rst("thread-time", Some("content" + seq * 100)))
      val b3 = JsonParse.enCode(Rst("thread-time", None))
      val b4 = JsonParse.enCode(Rst("thread-time", Some("content" + seq * 1000)))

      rxsocketLogger.log("seq - " + seq)

      seq = seq + 1

      sk.send(ByteBuffer.wrap(b1))
      sk.send(ByteBuffer.wrap(b2))
      sk.send(ByteBuffer.wrap(b5))
      sk.send(ByteBuffer.wrap(b3))
      sk.send(ByteBuffer.wrap(b4))
    }
  }
x.subscribe()
//  val jread = read.map(x => new JProtocol(x._1, x._2))
//  jread.subscribe{x =>
//
//    x.send(Rst("taskId", Some("content1")))
//    x.send(Rst("taskId", Some("content3")))
//    x.send(Rst("taskId", Some("content2")))
//    x.send(Rst("taskId", None))
//  }
  Thread.currentThread().join()
}
