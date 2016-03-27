package lorance.rxscoket.session

import java.nio.ByteBuffer
import java.nio.channels.{CompletionHandler, AsynchronousSocketChannel}

import lorance.rxscoket.session.exception.ReadResultNegativeException
import lorance.rxscoket._
import rx.lang.scala.schedulers.NewThreadScheduler
import rx.lang.scala.{Subscription, Subscriber, Observable}

import scala.collection.mutable
import scala.concurrent.{Future, Promise}
import scala.util.{Success, Failure}
import scala.concurrent.ExecutionContext.Implicits.global

class ConnectedSocket(val socketChannel: AsynchronousSocketChannel) {
  private val readerDispatch = new ReaderDispatch()
  private val readSubscribes = mutable.Set[Subscriber[Vector[CompletedProto]]]()

  private def append(s: Subscriber[Vector[CompletedProto]]) = readSubscribes.synchronized(readSubscribes += s)
  private def remove(s: Subscriber[Vector[CompletedProto]]) = readSubscribes.synchronized(readSubscribes -= s)

  def disconnect(): Unit = socketChannel.close()

  def startReading: Observable[Vector[CompletedProto]] = {
    log(s"beginReading - ", 1)
    beginReading
    Observable.apply[Vector[CompletedProto]]({ s =>
      append(s)
      s.add(Subscription(remove(s)))
    }).doOnCompleted {
      log("socket read - doOnCompleted")
    }.subscribeOn(NewThreadScheduler())
  }

  private def beginReading = {
    val readAttach = Attachment(ByteBuffer.allocate(Configration.READBUFFER_LIMIT), socketChannel)
    def beginReadingClosure: Unit = {
      read(readAttach) onComplete {
        case Failure(f) =>
          f match {
            case e: ReadResultNegativeException =>
              log(s"$getClass - read finished")
              for (s <- readSubscribes) { s.onCompleted()}
            case _ =>
              log(s"unhandle exception - $f")
              for (s <- readSubscribes) { s.onError(f)}
          }
        case Success(c) =>
          val src = c.byteBuffer
          log(s"read success - ${src.position} bytes", 2)
          readerDispatch.receive(src).foreach(protos => for (s <- readSubscribes) {s.onNext(protos)})
          beginReadingClosure
      }
    }
    beginReadingClosure
  }

  /**
    * it seems NOT support concurrent write, but NOT break reading.
    * after many times test, later write request will be ignored when
    * under construct some write operation.
    */
  def send(data: ByteBuffer) = {
    val p = Promise[Unit]
    this.synchronized {
      socketChannel.write(data, 1, new CompletionHandler[Integer, Int] {
        override def completed(result: Integer, attachment: Int): Unit = {
          log(s"send completed result - $result")
          p.trySuccess(Unit)
        }

        override def failed(exc: Throwable, attachment: Int): Unit = {
          log(s"CompletionHandler - failed")
          p.tryFailure(exc)
        }
      })
    }
    p.future
  }

  private def read(readAttach: Attachment): Future[Attachment] = {
    val p = Promise[Attachment]
    val callback = new CompletionHandler[Integer, Attachment] {
      override def completed(result: Integer, attach: Attachment): Unit = {
        if (result != -1) {
          p.trySuccess(attach)
        } else {
          disconnect()
          log(s"disconnected - result = -1")
          p.tryFailure(new ReadResultNegativeException())
        }
      }

      override def failed(exc: Throwable, attachment: Attachment): Unit = {
        log(s"socket read I/O operations fails - $exc")
        p.tryFailure(exc)
      }
    }

    socketChannel.read(readAttach.byteBuffer, readAttach, callback)
    p.future
  }
}
