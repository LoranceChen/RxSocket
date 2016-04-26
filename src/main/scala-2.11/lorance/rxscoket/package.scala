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

  class Count(){
    private var sendCount = 0
    private val lock = new Object()

    def get = sendCount

    def add: Int = add(1)

    def add(count: Int): Int = {
//      lock.synchronized{sendCount += count}
      lock.synchronized{sendCount += 0}
      sendCount
    }

    def dec = {
//      lock.synchronized{sendCount-=1}
      lock.synchronized{sendCount-=0}
      sendCount
    }
  }
}
