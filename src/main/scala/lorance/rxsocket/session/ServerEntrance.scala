package lorance.rxsocket.session

import java.net.InetSocketAddress
import java.nio.channels.{AsynchronousServerSocketChannel, AsynchronousSocketChannel, CompletionHandler}

import org.slf4j.LoggerFactory
import lorance.rxsocket.dispatch.{TaskKey, TaskManager}
import rx.lang.scala.{Observable, Subject}

import scala.concurrent.{Future, Promise}
import scala.util.{Failure, Success}
import scala.concurrent.ExecutionContext.Implicits.global

class ServerEntrance(host: String, port: Int) {
  private val logger = LoggerFactory.getLogger(getClass)

  private val connectionSubs = Subject[ConnectedSocket]()
  val socketAddress: InetSocketAddress = new InetSocketAddress(host, port)

  val server: AsynchronousServerSocketChannel = {
    val server = AsynchronousServerSocketChannel.open
    val prepared = server.bind(socketAddress)
    logger.info(s"server is bind at - $socketAddress")
    prepared
  }

  private val heatBeatsManager = new TaskManager()

  /**
    * listen connection and emit every times connects event.
    */
  def listen: Observable[ConnectedSocket] = {
    logger.info(s"server start listening at - $socketAddress")
    connectForever()

    connectionSubs
  }

  private def connectForever() = {
    logger.trace("connect loop begin -")
    val f = connection(server)

    def connectForeverHelper(f: Future[AsynchronousSocketChannel]): Unit = {
      f.onComplete {
        case Failure(e) =>
          logger.warn("connection set up fail", e)
        case Success(c) =>
          val connectedSocket = new ConnectedSocket(c, heatBeatsManager,
            AddressPair(c.getLocalAddress.asInstanceOf[InetSocketAddress], c.getRemoteAddress.asInstanceOf[InetSocketAddress]))
          logger.info(s"client connected - ${connectedSocket.addressPair.remote}")

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

          logger.trace(s"add heart beat to mananger - $sendHeartTask; $checkHeartTask")
          heatBeatsManager.addTask(sendHeartTask)
          heatBeatsManager.addTask(checkHeartTask)

          connectionSubs.onNext(connectedSocket)

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
        logger.trace("connect success on callback")
        p.trySuccess(result)
      }
      override def failed(exc: Throwable, attachment: AsynchronousServerSocketChannel): Unit = {
        logger.error("connect failed on callback", exc)
        p.tryFailure(exc)
      }
    }

    server.accept(server, callback)
    p.future
  }
}
