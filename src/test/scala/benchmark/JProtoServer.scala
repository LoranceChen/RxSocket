package benchmark

import lorance.rxscoket._
import lorance.rxscoket.presentation.json.{IdentityTask, JProtocol}
import lorance.rxscoket.session.ServerEntrance
import net.liftweb.json.JsonAST.JObject

object JProtoServer extends App {
  rxsocketLogger.logLevel = 1
  rxsocketLogger.logAim ++= List[String]("send completed", "read success", "dispatch-protos")

  val conntected = new ServerEntrance("127.0.0.1", 10011).listen
  val readX = conntected.map(c => (c, c.startReading))

  val readerJProt = readX.map(cx => new JProtocol(cx._1, cx._2))

  case class OverviewRsp(result: Option[OverviewContent], taskId: String) extends IdentityTask
  case class OverviewContent(id: String)

//  Configration.NET_MSG_OVERLOAD = 3

  readerJProt.subscribe ( s =>
    s.jRead.subscribe{ j =>
      val jo = j.asInstanceOf[JObject]
      val tsk = jo.\("taskId").values.toString
//      log(s"get jProto - $tsk")
      s.send(OverviewRsp(Some(OverviewContent("id")), tsk))
      s.send(OverviewRsp(None, tsk))
    }
  )
  Thread.currentThread().join()
}
