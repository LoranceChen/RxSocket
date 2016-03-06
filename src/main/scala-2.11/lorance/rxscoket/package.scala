package lorance

/**
  *
  */
package object rxscoket {
  def log(msg: String): Unit = {
    println(s"${Thread.currentThread.getName}: $msg")
  }
}
