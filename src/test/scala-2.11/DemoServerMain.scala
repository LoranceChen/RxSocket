import lorance.rxscoket._
import lorance.rxscoket.session.exception.ReadResultNegativeException
import lorance.rxscoket.session.{CompletedProto, ConnectedSocket, ServerEntrance}
import rx.lang.scala.Observer
import scala.collection.mutable

object DemoServerMain extends App {
  val server = new ServerEntrance("localhost", 10002)
  val socket = server.listen

  /**
    * read obs
    */
  val read = socket.flatMap(_.startReading)

  val readSub = new Observer[Vector[CompletedProto]] {
    override def onNext(protos: Vector[CompletedProto]) = {
      protos.map{ proto =>
        val context = new String(proto.loaded.array())
        log(s"get info - $context, uuid: ${proto.uuid}, length: ${proto.length}")
        context
      }
    }

    override def onCompleted() = log(s"No more read.")
  }

  val onRead = read.subscribe(readSub)

  val readWithSSocketSub = read
  /**
    * read with this socket obs
    */
//  val socketWithRead = socket.flatMap{s =>  s.startReading.map{r => (s, r)}}
//  socketWithRead.subscribe{ sAndR =>
//    log(s"connected address - ${sAndR._1.socketChannel.getRemoteAddress}. " +
//      s"read info - ${sAndR._2.map{proto => new String(proto.loaded.array())}}")
//  }

  //TODO how to create read with socket combined stream, meanwhile, we can create a stream with read only
  val sub = new Observer[ConnectedSocket] {
    override def onNext(c: ConnectedSocket) = {

    }
  }

  Thread.currentThread().join()
}
