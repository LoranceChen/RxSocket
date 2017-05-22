package lorance.rxscoket

package object presentation {

  /**
    * combine with thread and current time to identity this task
    */
  def getTaskId: String = {
    val threadId = Thread.currentThread().getId
    val nanoTime = System.nanoTime()
    nanoTime + "-" + threadId
  }

  var JPROTO_TIMEOUT = 30 //seconds
}
