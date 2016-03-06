package lorance.rxscoket.session

import java.nio.ByteBuffer
import java.nio.channels.{CompletionHandler, AsynchronousSocketChannel}

import lorance.rxscoket.session.exception.ReadResultNegativeException
import lorance.rxscoket._
import rx.lang.scala.Observable

import scala.concurrent.{Future, Promise}
import scala.util.{Success, Failure}
import scala.concurrent.ExecutionContext.Implicits.global

class ConnectedSocket(socketChannel: AsynchronousSocketChannel) {
  def disconnect(): Unit = {
    socketChannel.close()
  }

  val readerDispatch = new ReaderDispatch()

  def startReading: Observable[Vector[CompletedProto]] = {
    val readAttach = Attachment(ByteBuffer.allocate(10), socketChannel)


    Observable.apply[Vector[CompletedProto]]({ s =>
      def readForever(): Unit = read(readAttach) onComplete {
        case Failure(f) =>
          log(s"read exception - $f")
          s.onError(f)
        case Success(c) =>
          val src = c.byteBuffer
          log(s"read success - ${src.array()}")
          readerDispatch.receive(src).foreach(s.onNext)
          readForever()
      }
      readForever()
    }).doOnCompleted {
      log("read socket - doOnCompleted")
    }
  }

  def send(data: ByteBuffer) = {
    val p = Promise[Unit]
    socketChannel.write(data, 1, new CompletionHandler[Integer, Int]{
      override def completed(result: Integer, attachment: Int): Unit = {
        log(s"send result - $result")
        p.trySuccess(Unit)
      }

      override def failed(exc: Throwable, attachment: Int): Unit = {
        log(s"CompletionHandler - failed")
        p.tryFailure(exc)
      }
    })
    p.future
  }

  private def read(readAttach: Attachment): Future[Attachment] = {
    val p = Promise[Attachment]
    val callback = new CompletionHandler[Integer, Attachment] {
      override def completed(result: Integer, readAttach: Attachment): Unit = {
        if (result != -1) {
          p.trySuccess(readAttach)
        } else {
          disconnect()
          log(s"disconnected - result = -1")
          p.tryFailure(new ReadResultNegativeException())
        }
      }

      override def failed(exc: Throwable, attachment: Attachment): Unit = {
        log(s"read I/O operations fails - $exc")
        p.tryFailure(exc)
      }
    }

    socketChannel.read(readAttach.byteBuffer, readAttach, callback)
    p.future
  }
}
