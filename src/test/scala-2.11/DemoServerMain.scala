
import java.nio.ByteBuffer

import lorance.rxscoket._
import lorance.rxscoket.session.{ConnectedSocket, ServerEntrance}
import rx.lang.scala.Observable

/**
  * TODO difference between Observer and Subscription? both of them has OnNext, OnError, OnComplete.which stage in Rx is them place
  */
object DemoServerMain extends App {
  logLevel = 1000
  val server = new ServerEntrance("localhost", 10002)
  val socket: Observable[ConnectedSocket] = server.listen

  socket.subscribe(s => log(s"Hi, Mike, someone connected - "))
  socket.subscribe(s => log(s"Hi, John, someone connected - "))

  /**
    * read obs
    * NOTE: use `.publish` ensure the production of Observable is hot - a hot observable make a independence of a map chain,
    * it also important to avoid multi execute `startReading`.
    * Why use hot: It's really sad, the `map` make subscribe exec map body every times.so `stratReading` caused failure of
    */
  val read = socket.flatMap{l => l.startReading.map(l -> _)}.publish
  read.connect

  socket.subscribe(s => log(s"Hi, John2, someone connected - "))

  //this way also make a subscribe event to hot Observable by explicit create new one. It seems odd but practical.
//  var read2: Observable[Vector[CompletedProto]] = {
//    val p = Promise[Observable[Vector[CompletedProto]]]
//    socket.subscribe{l => log("read start reading");p.trySuccess(l.startReading)}
//    Observable.from(p.future).flatten
//  }

  read.subscribe{r => r._2.foreach{x =>
      log(s"first subscriber get protocol - ${new String(x.loaded.array())}")
      val msg = "hi client~"
      r._1.send(ByteBuffer.wrap(session.enCode(0.toByte, msg)))
    }
  }
  read.subscribe{r => r._2.foreach{x => log(s"second subscriber get protocol - ${new String(x.loaded.array())}")}}

  Thread.currentThread().join()
}
