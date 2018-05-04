package benchmark

import lorance.rxsocket.presentation.json.JProtocol
import lorance.rxsocket.session.ClientEntrance
import org.slf4j.LoggerFactory
import monix.execution.Scheduler.Implicits.global
import monix.execution.atomic.AtomicInt
import scala.util.{Failure, Success}

object JProtoClient extends App {
  val logger = LoggerFactory.getLogger(getClass)

  lorance.rxsocket.session.Configration.CHECK_HEART_BEAT_BREAKTIME = Int.MaxValue
  lorance.rxsocket.session.Configration.SEND_HEART_BEAT_BREAKTIME = Int.MaxValue


  case class OverviewReq(penName: String)//, taskId: String = "blog/index/overview")// extends IdentityTask
  case class OverviewRsp(result: Option[OverviewContent])//, taskId: String)// extends IdentityTask
  case class OverviewContent(id: String)

  val client = new ClientEntrance("localhost", 10011)
  val connect = client.connect
  connect.onComplete{
    case Failure(f) => logger.info(s"connect fail - $f")
    case Success(s) => logger.info(s"connect success - $s")
  }

  val sr = connect.map(s => (s, s.startReading))

  val jproto = sr.map { x => logger.info("hi strat reading"); new JProtocol(x._1, x._2) }

  def get(name: String) = {
    jproto.flatMap { s =>
      val rsp = s.sendWithRsp[OverviewReq, OverviewRsp](OverviewReq(name))
      rsp
    }
  }

  val atomCount = AtomicInt(1)

  logger.info(s"begin send 1000 times for make jvm hot =============")
  for(i <- -1000 to -1) {
    get(s"ha$i").foreach(x => {
      logger.info(s"get response - $x, ${atomCount.getAndIncrement()}")
    })
  }

  Thread.sleep(1000 * 10)

  /**
    * scala> (1504661141248L - 1504661140886L ) / 3000.0F
    * res2: Float = 0.12066667 (ms)
    */
  logger.info(s"begin send times  =============")
  for(i <- 1 to 700000) {//testes 100w
    get(s"ha$i").foreach(x => {
      logger.info(s"get response - $x, ${atomCount.getAndIncrement()}")
    })
  }

  Thread.currentThread().join()
}
