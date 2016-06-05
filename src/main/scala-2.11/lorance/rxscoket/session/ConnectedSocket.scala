package lorance.rxscoket.session

import java.nio.ByteBuffer
import java.nio.channels.{CompletionHandler, AsynchronousSocketChannel}
import java.util.concurrent.Semaphore

import lorance.rxscoket.session.exception.ReadResultNegativeException
import lorance.rxscoket._
import lorance.rxscoket.session.implicitpkg._
import rx.lang.scala.schedulers.ExecutionContextScheduler
import rx.lang.scala.{Subject, Subscription, Subscriber, Observable}

import scala.collection.mutable
import scala.concurrent.{Future, Promise}
import scala.util.{Try, Success, Failure}
import scala.concurrent.ExecutionContext.Implicits.global

//as socket name or flag
case class AddressPair(local: String, remote: String)

class ConnectedSocket(socketChannel: AsynchronousSocketChannel,
                      heartBeatsManager: HeartBeatsManager,
                      val addressPair: AddressPair) {
  private val readerDispatch = new ReaderDispatch()
  private val readSubscribes = mutable.Set[Subscriber[CompletedProto]]()

  private def append(s: Subscriber[CompletedProto]) = readSubscribes.synchronized(readSubscribes += s)
  private def remove(s: Subscriber[CompletedProto]) = readSubscribes.synchronized(readSubscribes -= s)

//  val netMsgCountBuf = new Count()
  private val closeObv = Subject[AddressPair]()
  private val readAttach = Attachment(ByteBuffer.allocate(Configration.READBUFFER_LIMIT), socketChannel)

  private[session] var heart: Boolean = false

  //a event register when socket disconnect
  val onDisconnected: Observable[AddressPair] = closeObv.observeOn(ExecutionContextScheduler(global))

  lazy val disconnect = {

    Try(socketChannel.close()) match {
      case Failure(e) => rxsocketLogger.log(s"socket close - $addressPair - excption - $e")
      case Success(_) => rxsocketLogger.log(s"socket close success - $addressPair")
    }
    heartBeatsManager.cancelTask(addressPair.remote + ".SendHeartBeat")
    heartBeatsManager.cancelTask(addressPair.remote + ".CheckHeartBeat")
    closeObv.onNext(addressPair)
    closeObv.onCompleted()
    rxsocketLogger.log(s"disconnected socket - $addressPair")
  }

  val startReading: Observable[CompletedProto] = {
    rxsocketLogger.log(s"beginReading - ", 1)
    beginReading()
    Observable.apply[CompletedProto]({ s =>
      append(s)
      s.add(Subscription(remove(s)))
    }).onBackpressureBuffer.
      observeOn(ExecutionContextScheduler(global)).doOnCompleted {
      rxsocketLogger.log("socket read - doOnCompleted")
    }
  }

  private def beginReading() = {
    def beginReadingClosure(): Unit = {
      read(readAttach).onComplete{
        case Failure(f) =>
          f match {
            case e: ReadResultNegativeException =>
              rxsocketLogger.log(s"$getClass - read finished")
              for (s <- readSubscribes) { s.onCompleted()}
            case _ =>
              rxsocketLogger.log(s"unhandle exception - $f")
              for (s <- readSubscribes) { s.onCompleted()} //exception or onCompleted
          }
        case Success(c) =>
          val src = c.byteBuffer
          rxsocketLogger.log(s"${src.position} bytes", 50, Some("read success"))
          readerDispatch.receive(src).foreach{protos =>
            rxsocketLogger.log(s"dispatched protos - ${protos.map(p => p.loaded.array().string)}", 70, Some("dispatch-protos"))
            protos.foreach{proto =>
              //filter heart beat proto
              rxsocketLogger.log(s"completed proto - $proto", 130, Some("heart-beat"))

              if(proto.uuid == 0.toByte) {
                rxsocketLogger.log(s"dispatched heart beat - $proto", 100, Some("heart-beat"))
                heart = true
              } else {
                readSubscribes.foreach { s =>
                  s.onNext(proto)
                }
              }
            }
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
    * throw WritePendingException Unchecked exception thrown when an attempt is made to write to an asynchronous socket channel and a previous write has not completed.
    */
  def send(data: ByteBuffer) = {
    val p = Promise[Unit]

    writeSemaphore.acquire()
    rxsocketLogger.log(s"ConnectedSocket send - ${session.deCode(data.array())}", 70)
    socketChannel.write(data, 1, new CompletionHandler[Integer, Int] {
      override def completed(result: Integer, attachment: Int): Unit = {
        writeSemaphore.release()
        rxsocketLogger.log(s"result - $result - count - ${count.add}", 50, Some("send completed"))
        p.trySuccess(Unit)
      }

      override def failed(exc: Throwable, attachment: Int): Unit = {
        writeSemaphore.release()
        rxsocketLogger.log(s"CompletionHandler fail - $exc")
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
          rxsocketLogger.log(s"$result", 80, Some("read completed"))
          p.trySuccess(attach)
        } else {
          disconnect
          rxsocketLogger.log(s"disconnected - result = -1")
          p.tryFailure(new ReadResultNegativeException())
        }
      }

      override def failed(exc: Throwable, attachment: Attachment): Unit = {
        rxsocketLogger.log(s"socket read I/O operations fails - $exc")
        disconnect
        p.tryFailure(exc)
      }
    }

    //todo if throw this exception does readAttach lead to memory leak
    try {
      socketChannel.read(readAttach.byteBuffer, readAttach, callback)
    } catch {
      case t: Throwable =>
        rxsocketLogger.log(s"[Throw] - $t", 0)
        throw t
    }

    p.future
  }

  /**
    * send heat beat data. disconnect socket if not get response
    * 1. send before set heart as false
    * 2. after 1 mins (or other values) check does the value is true
    */
//  private val heartLock = new AnyRef
//  private val heartData = session.enCode(0.toByte, "heart beat")
//  private val heartThread = new Thread {
//    setDaemon(true)
//
//    override def run(): Unit = {
//      while(true) {
//        heartLock.synchronized{
//          heart = false
//          println("send heart beat data")
//          send(ByteBuffer.wrap(heartData))
//          heartLock.wait(Configration.HEART_BEAT_BREAKTIME * 1000)
//          if(!heart) { //not receive response
//            println("disconnected because of no heart beat response")
//            disconnect()
//            return
//          }
//        }
//      }
//    }
//  }
//
//  heartThread.start()

  override def toString = {
    super.toString + s"-local-${socketChannel.getLocalAddress};remote-${socketChannel.getRemoteAddress}"
  }
}
