package lorance.rxsocket.dispatch

import java.util.concurrent.ConcurrentLinkedQueue

import org.slf4j.LoggerFactory
import lorance.rxsocket._

trait CommandQueue[T] {
  private val logger = LoggerFactory.getLogger(getClass)
  val myQueue = new ConcurrentLinkedQueue[T]()
  val lock = new Object()
  object QueueThread extends Thread {
    setDaemon(true)

    override def run = {
      while(true) {
        if (myQueue.size() == 0) {
          lock.synchronized(lock.wait())
        } else {
          val theTask = myQueue.poll()
          logger.debug(s"poll task cmd queue - $theTask")

          receive(theTask)
        }
      }
    }
  }

  QueueThread.start()

  def tell(cmd: T) = {
    myQueue.add(cmd)
    logger.debug(s"tell cmd - $cmd - current count - ${myQueue.size()}")
    lock.synchronized(lock.notify())
  }

  //must sync operation
  protected def receive(t: T): Unit
}

