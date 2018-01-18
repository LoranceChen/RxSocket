package lorance.rxsocket.session

import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.{AsynchronousSocketChannel, CompletionHandler}
import java.util.concurrent.Semaphore

import org.slf4j.LoggerFactory
import lorance.rxsocket.dispatch.TaskManager
import lorance.rxsocket.session.exception.ReadResultNegativeException
import lorance.rxsocket._
import lorance.rxsocket.session.implicitpkg._
import rx.lang.scala.schedulers.ExecutionContextScheduler
import rx.lang.scala.{Observable, Subject, Subscriber, Subscription}

import scala.collection.mutable
import scala.concurrent.{Future, Promise}
import scala.util.{Failure, Success, Try}
import scala.concurrent.ExecutionContext.Implicits.global

case class AddressPair(local: InetSocketAddress, remote: InetSocketAddress)

class ConnectedSocket(socketChannel: AsynchronousSocketChannel,
                      heartBeatsManager: TaskManager,
                      val addressPair: AddressPair) {
  private val logger = LoggerFactory.getLogger(getClass)

  private val readerDispatch = new ReaderDispatch()
  private val readSubscribes = Subject[CompletedProto]

  private val closeObv = Subject[AddressPair]()
  private val readAttach = Attachment(ByteBuffer.allocate(Configration.READBUFFER_LIMIT), socketChannel)

  private[session] var heart: Boolean = false

  //a event register when socket disconnect
  val onDisconnected: Observable[AddressPair] = closeObv.observeOn(ExecutionContextScheduler(global))

  lazy val disconnect = {

    Try(socketChannel.close()) match {
      case Failure(e) => logger.info(s"socket close - $addressPair - exception - $e")
      case Success(_) => logger.info(s"socket close success - $addressPair")
    }
    heartBeatsManager.cancelTask(addressPair.remote + ".SendHeartBeat")
    heartBeatsManager.cancelTask(addressPair.remote + ".CheckHeartBeat")
    closeObv.onNext(addressPair)
    closeObv.onCompleted()
  }

  val startReading: Observable[CompletedProto] = {
    logger.debug(s"beginReading - ")

    beginReading()

    //todo handle back pressure buffer with more controller
    readSubscribes
      .onBackpressureBuffer
      .doOnCompleted(() => logger.debug("reading completed"))
      .doOnError(e => logger.warn("reading completed with error - ", e))
  }

  private def beginReading() = {
    def beginReadingClosure(): Unit = {
      read(readAttach).onComplete{
        case Failure(f) =>
          f match {
            case e: ReadResultNegativeException =>
              logger.debug(s"read finished")
              readSubscribes.onCompleted()
//              for (s <- readSubscribes) { s.onCompleted()}
            case e =>
              logger.error(s"unhandle exception - $f")
              //unexpected disconnect also seems as normal completed
              readSubscribes.onCompleted()
            //              for (s <- readSubscribes) { s.onCompleted()} //exception or onCompleted
          }
        case Success(c) =>
          val src = c.byteBuffer
          logger.trace(s"read position: ${src.position} bytes")
          readerDispatch.receive(src).foreach{protos =>
            logger.trace(s"dispatched protos - ${protos.map(p => p.loaded.array().string)}")
            protos.foreach{proto =>
              //filter heart beat proto
              logger.trace(s"completed proto - $proto")

              if(proto.uuid == 0.toByte) {
                logger.trace(s"dispatched heart beat - $proto")
                heart = true
              } else {
                readSubscribes.onNext(proto)
//                readSubscribes.foreach { s =>
//                  s.onNext(proto)
//                }
              }
            }
          }
          beginReadingClosure()
      }
    }
    beginReadingClosure()
  }

//  private lazy val count = new Count()

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
    logger.debug(s"send - {}", session.deCode(data.array()))
    socketChannel.write(data, 1, new CompletionHandler[Integer, Int] {
      override def completed(result: Integer, attachment: Int): Unit = {
        writeSemaphore.release()
        logger.trace(s"result - $result")
        p.trySuccess(Unit)
      }

      override def failed(exc: Throwable, attachment: Int): Unit = {
        writeSemaphore.release()
        logger.error(s"CompletionHandler fail - $exc", exc)
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
          logger.trace(s"$result")
          p.trySuccess(attach)
        } else {
          disconnect
          logger.trace(s"disconnected - result = -1")
          p.tryFailure(new ReadResultNegativeException())
        }
      }

      override def failed(exc: Throwable, attachment: Attachment): Unit = {
        logger.error(s"socket read I/O operations fails - $exc",exc)
        disconnect
        p.tryFailure(exc)
      }
    }

    //todo if throw this exception does readAttach lead to memory leak
    try {
      socketChannel.read(readAttach.byteBuffer, readAttach, callback)
    } catch {
      case t: Throwable =>
        logger.error(s"[Throw] - $t", t)
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
