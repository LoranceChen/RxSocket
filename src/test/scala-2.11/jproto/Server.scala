package jproto

import java.nio.ByteBuffer

import lorance.rxscoket._
import lorance.rxscoket.presentation.json.{IdentityTask, JsonParse, JProtocol}
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
  //read.connect

  case class Rst(taskId: String, data: Option[String]) extends IdentityTask

//  read.subscribe { x =>
//    log(s"receive form connect - ${x}")
//    val b = JsonParse.enCode(Rst("thread-time", Some("content")))
//    Thread.sleep(1000)
//    x.send(ByteBuffer.wrap(b))
//  }

  read.flatMap(x => x._2).subscribe(x => log(s"x - ${x.map(x => x.loaded.array().string)}"))

  var seq = 1
  val x = read.map{ x =>
    log(s"some one connect - ${x}")

    //pong
    val sk = x._1
    x._2.subscribe{ x =>
      val b1 = JsonParse.enCode(Rst("thread-time", Some("content" + seq)))
      val b2 = JsonParse.enCode(Rst("thread-time", Some("content" + seq * 10)))
      val b3 = JsonParse.enCode(Rst("thread-time", None))
      val b4 = JsonParse.enCode(Rst("thread-time", Some("content" + seq * 100)))

      log("seq - " + seq)

      seq = seq + 1

      sk.send(ByteBuffer.wrap(b1))
      sk.send(ByteBuffer.wrap(b2))
      sk.send(ByteBuffer.wrap(b3))
      sk.send(ByteBuffer.wrap(b4))
    }
  }
//  x.connect


  x.subscribe(x => log(s"some thing connected:"))
  Thread.currentThread().join()
}
