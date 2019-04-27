package example.benchmark

import java.lang.management.ManagementFactory

import lorance.rxsocket.presentation.json.JProtocol
import lorance.rxsocket.session.{ClientEntrance, CommActiveParser, CommPassiveParser}
import org.slf4j.LoggerFactory
import lorance.rxsocket.execution.global
import monix.execution.atomic.AtomicInt

import scala.concurrent.Future
import scala.util.{Failure, Success}

object JProtoClient extends App {
  val logger = LoggerFactory.getLogger(getClass)

//  lorance.rxsocket.session.Configration.CHECK_HEART_BEAT_BREAKTIME = Int.MaxValue
//  lorance.rxsocket.session.Configration.SEND_HEART_BEAT_BREAKTIME = Int.MaxValue


  val runtime = ManagementFactory.getRuntimeMXBean()
  val name = runtime.getName()
  System.out.println("当前进程的标识为："+name)
  val index = name.indexOf("@")
  if (index != -1) {
    val pid = Integer.parseInt(name.substring(0, index))
    System.out.println("当前进程的PID为："+pid)
  }


  case class OverviewReq(id: Int)//, taskId: String = "blog/index/overview")// extends IdentityTask
  case class OverviewRsp(result: Option[OverviewContent])//, taskId: String)// extends IdentityTask
  case class OverviewContent(id: Int)

  val client = new ClientEntrance("localhost", 10011, () => new CommPassiveParser())

  val connect = client.connect
  connect.onComplete{
    case Failure(f) => logger.info(s"connect fail - $f")
    case Success(s) => logger.info(s"connect success - $s")
  }

  val sr = connect.map(s => (s, s.startReading))

  val jproto: Future[JProtocol] = sr.map { x => logger.info("hi strat reading"); new JProtocol(x._1, x._2) }


  def get(id: Int) = {
    jproto.flatMap { s =>
//      logger.info(s"send request - $name")
      val rsp = s.sendWithRsp[OverviewReq, OverviewRsp](OverviewReq(id))
      rsp
    }
  }

  val atomCountWarmup = AtomicInt(1)

  logger.info(s"begin send 1000 times for make jvm hot =============")
  val testBeginTime = System.currentTimeMillis()
  val testNumber = 10000
  for(i <- -testNumber to -1) {
//    logger.info(s"send request - ha$i")
    get(i)
      .foreach(x => {
      val count = atomCountWarmup.getAndIncrement()
//      logger.info(s"get response - $x, $count")
      if(count == testNumber) {
        println(s"warmup send $testNumber request-response use time total: ${System.currentTimeMillis() - testBeginTime} ms")
        println(s"warmup send $testNumber request-response use time QPS: ${testNumber * 1000 / (System.currentTimeMillis() - testBeginTime)}")
      }
    })
  }

  Thread.sleep(1000 * 10)

  val atomCount = AtomicInt(1)

  /**
    * scala> (1504661141248L - 1504661140886L ) / 3000.0F
    * res2: Float = 0.12066667 (ms)
    */
  logger.info(s"begin send times  =============")
  val beginTime = System.currentTimeMillis()
  val toNumber = 100000
  for(i <- 1 to toNumber) {
//    logger.info(s"send request - ha$i")
    get(i).foreach(x => {
      val count = atomCount.getAndIncrement()
//      logger.info(s"get response - $x, $count")
      if(count == toNumber) {
        println(s"send $toNumber request-response use time total: ${System.currentTimeMillis() - beginTime} ms")
        println(s"send $toNumber request-response use time QPS: ${toNumber * 1000 / (System.currentTimeMillis() - beginTime)}")
      }
    })
  }

  Thread.currentThread().join()
}
