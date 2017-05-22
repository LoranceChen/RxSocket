package lorance

import scala.collection.mutable

package object rxscoket {

  var rxsocketLogger = ConsoleLog()

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
