package benchmark

import lorance.rxsocket.presentation.json.{IdentityTask, JProtocol}
import lorance.rxsocket.session.ServerEntrance
import monix.execution.Ack.Continue
import org.json4s.JObject
import org.slf4j.LoggerFactory
import monix.execution.Scheduler.Implicits.global

object JProtoServer extends App {
  val logger = LoggerFactory.getLogger(getClass)

  lorance.rxsocket.session.Configration.CHECK_HEART_BEAT_BREAKTIME = Int.MaxValue
  lorance.rxsocket.session.Configration.SEND_HEART_BEAT_BREAKTIME = Int.MaxValue


  val conntected = new ServerEntrance("127.0.0.1", 10011).listen
  val readX = conntected.map(c => (c, c.startReading))

  val readerJProt = readX.map(cx => new JProtocol(cx._1, cx._2))

  case class OverviewRsp(result: Option[OverviewContent], taskId: String) extends IdentityTask
  case class OverviewContent(id: String)

  readerJProt.subscribe { s =>
    s.jRead.subscribe { j =>
      val jo = j.asInstanceOf[JObject]
      val tsk = jo.\("taskId").values.toString
      logger.info(s"get data - $tsk")

      s.send(OverviewRsp(Some(OverviewContent("id")), tsk))
      logger.info(s"sent data - $tsk")
//      s.send(OverviewRsp(None, tsk))
      Continue
    }

    Continue
  }
  Thread.currentThread().join()
}
