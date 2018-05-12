package lorance.rxsocket.session

import java.net.{InetSocketAddress, SocketAddress}
import java.nio.channels.{AsynchronousSocketChannel, CompletionHandler}

import org.slf4j.LoggerFactory
import lorance.rxsocket.dispatch.{TaskKey, TaskManager}
import lorance.rxsocket.session.implicitpkg._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future, Promise}

/**
  *
  */
class ClientEntrance(remoteHost: String, remotePort: Int) {
  private val logger = LoggerFactory.getLogger(getClass)

  val channel: AsynchronousSocketChannel = AsynchronousSocketChannel.open
  val serverAddr: SocketAddress = new InetSocketAddress(remoteHost, remotePort)

//  private val heartBeatManager = new TaskManager()

  def connect: Future[ConnectedSocket] = {
    val p = Promise[ConnectedSocket]

    channel.connect(serverAddr, channel, new CompletionHandler[Void, AsynchronousSocketChannel]{
      override def completed(result: Void, attachment: AsynchronousSocketChannel): Unit = {
        logger.debug(s"linked to server success")
        val connectedSocket = new ConnectedSocket(attachment,
//          heartBeatManager,
          AddressPair(channel.getLocalAddress.asInstanceOf[InetSocketAddress], channel.getRemoteAddress.asInstanceOf[InetSocketAddress]),
          false
        )

//        logger.info("add heart beat task")
//        heartBeatManager.addTask(new HeartBeatSendTask(
//          TaskKey(connectedSocket.addressPair.remote + ".SendHeartBeat", System.currentTimeMillis() + Configration.SEND_HEART_BEAT_BREAKTIME * 1000L),
//            Some(-1, Configration.SEND_HEART_BEAT_BREAKTIME * 1000L),
//            connectedSocket
//          )
//        )

        p.trySuccess(connectedSocket)
      }

      override def failed(exc: Throwable, attachment: AsynchronousSocketChannel): Unit = {
        logger.debug(s"linked to server error - $exc")
        p.tryFailure(exc)
      }
    })

    /**
      * to test
      * 目前没有合适的方式来测试超时异常是否能起作用
      * 之前想到测试的方式是将服务端的端口绑定好ip地址,但不执行监听行为,但是就算这样,客户端依然认为是连接成功.
      */
    p.future
//     .withTimeout(Configration.CONNECT_TIME_LIMIT * 1000)
  }
}
