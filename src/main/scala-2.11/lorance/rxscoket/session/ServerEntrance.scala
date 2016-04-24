package lorance.rxscoket.session

import java.net.InetSocketAddress
import java.nio.channels.{CompletionHandler, AsynchronousSocketChannel, AsynchronousServerSocketChannel}

import lorance.rxscoket._
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

  val server: AsynchronousServerSocketChannel = {
    val server = AsynchronousServerSocketChannel.open
    val socketAddress: InetSocketAddress = new InetSocketAddress(host, port)
    val prepared = server.bind(socketAddress)
    log(s"Server is prepare listen at - $socketAddress")
    prepared
  }

  /**
    * listen connection and emit every times connects event.
    */
  def listen: Observable[ConnectedSocket] = {
    log("listen begin - ", 1)
    connectForever

    Observable.apply[ConnectedSocket]({ s =>
      append(s)
      s.add(Subscription(remove(s)))
    }).doOnCompleted {
      log("socket connection - doOnCompleted")
    }
  }

  private def connectForever = {
    log("connect loop begin -", 1)
    val f = connection(server)

    def connectForeverHelper(f: Future[AsynchronousSocketChannel]): Unit = {
      f.onComplete {
        case Failure(e) =>
          for(s <- connectionSubs) {s.onError(e)}
        case Success(c) =>
          log(s"client connected")
          val connectedSocket = new ConnectedSocket(c)
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
