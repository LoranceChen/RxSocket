import java.nio.ByteBuffer

import lorance.rxscoket.session.ClientEntrance
import lorance.rxscoket._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.io.StdIn

object DemoClientMain extends App {
  val client = new ClientEntrance("localhost", 10001)
  val socket = client.connect

  def enCoding(msg: String) = {
    val msgBytes = msg.getBytes
    val bytes = Array[Byte](1,msgBytes.length.toByte)
    bytes ++ msgBytes
  }

  val send = socket.flatMap{s =>
    val firstMsg = enCoding("hello server!")
    val secondMsg = enCoding("北京,你好!")
    val data = ByteBuffer.wrap(firstMsg ++ secondMsg)
    s.send(data)
  }

  val reading = socket.map(_.startReading)
  reading.map{r =>
    r.subscribe{protos =>
      protos.map{ proto =>
        val context = new String(proto.loaded.array())
        log(s"get info - $context, uuid: ${proto.uuid}, length: ${proto.length}")
        context
      }
    }
  }

  def encodeFromRead = {
    val line = StdIn.readLine()
    ByteBuffer.wrap(enCoding(line))
  }

  /**
    * simulate user
    */
  while(true) {
    val line = StdIn.readLine()
    val data = ByteBuffer.wrap(enCoding(line))
    socket.flatMap{s => {
        s.send(data)
      }
    }
  }
  Thread.currentThread().join()
}
