package lorance.rxsocket.session

import java.net.{InetSocketAddress, StandardSocketOptions}
import java.nio.channels.{AsynchronousChannelGroup, AsynchronousServerSocketChannel, AsynchronousSocketChannel, CompletionHandler}
import java.util.concurrent.{Executors, ThreadFactory}

import org.slf4j.LoggerFactory
import lorance.rxsocket.dispatch.TaskManager
import monix.reactive.Observable
import monix.reactive.subjects.PublishSubject

import scala.concurrent.{Future, Promise}
import scala.util.{Failure, Success}
import lorance.rxsocket.execution.global

class ServerEntrance[Proto](host: String, port: Int, genParser: () => ProtoParser[Proto]) {
  private val logger = LoggerFactory.getLogger(getClass)

  private val connectionSubs = PublishSubject[ConnectedSocket[Proto]]

  val socketAddress: InetSocketAddress = new InetSocketAddress(host, port)

  val server: AsynchronousServerSocketChannel = {
    val server = AsynchronousServerSocketChannel.open
    val prepared = server.bind(socketAddress)
    logger.info(s"server is bind at - $socketAddress")
    prepared
  }

  //todo: consider use pool for ton of socket
//  private val heatBeatsManager = new TaskManager()

  /**
    * listen connection and emit every times connects event.
    */
  def listen: Observable[ConnectedSocket[Proto]] = {
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
          val connectedSocket = new ConnectedSocket[Proto](c,
//            heatBeatsManager,
            AddressPair(c.getLocalAddress.asInstanceOf[InetSocketAddress], c.getRemoteAddress.asInstanceOf[InetSocketAddress]),
            true,
            genParser()
          )
          logger.info(s"client connected - ${connectedSocket.addressPair.remote}")

//          val checkHeartTask = new HeartBeatCheckTask(
//            TaskKey(connectedSocket.addressPair.remote + ".CheckHeartBeat", System.currentTimeMillis() + Configration.CHECK_HEART_BEAT_BREAKTIME * 1000L),
//            Some(-1, Configration.CHECK_HEART_BEAT_BREAKTIME * 1000L),
//            connectedSocket
//          )

  //          logger.trace(s"add heart beat to mananger - $sendHeartTask; $checkHeartTask")
//          heatBeatsManager.addTask(checkHeartTask)

          //todo connect setting back-pressure
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
    // scala.Boolean vs java.lang.Boolean:
    // https://stackoverflow.com/questions/25665379/calling-java-generic-function-from-scala?utm_medium=organic&utm_source=google_rich_qa&utm_campaign=google_rich_qa
    server.setOption[java.lang.Boolean](StandardSocketOptions.SO_REUSEADDR, true)
    // not support yet
//        server.setOption[java.lang.Integer](StandardSocketOptions.SO_LINGER, 3)
    //    server.setOption[java.lang.Boolean](StandardSocketOptions.SO_KEEPALIVE, true)
    server.accept(server, callback)
    p.future
  }

}
