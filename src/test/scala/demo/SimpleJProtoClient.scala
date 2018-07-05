package demo

import java.lang.management.ManagementFactory

import lorance.rxsocket.presentation.json.JProtocol
import lorance.rxsocket.session.{ClientEntrance, CommActiveParser, CommPassiveParser}
import monix.execution.Ack.Continue
import monix.reactive.Observable

import scala.concurrent.{Await, Future, Promise}
import monix.execution.Scheduler.Implicits.global

/**
  * Json presentation Example
  */
object SimpleJProtoClient extends App {


  val runtime = ManagementFactory.getRuntimeMXBean()
  val name = runtime.getName()
  System.out.println("当前进程的标识为："+name)
  val index = name.indexOf("@")
  if (index != -1) {
    val pid = Integer.parseInt(name.substring(0, index))
    System.out.println("当前进程的PID为："+pid)
  }


  val client = new ClientEntrance("localhost", 10011, () => new CommPassiveParser()).connect
  val jproto = client.map { x => new JProtocol(x, x.startReading) }

  val rst = Observable.fromFuture(jproto).flatMap(jprotocol => {
    jprotocol.sendWithStream[Request, Response](Request("admin"),
      Some((x: Observable[Response]) => x.takeWhile(_.result.nonEmpty))
    )
  })


  rst.subscribe{x =>
    println("subscriber1 get result - " + x)
    Continue
  }

  rst.subscribe{x =>
    println("subscriber2 get result - " + x)
    Continue
  }

  Thread.currentThread().join()

  case class Request(accountId: String)
  case class Response(result: Option[String])
}

/**
OUTPUT:
ForkJoinPool-1-worker-9:1471133677987 - List(Response(Some(foo),ForkJoinPool-1-worker-9197464411151476), Response(Some(boo),ForkJoinPool-1-worker-9197464411151476))
*/