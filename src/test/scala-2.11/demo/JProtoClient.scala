package demo

import lorance.rxscoket._
import lorance.rxscoket.presentation.json.{JProtocol, IdentityTask}
import lorance.rxscoket.session.ClientEntrance
import rx.lang.scala.Observable

import scala.concurrent.{Promise, Future}

import scala.concurrent.ExecutionContext.Implicits.global
import lorance.rxscoket.presentation

/**
  *
  */
object JProtoClient extends App {

  val client = new ClientEntrance("localhost", 10011).connect
  val jproto = client.map { x => new JProtocol(x, x.startReading) }

  val namesFur = getMyNames("admin")

  namesFur.foreach(names => rxsocketLogger.log(names))

  Thread.currentThread().join()

  def getMyNames(accountId: String) = {
    jproto.flatMap { s =>
      val rsp = s.sendWithResult[Response, Request](Request(accountId),
        Some((x: Observable[Response]) => x.takeWhile(_.result.nonEmpty)))
      toFuture(rsp)
    }
  }

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

  case class Request(accountId: String, taskId: String = presentation.getTaskId) extends IdentityTask
  case class Response(result: Option[String], taskId: String) extends IdentityTask
}
