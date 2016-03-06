import lorance.rxscoket._
import lorance.rxscoket.session.ServerEntrance

object DemoServerMain extends App {
  val server = new ServerEntrance("localhost", 10001)
  val socket = server.listen
  val read = socket.flatMap(_.startReading)

  read.subscribe{ protos =>
    protos.map{ proto =>
      val context = new String(proto.loaded.array())
      log(s"get info - $context, uuid: ${proto.uuid}, length: ${proto.length}")
      context
    }
  }

  Thread.currentThread().join()
}
