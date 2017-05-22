package lorance.rxscoket.dispatch


import java.util.concurrent.ConcurrentLinkedQueue

import lorance.rxscoket._

trait CommandQueue[T] {
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
          rxsocketLogger.log(s"poll task cmd queue - $theTask", 200)

          receive(theTask)
        }
      }
    }
  }

  QueueThread.start()

  def tell(cmd: T) = {
    myQueue.add(cmd)
    rxsocketLogger.log(s"tell cmd - $cmd - current count - ${myQueue.size()}", 200)
    lock.synchronized(lock.notify())
  }

  //must sync operation
  protected def receive(t: T): Unit
}

