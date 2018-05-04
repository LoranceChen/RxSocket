package lorance.rxsocket.session

import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.{AsynchronousSocketChannel, ClosedChannelException, CompletionHandler, ShutdownChannelGroupException}
import java.util.concurrent.Semaphore

import org.slf4j.LoggerFactory
import lorance.rxsocket.dispatch.TaskManager
import lorance.rxsocket.session.exception.{ReadResultNegativeException, SocketClosedException}
import lorance.rxsocket._
import lorance.rxsocket.session.implicitpkg._
import monix.execution.Ack.{Continue, Stop}
import monix.reactive.{Observable, OverflowStrategy}
import monix.reactive.subjects.PublishSubject

import scala.concurrent.{Future, Promise}
import scala.util.{Failure, Success, Try}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.control.NonFatal

case class AddressPair(local: InetSocketAddress, remote: InetSocketAddress)

class ConnectedSocket(socketChannel: AsynchronousSocketChannel,
                      heartBeatsManager: TaskManager,
                      val addressPair: AddressPair) {
  private val logger = LoggerFactory.getLogger(getClass)

  private val readerDispatch = new ReaderDispatch()
  private val readSubscribes = PublishSubject[CompletedProto]

  private val closePromise = Promise[AddressPair]()
  private val readAttach = Attachment(ByteBuffer.allocate(Configration.READBUFFER_LIMIT), socketChannel)

  private[session] var heart: Boolean = false

  @volatile private var socketClosed = false

  //a event register when socket disconnect
  val onDisconnected: Future[AddressPair] = closePromise.future

  lazy val disconnect = {

    Try(socketChannel.close()) match {
      case Failure(e) => logger.info(s"socket close - $addressPair - exception - $e")
      case Success(_) => logger.info(s"socket close success - $addressPair")
    }
    heartBeatsManager.cancelTask(addressPair.remote + ".SendHeartBeat")
    heartBeatsManager.cancelTask(addressPair.remote + ".CheckHeartBeat")
    closePromise.trySuccess(addressPair)
  }

  val startReading: Observable[CompletedProto] = {
    logger.debug(s"beginReading - ")

    beginReading()

    readSubscribes
      .asyncBoundary(OverflowStrategy.Default)
      .doOnComplete(() => logger.debug("reading completed"))
      .doOnError(e => logger.warn("reading completed with error - ", e))
  }

  private def beginReading() = {
    def beginReadingClosure(): Unit = {
      read(readAttach).onComplete{
        case Failure(f) =>
          f match {
            case e: ReadResultNegativeException =>
              logger.debug(s"read finished")
              readSubscribes.onComplete()
            case e =>
              logger.error(s"unhandle exception - $f")
              //unexpected disconnect also seems as normal completed
              readSubscribes.onComplete()
          }
        case Success(c) =>
          val src = c.byteBuffer
          logger.trace(s"read position: ${src.position} bytes")
          readerDispatch.receive(src).foreach { protos: Vector[CompletedProto] =>
            logger.trace(s"dispatched protos - ${protos.map(p => p.loaded.array().string)}")

            def publishProtoWithGoodHabit(leftProtos: Vector[CompletedProto]): Unit = {
              leftProtos.headOption match {
                case None => //send all proto
                  beginReadingClosure() //read socket message
                case Some(proto) => //has some proto not send complete
                  //filter heart beat proto
                  logger.trace(s"completed proto - $proto")

                  if (proto.uuid == 0.toByte) {
                    logger.trace(s"dispatched heart beat - $proto")
                    heart = true
                  } else {
                    readSubscribes.onNext(proto).map {
                      case Continue =>
                        publishProtoWithGoodHabit(leftProtos.tail)
                        Continue
                      case Stop =>
                        Stop
                    }
                  }
              }

            }

            publishProtoWithGoodHabit(protos)
          }
//            protos.foreach{proto =>
//              //filter heart beat proto
//              logger.trace(s"completed proto - $proto")
//
//              if(proto.uuid == 0.toByte) {
//                logger.trace(s"dispatched heart beat - $proto")
//                heart = true
//              } else {
//                readSubscribes.onNext(proto)
//              }
//            }
        }
//          beginReadingClosure()
    }
    beginReadingClosure()
  }

  private val writeSemaphore = new Semaphore(1)

  /**
    * it seems NOT support concurrent write, but NOT break reading.
    * after many times test, later write request will be ignored when
    * some write operation not completed.
    *
    * WritePendingException Unchecked exception thrown when an attempt is made to write to an asynchronous socket channel and a previous write has not completed.
    */
  def send(data: ByteBuffer): Future[Unit] = {
    val p = Promise[Unit]

    //move block operation to thread pool
    Future {
      if (socketClosed) {
        p.tryFailure(SocketClosedException(addressPair.toString))
      } else {
        writeSemaphore.acquire()
        if (socketClosed) {
          p.tryFailure(SocketClosedException(addressPair.toString))
        } else {
          logger.debug(s"send - {}", session.deCode(data.array()))
          try {
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
          } catch {
            case _: ShutdownChannelGroupException | _: ClosedChannelException =>
              socketClosed = true
              closePromise.tryFailure(SocketClosedException(addressPair.toString))
              writeSemaphore.release()
            case NonFatal(e) =>
              logger.warn("send fail ", e)
              writeSemaphore.release()
          }
        }
      }
    }(execution.sendExecutor)

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
