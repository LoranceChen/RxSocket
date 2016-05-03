package lorance.rxscoket

package object presentation {

  /**
    * combine with thread and current time to identity this task
    */
  def getTaskId: String = {
    val threadName = Thread.currentThread().getName
    val nanoTime = System.nanoTime()
    threadName + nanoTime
  }

  var JPROTO_TIMEOUT = 8 //seconds
}
