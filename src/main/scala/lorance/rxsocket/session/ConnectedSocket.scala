package lorance.rxsocket.session

import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.{AsynchronousSocketChannel, ClosedChannelException, CompletionHandler, ShutdownChannelGroupException}
import java.util.concurrent.{ConcurrentLinkedQueue, Semaphore}

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
import scala.concurrent.duration._

case class AddressPair(local: InetSocketAddress, remote: InetSocketAddress)
case class OnSocketCloseMsg(addressPair: AddressPair, reason: String)

class ConnectedSocket(socketChannel: AsynchronousSocketChannel,
//                      heartBeatsManager: TaskManager,
                      val addressPair: AddressPair,
                      isServer: Boolean) {
  private val logger = LoggerFactory.getLogger(getClass)

  private val readerDispatch = new ReaderDispatch()
  private val readSubscribes = PublishSubject[CompletedProto]

  private val closePromise = Promise[OnSocketCloseMsg]()
  private val readAttach = Attachment(ByteBuffer.allocate(Configration.READBUFFER_LIMIT), socketChannel)

//  @volatile private[session] var heartMilliTime: Long = System.currentTimeMillis()

  @volatile private var socketClosed = false
  def isSocketClosed: Boolean = socketClosed

  private val socketClosedLock = new Object
  @volatile private var closeReason: String = "normal close"

  private val formerSendLock = new Object
  @volatile private var formerSendFur = Future.successful(())
//  @volatile private var formerSendTimeoutPromise = Promise[Unit]()

  //a event register when socket disconnect
  val onDisconnected: Future[OnSocketCloseMsg] = closePromise.future

  /**
    * disconnect 方法为单例模式，使用双重锁保证多线程环境下只执行一次
    * @param reason
    * @return
    */
  def disconnect(reason: String): Future[OnSocketCloseMsg] = {
    if(!socketClosed) {
      socketClosedLock.synchronized {
        if (!socketClosed) {
          socketClosed = true
          closeReason = reason
          Try {
            //Notice: it release resource, such as JProtocol class
            readSubscribes.onComplete()

            //gracefully close，会等待缓冲区的数据发送完，之后实行四次挥手
            socketChannel.shutdownInput()
            socketChannel.shutdownOutput()

            //Notice: it release resource, such as ConnectSocket class
            //todo: close socket after 3 seconds (put the task to task manager)
            //      Thread.sleep(3 * 1000)
            socketChannel.close()
          } match {
            case Failure(e) => logger.info(s"socket close - $addressPair - exception - $e")
            case Success(_) => logger.info(s"socket close success - $addressPair")
          }
          //    heartBeatsManager.cancelTask(addressPair.remote + ".SendHeartBeat")
          //    heartBeatsManager.cancelTask(addressPair.remote + ".CheckHeartBeat")
          closePromise.trySuccess(OnSocketCloseMsg(addressPair, reason))
        }
      }
    }

    closePromise.future
  }

  lazy val startReading: Observable[CompletedProto] = {
    logger.debug(s"beginReading - ")

    beginReading()

    readSubscribes
      .asyncBoundary(OverflowStrategy.BackPressure(100))
      .doOnComplete(() => logger.debug("reading completed"))
      .doOnError(e => logger.warn("reading completed with error - ", e))
  }

  private def beginReading() = {
    def beginReadingClosure(): Unit = {
      val readyFur = read(readAttach)
//      val handleTimeOut = if(isServer) {
//        readyFur.withTimeout(Configration.CHECK_HEART_BEAT_BREAKTIME seconds)
//      } else { //isClient: send a heartbeat
//        readyFur
//      }

//      handleTimeOut.onComplete{
      readyFur.onComplete{
        case Failure(f) =>
          f match {
            case e: ReadResultNegativeException =>
              logger.debug(s"read finished")
              disconnect(e.toString)
            case e =>
              logger.warn(s"socket read unhandle exception - $f")
              disconnect(e.toString)
          }
        case Success(c) =>
          val src = c.byteBuffer
          logger.trace(s"read position: ${src.position} bytes")
          readerDispatch.receive(src).foreach { protos: Vector[CompletedProto] =>
            logger.trace(s"dispatched protos - ${protos.map(p => p.loaded.array().string)}")
//            heartMilliTime = System.currentTimeMillis() //update time

            def publishProtoWithGoodHabit(leftProtos: Vector[CompletedProto]): Unit = {
              leftProtos.headOption match {
                case None => //send all proto
                  beginReadingClosure() //read socket message
                case Some(proto) => //has some proto not send complete
                  //filter heart beat proto
                  logger.trace(s"completed proto - $proto")

                  if (proto.uuid == 0.toByte) {
                    //update heart beat time
                    logger.debug(s"get heart beat - $proto")
//                    heartMilliTime = System.currentTimeMillis()

                    publishProtoWithGoodHabit(leftProtos.tail)
                  } else {
                    readSubscribes.onNext(proto).map {
                      case Continue =>
                        publishProtoWithGoodHabit(leftProtos.tail)
                        Continue
                      case Stop =>
                        Stop
                    }//(session.execution.readExecutor)
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
        }//(session.execution.readExecutor)
//          beginReadingClosure()
    }
    beginReadingClosure()
  }

  /**
    * use Promise/Future simulate a queue to do my best reduce block time.
    */
  def send(data: ByteBuffer): Future[Unit] = {
    if(socketClosed){
      Future.failed(SocketClosedException(closeReason))
    }else {
      formerSendLock.synchronized {
        // cancel wait
//        formerSendTimeoutPromise.trySuccess(Unit)

        val curFur = formerSendFur.flatMap(_ => {
          val p = Promise[Unit]()
          try {
            socketChannel.write(data, 1, new CompletionHandler[Integer, Int] {
              override def completed(result: Integer, attachment: Int): Unit = {
                //            logger.trace(s"result - $result")
//                logger.info(s"send:write result - $result")
                p.trySuccess(Unit)
              }

              override def failed(exc: Throwable, attachment: Int): Unit = {
                exc match {
                  case _: ShutdownChannelGroupException | _: ClosedChannelException =>
                    logger.info(s"send:write fail -")

                  case _ =>
                    logger.info(s"send:write fail -")
                    Unit
                }
                logger.warn(s"CompletionHandler fail - $exc")
                disconnect(exc.toString)
                p.tryFailure(exc)
              }
            })
          } catch {
            //        case err @ (_: ShutdownChannelGroupException | _: ClosedChannelException | _ @ NonFatal(e)) =>
            case NonFatal(err) =>
              logger.warn(s"send:write fail - {}", err)
//              socketClosed = true
              closePromise.tryFailure(SocketClosedException(addressPair.toString))
              p.tryFailure(err)
          }

          p.future
        }) //(session.execution.sendExecutor)

//        curFur.foreach(_ => {
//          //send heartbeat with timeout
//          val p = Promise[Unit]()
//          val f = p.future
//          f.withTimeout(Configration.SEND_HEART_BEAT_BREAKTIME).recoverWith{
//            case FutureTimeoutException =>
//              this.send(ByteBuffer.wrap(session.enCode(0.toByte, "heart beat")))
//              curFur
//          }
//
//          //update var
////          formerSendTimeoutPromise = p
//        })
//        val sendWithTimeout = curFur
//        formerSendFur = sendWithTimeout
//        sendWithTimeout
        formerSendFur = curFur
        curFur
      }
    }
  }

  private def read(readAttach: Attachment): Future[Attachment] = {
    val p = Promise[Attachment]
    if (socketClosed) {
      p.tryFailure(SocketClosedException(addressPair.toString))
    } else {
      val callback = new CompletionHandler[Integer, Attachment] {
        override def completed(result: Integer, attach: Attachment): Unit = {
          if (result != -1) {
            logger.trace(s"$result")
            p.trySuccess(attach)
          } else {
//            socketClosed = true
            logger.trace(s"disconnected - result = -1")
            disconnect("disconnected read result = -1")
            p.tryFailure(new ReadResultNegativeException())
          }
        }

        override def failed(exc: Throwable, attachment: Attachment): Unit = {
          exc match {
            case _: ShutdownChannelGroupException | _: ClosedChannelException =>
//              socketClosed = true
              Unit
            case _ =>
              Unit
          }
          logger.warn(s"socket read I/O operations fails - $exc")
          disconnect(exc.toString)
          p.tryFailure(exc)
        }
      }

      try {
        // todo: implement read data with Erlang tcp passive mode.
        // https://ninenines.eu/docs/en/ranch/1.4/guide/transports/
        socketChannel.read(readAttach.byteBuffer, readAttach, callback)
      } catch {
        case NonFatal(e) =>
          logger.warn(s"socketChannel.read fail - $e")
//          socketClosed = true
          disconnect(e.toString)
          p.tryFailure(e)
      }
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

  override val toString = {
    super.toString + s"-local-${socketChannel.getLocalAddress};remote-${socketChannel.getRemoteAddress}"
  }

}
