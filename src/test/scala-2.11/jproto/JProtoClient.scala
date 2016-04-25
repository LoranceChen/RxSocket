package jproto

import lorance.rxscoket._
import lorance.rxscoket.presentation.json.{JProtocol, IdentityTask}
import lorance.rxscoket.session.ClientEntrance
import rx.lang.scala.Observable

import scala.concurrent.{Promise, Future}
import scala.util.{Success, Failure}
import scala.concurrent.ExecutionContext.Implicits.global

object JProtoClient extends App {
  private def toFuture(observable: Observable[OverviewRsp]): Future[List[OverviewRsp]] = {
    val p = Promise[List[OverviewRsp]]
    val lst = scala.collection.mutable.ListBuffer[OverviewRsp]()
    observable.subscribe(
      s => {
        lst.synchronized(lst.+=(s))
        lorance.rxscoket.log("overview Rsp ready - " + lst.mkString("\n"), -16)
      },
      e => p.tryFailure(e),
      () => p.trySuccess(lst.toList)
    )

    p.future
  }
  case class OverviewReq(penName: String, taskId: String = "blog/index/overview") extends IdentityTask
  case class OverviewRsp(result: Option[OverviewContent], taskId: String) extends IdentityTask
  case class OverviewContent(id: String)

  logLevel = -15
//  logAim ++= List("get protocol")//, "send completed")
  val client = new ClientEntrance("localhost", 10011)
  val connect = client.connect
  connect.onComplete{
    case Failure(f) => log(s"connect fail - $f", -10)
    case Success(s) => log(s"connect success - $s", -10)
  }

  val sr = connect.map(s => (s, s.startReading))

  val jproto = sr.map { x => log("hi strat reading"); val x1= new JProtocol(x._1, x._2) ; x1.jRead.subscribe{x1 => log("aaaa", 100)}; x1}

  def get(name: String) = {
    jproto.flatMap { s =>
      val rsp = s.sendWithResult[OverviewRsp, OverviewReq](OverviewReq(name, name), Some(x => x.takeWhile(_.result.nonEmpty)))
      toFuture(rsp)
    }
  }

  def justSend(penName: String) = {
    jproto.flatMap { s =>
      s.send(OverviewReq(penName, penName))
    }
  }

  log(s"begin send 1000 times for make jvm hot =============", -15)
  for(i <- 1 to 3000) {
    get(s"ha${i}")
//    justSend(s"ha${i}")
  }

  Thread.sleep(7000)
//  get(s"ha${20}")
  log(s"begin send 1000 times  =============", -15)
  for(i <- 1 to 16000) {
    get(s"ha${i}")
//    justSend(s"ha${i}")
  }
//  get(s"ha${20}")

  Thread.currentThread().join()
}
