package lorance.rxscoket.session

import java.nio.ByteBuffer
import java.nio.channels.{CompletionHandler, AsynchronousSocketChannel}
import java.util.concurrent.Semaphore
import java.util.concurrent.locks.{ReentrantLock, Lock}

import lorance.rxscoket.session.exception.ReadResultNegativeException
import lorance.rxscoket._
import lorance.rxscoket.session.implicitpkg._
import rx.lang.scala.schedulers.ExecutionContextScheduler
import rx.lang.scala.{Subscription, Subscriber, Observable}

import scala.collection.mutable
import scala.concurrent.{Future, Promise}
import scala.util.{Success, Failure}
import scala.concurrent.ExecutionContext.Implicits.global
import lorance.rxscoket.session.execution.customExecutionContent
//import lorance.rxscoket.session.execution.currentThread

class ConnectedSocket(val socketChannel: AsynchronousSocketChannel) {
  private val readerDispatch = new ReaderDispatch()
  private val readSubscribes = mutable.Set[Subscriber[Vector[CompletedProto]]]()

  private def append(s: Subscriber[Vector[CompletedProto]]) = readSubscribes.synchronized(readSubscribes += s)
  private def remove(s: Subscriber[Vector[CompletedProto]]) = readSubscribes.synchronized(readSubscribes -= s)

  //locks for send msg
//  val sendGetLock = new AnyRef
//  val sendPutLock = new AnyRef

//  val msgQueue = new MessageQueue[(ByteBuffer, Promise[Unit])](
//    Configration.NET_SENDMSG_OVERLOAD,
//    sendPutLock.synchronized(sendPutLock.notify()), //    beginGet: => Unit,
//    log("able to put net data to msg queue again"),//sendBeginPutLock.notify(), //  beginPut: => Unit,
//    throw new Exception("ready send message to lot"),//sendBlockPutLock.wait(),   //  blockPut: => Unit,
//    sendPutLock.synchronized(sendPutLock.wait())    //  blockGet: => Unit)
//  )

  /**
    * if put logic in new Thread() body, it will use new operation thread to init
    */
//  val sendQueueMsg = new Thread {
//    override def run(): Unit = {
//      log("sendQueueMsg thread name test - ", -100)
//      val curThread = currentThread
//      def sendLoop(): Unit = {
//        sendPutLock.synchronized {
//          msgQueue.get match {
//            case None => sendLoop()
//            case Some(msg) =>
//              sendMsg(msg._1).onComplete {
//                case Success(rsp) =>
//                  log("sendQueueMsg thread name should equal uppers - ", -100)
//                  msg._2.trySuccess(rsp)
//                  sendLoop()
//                case Failure(f) => throw f
//              }(curThread)//todo it not work
//          }
//        }
//      }
//      sendLoop()
//    }
//  }

//  sendQueueMsg.start()

  /**
    * if queue has space return true
    *
    * @return also consider as Unit because TCP can ensure arrive
    *         todo test server disconnect does will write success? if also success the result is meanless. we also should use addition operation ensure arrive success.
    */
//  def send(msg: ByteBuffer) = {
//    val p = Promise[Unit]
//    msgQueue.put(msg, p)
////    match {
////      case None => throw new Exception("ready send message to lot")
////      case Some(_) => Unit
////    }
//    p.future
//  }

  val netMsgCountBuf = new Count()

  val readAttach = Attachment(ByteBuffer.allocate(Configration.READBUFFER_LIMIT), socketChannel)

  //val readLocker = new AnyRef

  val dispatchThread = customExecutionContent(4)//used to wait

  //def continueRead(): Unit = readLocker.notify()

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
            netMsgCountBuf.add(protos.map(_.loaded.capacity).sum)
//            log(s"CompletedPtoro buffered count - ${netMsgCountBuf.get}", -1000)
            for (s <- readSubscribes) s.onNext(protos)
          }

          //limit read operation
//          if(netMsgCountBuf.get >= Configration.NET_MSG_OVERLOAD) {
//            log(s"hold read operation - ")
//            readLocker.wait()
//            beginReadingClosure
//          } else {
//          log(s"receive sleeping - ", -100)
//          Thread.sleep(Configration.NET_RATE)
          beginReadingClosure()
//          }
      }//(dispatchThread)
    }
    beginReadingClosure()
  }

  val count = new Count()

  val writeSemaphore = new Semaphore(1)
  /**
    * it seems NOT support concurrent write, but NOT break reading.
    * after many times test, later write request will be ignored when
    * under construct some write operation.
    *
    * use message queue to deal with send message, send message will
    * this method not support multi-thread
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
//    }
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
