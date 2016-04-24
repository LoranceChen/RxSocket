package lorance.rxscoket.session

import java.net.{InetSocketAddress, SocketAddress}
import java.nio.channels.{CompletionHandler, AsynchronousSocketChannel}

import lorance.rxscoket._

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
    p.future
  }
}
