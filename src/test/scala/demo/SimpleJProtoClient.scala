package demo

import java.lang.management.ManagementFactory

import lorance.rxsocket.presentation.json.JProtocol
import lorance.rxsocket.session.ClientEntrance
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


  val client = new ClientEntrance("localhost", 10011).connect
  val jproto = client.map { x => new JProtocol(x, x.startReading) }

//  val namesFur = getMyNames("admin")
//
//  namesFur.foreach(names => println(names.toString))


//  Thread.sleep(10 * 1000)
  println("sleeped")

  import concurrent.duration._
  val rst = Await.result(jproto, 5 seconds).
    sendWithStream[Request, Response](Request("admin"),
      Some((x: Observable[Response]) => x.takeWhile(_.result.nonEmpty))
    )

  rst.subscribe{x =>
    println("rst - " + x)
    Continue
  }

  rst.subscribe{x =>
    println("rst2 - " + x)
    Continue
  }
  Thread.sleep(1000 * 2)

  println("later subscribe miss the message")
  rst.subscribe{x =>
    println("rst3 - " + x)
    Continue
  }

  Thread.currentThread().join()

  def getMyNames(accountId: String) = {
    jproto.flatMap { s =>
      val rsp = s.sendWithStream[Request, Response](Request(accountId),
        Some((x: Observable[Response]) => x.takeWhile(_.result.nonEmpty))//`takeWhile` marks which point the stream completed
      )
      toFuture(rsp)
    }
  }

  //transfer stream to Future if need
  private def toFuture(observable: Observable[Response]): Future[List[Response]] = {
    val p = Promise[List[Response]]
    val lst = scala.collection.mutable.ListBuffer[Response]()
    observable.subscribe(
      s => {lst.synchronized(lst.+=(s));Continue},
      e => p.tryFailure(e),
      () => p.trySuccess(lst.toList)
    )

    p.future
  }

  case class Request(accountId: String)
  case class Response(result: Option[String])
}

/**
OUTPUT:
ForkJoinPool-1-worker-9:1471133677987 - List(Response(Some(foo),ForkJoinPool-1-worker-9197464411151476), Response(Some(boo),ForkJoinPool-1-worker-9197464411151476))
*/