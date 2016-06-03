package session

import lorance.rxscoket._
import lorance.rxscoket.session.{Configration, TaskKey, HeartBeatTask, HeartBeatsManager, ServerEntrance, ClientEntrance}

import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContext.Implicits.global
/**
  *
  */
object TestHeartBeatClient extends App {
  val client = new ClientEntrance("127.0.0.1", 10000)
  lorance.rxscoket.rxsocketLogger.logAim = ListBuffer("heart-beat")
  rxsocketLogger.logLevel = 1000

  val x= client.connect
  x.foreach{y => println(y)}
  Thread.currentThread().join()
}

object TestHeartBeatServer extends App {
  import lorance.rxscoket.rxsocketLogger
  rxsocketLogger.logLevel = 1000
  val server = new ServerEntrance("127.0.0.1", 10000)
  lorance.rxscoket.rxsocketLogger.logAim = ListBuffer("heart-beat")

  server.listen.subscribe{x => println(s"$x - connected")}

  Thread.currentThread().join()
}

//object TestManager extends App {
//  val manager = new HeartBeatsManager()
//
//  manager.addTask()
//
//  def getTask() = {
//    new HeartBeatTask(
//      TaskKey(connectedSocket.addressPair.remote, System.currentTimeMillis() + Configration.HEART_BEAT_BREAKTIME * 1000L),
//      Some(-1, Configration.HEART_BEAT_BREAKTIME * 1000L),
//      connectedSocket
//    )
//  }
//}