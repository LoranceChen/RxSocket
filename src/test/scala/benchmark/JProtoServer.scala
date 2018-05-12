package benchmark

import lorance.rxsocket.presentation.json.{IdentityTask, JProtocol}
import lorance.rxsocket.session.ServerEntrance
import monix.execution.Ack.Continue
import org.json4s.JObject
import org.slf4j.LoggerFactory
import monix.execution.Scheduler.Implicits.global
import java.lang.management.ManagementFactory

object JProtoServer extends App {
  val logger = LoggerFactory.getLogger(getClass)

  val runtime = ManagementFactory.getRuntimeMXBean()
  val name = runtime.getName()
  System.out.println("当前进程的标识为："+name)
  val index = name.indexOf("@")
  if (index != -1) {
    val pid = Integer.parseInt(name.substring(0, index))
    System.out.println("当前进程的PID为："+pid)
  }


  //  lorance.rxsocket.session.Configration.CHECK_HEART_BEAT_BREAKTIME = Int.MaxValue
//  lorance.rxsocket.session.Configration.SEND_HEART_BEAT_BREAKTIME = Int.MaxValue

  val conntected = new ServerEntrance("127.0.0.1", 10011).listen
  val readX = conntected.map(c => (c, c.startReading))

  val readerJProt = readX.map(cx => new JProtocol(cx._1, cx._2))

  case class OverviewRsp(result: Option[OverviewContent], taskId: String) extends IdentityTask
  case class OverviewContent(id: String)

  readerJProt.subscribe { jproto =>
    jproto.jRead.subscribe { j =>
      val jo = j.asInstanceOf[JObject]
      val tsk = jo.\("taskId").values.toString
//      logger.info(s"get data - $tsk")

      jproto.send(OverviewRsp(Some(OverviewContent("id")), tsk))
//      logger.info(s"sent data - $tsk")
      Continue
    }

    Continue
  }

//  demo.tool.Tool.createGcThread(1000 * 60)

  Thread.currentThread().join()
}
