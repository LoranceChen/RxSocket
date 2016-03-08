import lorance.rxscoket._
import lorance.rxscoket.session.exception.ReadResultNegativeException
import lorance.rxscoket.session.{CompletedProto, ConnectedSocket, ServerEntrance}
import rx.lang.scala.Observer
import scala.collection.mutable

object DemoServerMain extends App {
  val server = new ServerEntrance("localhost", 10001)
  val socket = server.listen
  val read = socket.flatMap(_.startReading)

  val readObs = new Observer[Vector[CompletedProto]] {
    override def onNext(protos: Vector[CompletedProto]) = {
      protos.map{ proto =>
        val context = new String(proto.loaded.array())
        log(s"get info - $context, uuid: ${proto.uuid}, length: ${proto.length}")
        context
      }
    }

    override def onError(e: Throwable): Unit = {
      e match {
        case n: ReadResultNegativeException =>
          log(s"read exception on subscribe - $n!")
        case _ => log(s"unhandle exception")
      }
    }

    override def onCompleted() = log(s"No more read.")
  }

  val onRead = read.subscribe(readObs)

  val sub = new Observer[ConnectedSocket] {
    override def onNext(c: ConnectedSocket) = {

    }
  }

  Thread.currentThread().join()
}
