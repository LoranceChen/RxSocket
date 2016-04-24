package lorance

package object rxscoket {

  // a threshold of level filter small.
  var logLevel = 0

  /**
    * @param level higher represent more detail message
    */
  def log(msg: String, level: Int = 0): Unit = {
    if (level <= this.logLevel) println(s"${Thread.currentThread.getName}:${System.currentTimeMillis()} - $msg")
  }
}
