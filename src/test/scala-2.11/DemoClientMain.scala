import java.nio.ByteBuffer
import lorance.rxscoket.session.ClientEntrance
import lorance.rxscoket._
import lorance.rxscoket.session._
import rx.lang.scala.Observable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.io.StdIn
//import lorance.rxscoket.session.execution.currentThread

object DemoClientMainTest extends App with DemoClientMain {
  inputLoop
  Thread.currentThread().join()
}

trait DemoClientMain {
  val client = new ClientEntrance("localhost", 10002)
  val socket = client.connect

  val reading = Observable.from(socket).flatMap(_.startReading).publish
  reading.connect

  reading.subscribe { protos =>
    protos.map { proto =>
      val context = new String(proto.loaded.array())
      log(s"get info - $context, uuid: ${proto.uuid}, length: ${proto.length}")
      context
    }
  }

  socket.flatMap{s =>
    val firstMsg = enCode(0.toByte, "hello server!")
    val secondMsg = enCode(0.toByte, "北京,你好!")

    val data = ByteBuffer.wrap(firstMsg ++ secondMsg)
    s.send(data)
  }

  /**
    * simulate application input
    */
  def inputLoop = {
    while (true) {
      log(s"input message:")
      val line = StdIn.readLine()
      val data = ByteBuffer.wrap(enCode(0.toByte, line))
      socket.flatMap { s => {
        s.send(data)
      }}
    }
  }
}
