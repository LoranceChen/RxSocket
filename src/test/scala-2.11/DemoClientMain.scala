import java.nio.ByteBuffer
import lorance.rxscoket.session.ClientEntrance
import lorance.rxscoket._
import lorance.rxscoket.session.implicitpkg._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.io.StdIn

object DemoClientMainTest extends App with DemoClientMain {
  inputLoop
  Thread.currentThread().join()
}

trait DemoClientMain {
  val client = new ClientEntrance("localhost", 10002)
  val socket = client.connect

  val reading = socket.map(_.startReading)
  reading.map { r =>
    r.subscribe { protos =>
      protos.map { proto =>
        val context = new String(proto.loaded.array())
        log(s"get info - $context, uuid: ${proto.uuid}, length: ${proto.length}")
        context
      }
    }
  }

  socket.flatMap{s =>
    val firstMsg = enCoding("hello server!")
    val secondMsg = enCoding("北京,你好!")

    val data = ByteBuffer.wrap(firstMsg ++ secondMsg)
    s.send(data)
  }

  /**
    * simulate user
    */
  def inputLoop = {
    while (true) {
      log(s"input message:")
      val line = StdIn.readLine()
      val data = ByteBuffer.wrap(enCoding(line))
      socket.flatMap { s => {
        s.send(data)
      }
      }
    }
  }

  //helps
  def enCoding(msg: String) = {
    val msgBytes = msg.getBytes
    val length = msgBytes.length.getByteArray
    val bytes = 1.toByte +: length
    bytes ++ msgBytes
  }

  def encodeFromRead = {
    val line = StdIn.readLine()
    ByteBuffer.wrap(enCoding(line))
  }
}
