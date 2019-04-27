package example.benchmark

import lorance.rxsocket.presentation.json.JProtocol
import lorance.rxsocket.session.{CommActiveParser, CommPassiveParser, ServerEntrance}
import monix.execution.Ack.Continue
import org.slf4j.LoggerFactory
import lorance.rxsocket.execution.global
import java.lang.management.ManagementFactory

object EchoJProtoServer extends App {
  val logger = LoggerFactory.getLogger(getClass)

  val runtime = ManagementFactory.getRuntimeMXBean()
  val name = runtime.getName()
  System.out.println("当前进程的标识为："+name)
  val index = name.indexOf("@")
  if (index != -1) {
    val pid = Integer.parseInt(name.substring(0, index))
    System.out.println("当前进程的PID为："+pid)
  }

  logger.error("hi logger")
  //  lorance.rxsocket.session.Configration.CHECK_HEART_BEAT_BREAKTIME = Int.MaxValue
  //  lorance.rxsocket.session.Configration.SEND_HEART_BEAT_BREAKTIME = Int.MaxValue

  //  val conntected = new ServerEntrance("127.0.0.1", 10011, new CommActiveParser()).listen
  val conntected = new ServerEntrance("127.0.0.1", 10011, () => new CommActiveParser()).listen
  val readX = conntected.map(c => (c, c.startReading))

  val readerJProt = readX.map(cx => new JProtocol(cx._1, cx._2))

  case class OverviewRsp(id: Int, taskId: String)

  readerJProt.subscribe { jproto =>
    jproto.jRead.subscribe { j =>
      //echo
      logger.info(s"get and ready send data - $j")
      jproto.sendRaw(j).flatMap(_ => Continue)
    }

    Continue
  }

  //  demo.tool.Tool.createGcThread(1000 * 60)

  Thread.currentThread().join()
}
