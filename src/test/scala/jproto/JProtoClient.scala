package jproto

import lorance.rxscoket._
import lorance.rxscoket.presentation.json.{JProtocol, IdentityTask}
import lorance.rxscoket.session.ClientEntrance
import rx.lang.scala.Observable

import scala.concurrent.{Promise, Future}
import scala.util.{Success, Failure}
import scala.concurrent.ExecutionContext.Implicits.global

object JProtoClient extends App {
  val count = new Count()

  private def toFuture(observable: Observable[OverviewRsp]): Future[List[OverviewRsp]] = {
    val p = Promise[List[OverviewRsp]]
    val lst = scala.collection.mutable.ListBuffer[OverviewRsp]()
    observable.subscribe(
      s => {
        lst.synchronized(lst.+=(s))
        lorance.rxscoket.rxsocketLogger.log(s"overview Rsp ready - ${count.add} " + lst.mkString("\n"), -16)
      },
      e => p.tryFailure(e),
      () => p.trySuccess(lst.toList)
    )

    p.future
  }
  case class OverviewReq(penName: String)//, taskId: String = "blog/index/overview")// extends IdentityTask
  case class OverviewRsp(result: Option[OverviewContent])//, taskId: String)// extends IdentityTask
  case class OverviewContent(id: String)

  rxsocketLogger.logLevel = 1
  rxsocketLogger.logAim ++= List("send completed", "get protocol")//, "send completed")
  val client = new ClientEntrance("localhost", 10011)
  val connect = client.connect
  connect.onComplete{
    case Failure(f) => rxsocketLogger.log(s"connect fail - $f", -10)
    case Success(s) => rxsocketLogger.log(s"connect success - $s", -10)
  }

  val sr = connect.map(s => (s, s.startReading))

  val jproto = sr.map { x => rxsocketLogger.log("hi strat reading"); new JProtocol(x._1, x._2) }

  def get(name: String) = {
    jproto.flatMap { s =>
//      val rsp = s.sendWithResult[OverviewRsp, OverviewReq](OverviewReq(name, name), Some((x: Observable[OverviewRsp]) => x.takeWhile(_.result.nonEmpty)))
      val rsp = s.sendWithStream[OverviewReq, OverviewRsp](OverviewReq(name), Some((x: Observable[OverviewRsp]) => x.takeWhile(_.result.nonEmpty)))
      toFuture(rsp)
    }
  }

  def justSend(penName: String) = {
    jproto.flatMap { s =>
      s.send(OverviewReq(penName))
    }
  }

  rxsocketLogger.log(s"begin send 1000 times for make jvm hot =============", -15)
  for(i <- -1000 to -1) {
    get(s"ha${i}")
//    justSend(s"ha${i}")
  }

  Thread.sleep(7000)

//  rxsocketLogger.log(s"begin send 30000 times  =============", -15)
  for(i <- 1 to 30000) {
    get(s"ha${i}")
//    justSend(s"ha${i}")
  }

//  var str = new StringBuilder()
//  for( i <- 1 to 1024000) yield {
//    str.append("1")
//  }
//
//  get(str.toString)

  Thread.currentThread().join()
}
