package lorance.rxscoket.session

import java.net.InetSocketAddress
import java.nio.channels.{CompletionHandler, AsynchronousSocketChannel, AsynchronousServerSocketChannel}

import lorance.rxscoket._
import lorance.rxscoket.dispatch.{TaskKey, TaskManager}
import rx.lang.scala.{Subscription, Subscriber, Observable}

import scala.concurrent.{Promise, Future}
import scala.util.{Success, Failure}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.collection.mutable
//import lorance.rxscoket.session.execution.currentThread

class ServerEntrance(host: String, port: Int) {
  private val connectionSubs = mutable.Set[Subscriber[ConnectedSocket]]()
  private def append(s: Subscriber[ConnectedSocket]) = connectionSubs.synchronized(connectionSubs += s)
  private def remove(s: Subscriber[ConnectedSocket]) = connectionSubs.synchronized(connectionSubs -= s)
//  private val heatBeats = new HeartBeats()
  val server: AsynchronousServerSocketChannel = {
    val server = AsynchronousServerSocketChannel.open
    val socketAddress: InetSocketAddress = new InetSocketAddress(host, port)
    val prepared = server.bind(socketAddress)
    rxsocketLogger.log(s"Server is prepare listen at - $socketAddress")
    prepared
  }

  private val heatBeatsManager = new TaskManager()
  /**
    * listen connection and emit every times connects event.
    */
  def listen: Observable[ConnectedSocket] = {
    rxsocketLogger.log("listen begin - ", 1)
    connectForever()

    val connected = Observable.apply[ConnectedSocket]({ s =>
      append(s)
      s.add(Subscription(remove(s)))
    }).doOnCompleted {
      rxsocketLogger.log("socket connection - doOnCompleted")
    }
    connected
  }

  private def connectForever() = {
    rxsocketLogger.log("connect loop begin -", 1)
    val f = connection(server)

    def connectForeverHelper(f: Future[AsynchronousSocketChannel]): Unit = {
      f.onComplete {
        case Failure(e) =>
          for(s <- connectionSubs) {s.onError(e)}
        case Success(c) =>
          val connectedSocket = new ConnectedSocket(c, heatBeatsManager,
            AddressPair(c.getLocalAddress.asInstanceOf[InetSocketAddress], c.getRemoteAddress.asInstanceOf[InetSocketAddress]))
          rxsocketLogger.log(s"client connected - ${connectedSocket.addressPair.remote}", 1, Some("connect"))

          val sendHeartTask = new HeartBeatSendTask(
            TaskKey(connectedSocket.addressPair.remote + ".SendHeartBeat", System.currentTimeMillis() + Configration.SEND_HEART_BEAT_BREAKTIME * 1000L),
            Some(-1, Configration.SEND_HEART_BEAT_BREAKTIME * 1000L),
            connectedSocket
          )
          val checkHeartTask = new HeartBeatCheckTask(
            TaskKey(connectedSocket.addressPair.remote + ".CheckHeartBeat", System.currentTimeMillis() + Configration.CHECK_HEART_BEAT_BREAKTIME * 1000L),
            Some(-1, Configration.CHECK_HEART_BEAT_BREAKTIME * 1000L),
            connectedSocket
          )

          rxsocketLogger.log(s"add heart beat to mananger - $sendHeartTask; $checkHeartTask", 400, Some("heart-beat"))
          heatBeatsManager.addTask(sendHeartTask)
          heatBeatsManager.addTask(checkHeartTask)

          for(s <- connectionSubs) {s.onNext(connectedSocket)}
          val nextConn = connection(server)
          connectForeverHelper(nextConn)
      }
    }
    connectForeverHelper(f)
  }

  private def connection(server: AsynchronousServerSocketChannel) = {
    val p = Promise[AsynchronousSocketChannel]
    val callback = new CompletionHandler[AsynchronousSocketChannel, AsynchronousServerSocketChannel] {
      override def completed(result: AsynchronousSocketChannel, attachment: AsynchronousServerSocketChannel): Unit = {
        rxsocketLogger.log("connect - success")
        p.trySuccess(result)
      }
      override def failed(exc: Throwable, attachment: AsynchronousServerSocketChannel): Unit = {
        rxsocketLogger.log("connect - failed")
        p.tryFailure(exc)
      }
    }

    server.accept(server, callback)
    p.future
  }
}
