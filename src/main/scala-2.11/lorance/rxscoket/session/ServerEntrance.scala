package lorance.rxscoket.session

import java.net.InetSocketAddress
import java.nio.channels.{CompletionHandler, AsynchronousSocketChannel, AsynchronousServerSocketChannel}

import lorance.rxscoket._
import rx.lang.scala.Observable

import scala.concurrent.{Promise, Future}
import scala.util.{Success, Failure}
import scala.concurrent.ExecutionContext.Implicits.global

class ServerEntrance(host: String, port: Int) {
  val server: AsynchronousServerSocketChannel = {
    val server = AsynchronousServerSocketChannel.open
    val socketAddress: InetSocketAddress = new InetSocketAddress(host, port)
    val prepared = server.bind(socketAddress)
    log(s"Server is listening at - $socketAddress")
    prepared
  }

  /**
    * listen connection and emit every times connects event.
    */
  def listen: Observable[ConnectedSocket] = {
    val f = connection(server)
    Observable.apply[ConnectedSocket]({ s =>

      /**
        * It's not a recursion actually because future will return immediately.
        * this times method has return when next invoke of `connectForever` occurred.
        */
      def connectForever(f: Future[AsynchronousSocketChannel]): Unit = {
        f.onComplete {
          case Failure(e) =>
            s.onError(e)
          case Success(c) =>
            s.onNext(new ConnectedSocket(c))
            log(s"client connected")
            val nextConn = connection(server)
            connectForever(nextConn)
        }
      }
      connectForever(f)
    }).doOnCompleted {
      log("socket connection - doOnCompleted")
    }
  }

  private def connection(server: AsynchronousServerSocketChannel) = {
    val p = Promise[AsynchronousSocketChannel]
    val callback = new CompletionHandler[AsynchronousSocketChannel, AsynchronousServerSocketChannel] {
      override def completed(result: AsynchronousSocketChannel, attachment: AsynchronousServerSocketChannel): Unit = {
        log("connect - success")
        p.trySuccess(result)
      }
      override def failed(exc: Throwable, attachment: AsynchronousServerSocketChannel): Unit = {
        log("connect - failed")
        p.tryFailure(exc)
      }
    }

    server.accept(server, callback)
    p.future
  }
}
