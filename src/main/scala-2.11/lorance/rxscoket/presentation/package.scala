package lorance.rxscoket

package object presentation {
  /**
    * combine with thread and current time to identity this task
    */
  def getTaskId: String = {
    val threadName = Thread.currentThread().getName
    val timestamp = System.nanoTime()
    val id = threadName + timestamp
    log(s"taskId - $id", 10)
    id
  }

  var JPROTO_TIMEOUT = 7 //seconds
}
