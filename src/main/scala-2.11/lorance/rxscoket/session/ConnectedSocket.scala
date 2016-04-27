package lorance.rxscoket.session

import java.nio.ByteBuffer
import java.nio.channels.{CompletionHandler, AsynchronousSocketChannel}
import java.util.concurrent.Semaphore

import lorance.rxscoket.session.exception.ReadResultNegativeException
import lorance.rxscoket._
import lorance.rxscoket.session.implicitpkg._
import rx.lang.scala.schedulers.ExecutionContextScheduler
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

//  val netMsgCountBuf = new Count()

  val readAttach = Attachment(ByteBuffer.allocate(Configration.READBUFFER_LIMIT), socketChannel)

  def disconnect(): Unit = socketChannel.close()

  val startReading: Observable[Vector[CompletedProto]] = {
    log(s"beginReading - ", 1)
    beginReading()
    Observable.apply[Vector[CompletedProto]]({ s =>
      append(s)
      s.add(Subscription(remove(s)))
    }).onBackpressureBuffer.
      observeOn(ExecutionContextScheduler(global)).doOnCompleted {
      log("socket read - doOnCompleted")
    }
  }

  private def beginReading() = {
    def beginReadingClosure(): Unit = {
      read(readAttach).onComplete{
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
          log(s"${src.position} bytes", 50, Some("read success"))
          readerDispatch.receive(src).foreach{protos =>
            log(s"dispatched protos - ${protos.map(p => p.loaded.array().string)}", 70, Some("dispatch-protos"))
//            netMsgCountBuf.add(protos.map(_.loaded.capacity).sum)
            for (s <- readSubscribes) s.onNext(protos)
          }
          beginReadingClosure()
      }
    }
    beginReadingClosure()
  }

  private lazy val count = new Count()

  private val writeSemaphore = new Semaphore(1)

  /**
    * it seems NOT support concurrent write, but NOT break reading.
    * after many times test, later write request will be ignored when
    * under construct some write operation.
    *
    *  @throw WritePendingException Unchecked exception thrown when an attempt is made to write to an asynchronous socket channel and a previous write has not completed.
    */
  def send(data: ByteBuffer) = {
    val p = Promise[Unit]

    writeSemaphore.acquire()
    log(s"ConnectedSocket send - ${session.deCode(data.array())}", 70)
    socketChannel.write(data, 1, new CompletionHandler[Integer, Int] {
      override def completed(result: Integer, attachment: Int): Unit = {
        writeSemaphore.release()
        log(s"result - $result - count - ${count.add}", 50, Some("send completed"))
        p.trySuccess(Unit)
      }

      override def failed(exc: Throwable, attachment: Int): Unit = {
        writeSemaphore.release()
        log(s"CompletionHandler fail - $exc")
        p.tryFailure(exc)
      }
    })

    p.future
  }

  private def read(readAttach: Attachment): Future[Attachment] = {
    val p = Promise[Attachment]
    val callback = new CompletionHandler[Integer, Attachment] {
      override def completed(result: Integer, attach: Attachment): Unit = {
        if (result != -1) {
          log(s"$result", 80, Some("read completed"))
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

    //todo if throw this exception does readAttach lead to memory leak
    try {
      socketChannel.read(readAttach.byteBuffer, readAttach, callback)
    } catch {
      case t: Throwable =>
        log(s"[Throw] - $t", 0)
        throw t
    }

    p.future
  }
}
