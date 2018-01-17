package demo

import lorance.rxsocket._
import lorance.rxsocket.presentation.json.{IdentityTask, JProtocol}
import lorance.rxsocket.session.ClientEntrance
import rx.lang.scala.Observable

import scala.concurrent.{Future, Promise}
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Json presentation Example
  */
object JProtoClient extends App {
  val client = new ClientEntrance("localhost", 10011).connect
  val jproto = client.map { x => new JProtocol(x, x.startReading) }

  val namesFur = getMyNames("admin")

  namesFur.foreach(names => println(names.toString))

  Thread.currentThread().join()

// older api `sendWithResult`
//  def getMyNames(accountId: String) = {
//    jproto.flatMap { s =>
//      val rsp = s.sendWithResult[Response, Request](Request(accountId),
//        Some((x: Observable[Response]) => x.takeWhile(_.result.nonEmpty))//`takeWhile` marks which point the stream completed
//      )
//      toFuture(rsp)
//    }
//  }

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
      s => {
        lst.synchronized(lst.+=(s))
      },
      e =>
        p.tryFailure(e),
      () =>
        p.trySuccess(lst.toList)
    )

    p.future
  }

//  case class Request(accountId: String, taskId: String = presentation.getTaskId) extends IdentityTask
//  case class Response(result: Option[String], taskId: String) extends IdentityTask
  case class Request(accountId: String)
  case class Response(result: Option[String])
}

/**
OUTPUT:
ForkJoinPool-1-worker-9:1471133677987 - List(Response(Some(foo),ForkJoinPool-1-worker-9197464411151476), Response(Some(boo),ForkJoinPool-1-worker-9197464411151476))
*/