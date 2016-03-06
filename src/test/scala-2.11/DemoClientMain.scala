import java.nio.ByteBuffer

import lorance.rxscoket.session.ClientEntrance
import lorance.rxscoket._
import scala.concurrent.ExecutionContext.Implicits.global

object DemoClientMain extends App {
  val client = new ClientEntrance("localhost", 10001)
  val socket = client.connect

  val send = socket.flatMap{s =>
    def enCoding(msg: String) = {
      val msgBytes = msg.getBytes
      val bytes = Array[Byte](1,msgBytes.length.toByte)
      bytes ++ msgBytes
    }
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

  Thread.currentThread().join()
}
