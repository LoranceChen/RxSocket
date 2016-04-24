package lorance

import scala.collection.mutable

package object rxscoket {

  // a threshold of level filter small.
  var logLevel = 0

  var logAim = mutable.ListBuffer[String]()
  /**
    * @param level higher represent more detail message
    */
  def log(msg: String, level: Int = 0, aim: Option[String] = None): Unit = {
    if (level <= this.logLevel || (aim.nonEmpty && logAim.contains(aim.get)))
      println(s"${Thread.currentThread.getName}:${System.currentTimeMillis()} - ${aim.map(a => s"[$a] - ").getOrElse("")}$msg")
  }
}
