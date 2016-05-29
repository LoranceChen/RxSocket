package lorance

import scala.collection.mutable

package object rxscoket {

  // a threshold of level filter small.
  var logLevel = 0

  var logAim = mutable.ListBuffer[String]()
  /**
    * @param level higher represent more detail message
    */
  def log(msg: Any, level: Int = 0, aim: Option[String] = None): Unit = {
    if (level <= this.logLevel || (aim.nonEmpty && logAim.contains(aim.get)))
      println(s"${Thread.currentThread.getName}:${System.currentTimeMillis()} - ${aim.map(a => s"[$a] - ").getOrElse("")}$msg")
  }

  class Count {
    private var count = 0
    private val lock = new Object()

    def get = count

    def add: Int = add(1)

    def add(count: Int): Int = {
      lock.synchronized{this.count += count}
      this.count
    }

    def dec: Int = dec(1)

    def dec(count: Int) = {
      lock.synchronized{this.count-=count}
      this.count
    }
  }
}
