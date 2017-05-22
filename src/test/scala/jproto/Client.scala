//package jproto
//
//import java.nio.ByteBuffer
//import java.util.concurrent.TimeoutException
//
//import lorance.rxscoket._
//import lorance.rxscoket.presentation.json.{IdentityTask, JProtocol}
//import lorance.rxscoket.session._
//import rx.lang.scala.{Subject, Observable}
//
//import scala.concurrent.ExecutionContext.Implicits.global
//import scala.util.{Success, Failure}
//
////import lorance.rxscoket.session.execution.currentThread
//import scala.concurrent.{Promise, Future}
//import scala.io.StdIn
//import scala.collection.mutable
//
//object DemoClientMain extends App {
//  val client = new ClientEntrance("localhost", 10002)
//  val socket = client.connect
//
//  val sockets = mutable.Map[ConnectedSocket, Observable[Vector[CompletedProto]]]()
//
//  rxsocketLogger.logLevel = 100
//
//  val sendJProtocol = Observable.from(socket).map{l =>
//    rxsocketLogger.log("new JProtocol")
//    new JProtocol(l, l.startReading)
//  }//{l => log("new JProtocol"); new JProtocol(l, l.startReading)}//.publish
//
////  sendJProtocol.connect
////  val sendWithProtocl = sendJProtocol.flatMap{l => l.read}//.publish
//
////  val rC = (s : Vector[CompletedProto]) => {log(s"receive - ${s.map(x => new String(x.loaded.array()))}")}
////  sendWithProtocl.subscribe{s =>
////    val r = rC(s)
////    r
////  }//(s => log(s"receive - ${s.map(x => new String(x.loaded.array()))}"))
//
//
//  case class Req(taskId: String, k: String) extends IdentityTask
//  case class Rst(taskId: String, data: Option[String]) extends IdentityTask
//
//  /**
//    * simulate application input
//    */
//  def inputLoop = {
//    while (true) {
//      rxsocketLogger.log(s"input message:")
//      val lineJStr = StdIn.readLine()
//      val x2 = sendJProtocol.flatMap(l => l.sendWithStream[Req, Rst](Req("thread-time", lineJStr), Some((rst: Observable[Rst]) => rst.takeUntil(_.data.isEmpty))))//.publish
////      val x2 = sendJProtocol.flatMap(l => l.sendWithStream[Req, Rst](Req(lineJStr), Some((rst: Observable[Rst]) => rst.takeUntil(_.data.isEmpty))))//.publish
//
//      x2.subscribe(
//        i => rxsocketLogger.log(s"get result of the task - $i"),
//        {
//          case e: TimeoutException =>rxsocketLogger.log(s"task result onError - timeout")
//          case _ => rxsocketLogger.log(s"task result onError - error")
//        },
//        () => rxsocketLogger.log(s"task complete")
//      )
//    }
//  }
//
//  val runnable = new Runnable {
//    override def run(): Unit = {
//      inputLoop
//    }
//  }
//
//  new Thread(runnable).start()
//
//  Thread.currentThread().join()
//}
