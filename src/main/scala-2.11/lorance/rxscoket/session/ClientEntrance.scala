package lorance.rxscoket.session

import java.net.{InetSocketAddress, SocketAddress}
import java.nio.channels.{CompletionHandler, AsynchronousSocketChannel}

import lorance.rxscoket._
import lorance.rxscoket.session.implicitpkg._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Promise
/**
  *
  */
class ClientEntrance(remoteHost: String, remotePort: Int) {
  def connect = {
    val channel: AsynchronousSocketChannel = AsynchronousSocketChannel.open
    val serverAddr: SocketAddress = new InetSocketAddress(remoteHost, remotePort)

    val p = Promise[ConnectedSocket]
    channel.connect(serverAddr, channel, new CompletionHandler[Void, AsynchronousSocketChannel]{
      override def completed(result: Void, attachment: AsynchronousSocketChannel): Unit = {
        log(s"linked to server success", -10)
        p.trySuccess(new ConnectedSocket(attachment))
      }

      override def failed(exc: Throwable, attachment: AsynchronousSocketChannel): Unit = {
        log(s"linked to server error - $exc")
        p.tryFailure(exc)
      }
    })

    /**
      * to test
      * 目前没有合适的方式来测试超时异常是否能起作用
      * 之前想到测试的方式是将服务端的端口绑定好ip地址,但不执行监听行为,但是就算这样,客户端依然认为是连接成功.
      */
    p.future.withTimeout(Configration.CONNECT_TIME_LIMIT * 1000)
  }
}
