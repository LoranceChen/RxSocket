package lorance.rxscoket.session

import java.nio.ByteBuffer
import java.nio.channels.{CompletionHandler, AsynchronousSocketChannel}

import lorance.rxscoket.session.exception.ReadResultNegativeException
import lorance.rxscoket._
import rx.lang.scala.Observable

import scala.concurrent.{Future, Promise}
import scala.util.{Success, Failure}
import scala.concurrent.ExecutionContext.Implicits.global

class ConnectedSocket(val socketChannel: AsynchronousSocketChannel) {
  private val readerDispatch = new ReaderDispatch()

  def disconnect(): Unit = {
    socketChannel.close()
  }

  def startReading: Observable[Vector[CompletedProto]] = {
    val readAttach = Attachment(ByteBuffer.allocate(10), socketChannel)

    Observable.apply[Vector[CompletedProto]]({ s =>
      def readForever(): Unit = read(readAttach) onComplete {
        case Failure(f) =>
          f match {
            case e: ReadResultNegativeException =>
              log(s"$getClass - read finished")
              s.onCompleted()
            case _ =>
              log(s"unhandle exception - $f")
              s.onError(f)
          }
        case Success(c) =>
          val src = c.byteBuffer
          log(s"read success - ${src.array().length} bytes")
          readerDispatch.receive(src).foreach(s.onNext)
          readForever()
      }
      readForever()
    }).doOnCompleted {
      log("read socket - doOnCompleted")
    }
  }

  /**
    * it seems NOT support concurrent write, but not break as read.
    * after many times test, later write request will be ignored when
    * under construct some write operation.
    *
    * TODO: Make a Queue to save write message, it's better then synchronized
    * those method (input Queue is less cost)
    *
    * @param data
    * @return
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
