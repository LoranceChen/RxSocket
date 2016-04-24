package jproto

import java.nio.ByteBuffer
import java.util.concurrent.TimeoutException

import lorance.rxscoket._
import lorance.rxscoket.presentation.json.{IdentityTask, JProtocol}
import lorance.rxscoket.session._
import rx.lang.scala.{Subject, Observable}

import scala.concurrent.ExecutionContext.Implicits.global
//import lorance.rxscoket.session.execution.currentThread
import scala.concurrent.{Promise, Future}
import scala.io.StdIn
import scala.collection.mutable

object DemoClientMain extends App {
  val client = new ClientEntrance("localhost", 10002)
  val socket = client.connect

  val sockets = mutable.Map[ConnectedSocket, Observable[Vector[CompletedProto]]]()

  logLevel = 100

  val sendJProtocol = Observable.from(socket).map{l =>
    log("new JProtocol")
    new JProtocol(l, l.startReading)
  }//{l => log("new JProtocol"); new JProtocol(l, l.startReading)}//.publish

//  sendJProtocol.connect
//  val sendWithProtocl = sendJProtocol.flatMap{l => l.read}//.publish

//  val rC = (s : Vector[CompletedProto]) => {log(s"receive - ${s.map(x => new String(x.loaded.array()))}")}
//  sendWithProtocl.subscribe{s =>
//    val r = rC(s)
//    r
//  }//(s => log(s"receive - ${s.map(x => new String(x.loaded.array()))}"))


  case class Req(taskId: String, k: String) extends IdentityTask
  case class Rst(taskId: String, data: Option[String]) extends IdentityTask

  /**
    * simulate application input
    */
  def inputLoop = {
    while (true) {
      log(s"input message:")
      val lineJStr = StdIn.readLine()
      val x2 = sendJProtocol.flatMap(l => l.sendWithResult[Rst, Req](Req("thread-time", lineJStr), Some(rst => rst.takeUntil(_.data.isEmpty))))//.publish

      x2.subscribe(
        i => log(s"get result of the task - $i"),
        {
          case e: TimeoutException =>log(s"task result onError - timeout")
          case _ => log(s"task result onError - error")
        },
        () => log(s"task complete")
      )
    }
  }

  val runnable = new Runnable {
    override def run(): Unit = {
      inputLoop
    }
  }

  new Thread(runnable).start()

  Thread.currentThread().join()
}

object InitBugClient extends App {
  private def toFuture(observable: Observable[OverviewRsp]): Future[List[OverviewRsp]] = {
    val p = Promise[List[OverviewRsp]]
    val lst = scala.collection.mutable.ListBuffer[OverviewRsp]()
    observable.subscribe(
      s => {
        Thread.sleep(1000)
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

  logLevel = -14

  val client = new ClientEntrance("localhost", 10011)
  val sr = client.connect.map(s => (s, s.startReading))
  val jproto = sr.map { x => log("init jProtocol - ", -20); new JProtocol(x._1, x._2) }

  def get(penName: String) = {
    log(s"penNameReady - ${penName}" ,10)

    jproto.flatMap { s =>
      log(s"penName - ${penName}" ,10)
      val rsp = s.sendWithResult[OverviewRsp, OverviewReq](OverviewReq(penName, penName), Some(x => x.takeWhile(_.result.nonEmpty)))
      toFuture(rsp)
    }
  }

  def justSend(penName: String) = {

    jproto.flatMap { s =>
      s.send(OverviewReq(penName, penName))
    }
  }
//
//  for(i <- -10 to -1) {
//    get(s"ha${i}")
//  }

//  sr.map{s =>
//    s._2.subscribe(x =>log(s"get ptorocol - ${x}", -100))
//  }

//  Thread.sleep(5000)
  log(s"begin send =============", -15)
  for(i <- 1 to 1000) {
    get(s"ha${i}")
//    justSend(s"ha${i}")
  }

  Thread.sleep(10000)
  log(s"begin send2  =============", -15)
  for(i <- 1 to 1000) {
    get(s"ha${i}")
//    justSend(s"ha${i}")
  }

  Thread.currentThread().join()
}