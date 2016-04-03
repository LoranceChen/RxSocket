package jproto

import java.nio.ByteBuffer
import java.util.concurrent.TimeoutException

import lorance.rxscoket._
import lorance.rxscoket.presentation.json.{IdentityTask, JProtocol}
import lorance.rxscoket.session._
import rx.lang.scala.{Subject, Observable}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Promise, Future}
import scala.io.StdIn
import scala.collection.mutable

object DemoClientMain extends App {
  val client = new ClientEntrance("localhost", 10002)
  val socket = client.connect

  val sockets = mutable.Map[ConnectedSocket, Observable[Vector[CompletedProto]]]()

  logLevel = 3

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
//      val x = sendJProtocol.flatMap(l => l.sendWithResult[Rst, Req](Req("thread-time", lineJStr))(rst => rst.data.isEmpty)).publish
//      val x = sendJProtocol.flatMap(l => l.sendWithResultTest[Rst, Req](Req("thread-time", lineJStr)))//.publish

//      x.connect
      val x2 = sendJProtocol.flatMap(l => l.sendWithResult[Rst, Req](Req("thread-time", lineJStr), Some(rst => rst.takeWhile(_.data.nonEmpty))))//.publish

//      x2.connect

      x2.subscribe(
        i => log(s"get result of the task - $i"),
        {
          case e: TimeoutException =>log(s"task result onError - timeout")
          case _ => log(s"task result onError - error")
        },
        () => log(s"task complete")
      )

      //log(s"Rst - $i"))
//      x.subscribe(i => "")//log(s"Rst - $i"))
//      x.subscribe(i => "")//log(s"Rst - $i"))

//      val data = ByteBuffer.wrap(enCode(0.toByte, """{"key":"value"}"""))
//      socket.flatMap { s => {
//        s.send(data)
//      }}
//      p.connects
//      x.connect
//
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

//object TestFlatmap extends App {
//  val f = Future(1)
//  val obv = Observable.from(f).map(x => ("x", Observable.just(11))).publish
//  val b2 = obv.flatMap{x =>log("flat"); x._2}
//  b2.subscribe(c => log(c.toString))
//  obv.connect
//
//
//  val obv2 = Observable.from(f).map(x => ("x", Observable.just(12))).publish
//  val b22 = obv2.flatMap(x => x._2)
//  b22.subscribe(c => log(c.toString))
//  obv2.connect
//}

object TestMap extends App {
  val sub = Observable.just(1,2,3)

//  val runnable = new Runnable {
//    override def run(): Unit = {
//      while (true) {
//        val str =  StdIn.readLine()
//        sub.onNext(str)
//      }
//    }
//  }

//  new Thread(runnable).start()

  val x = sub.publish

  val maped = x.map{s =>
    log(s"observer - ${s}")
    s
  }.publish
  maped.connect

  x.connect

  maped.subscribe(s => log(s"$s"))

  Thread.currentThread().join()
}