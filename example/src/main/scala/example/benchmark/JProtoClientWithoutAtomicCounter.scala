package example.benchmark

import java.lang.management.ManagementFactory

import lorance.rxsocket.presentation.json.JProtocol
import lorance.rxsocket.session.{ClientEntrance, CommActiveParser}
import lorance.rxsocket.execution.global
import org.slf4j.LoggerFactory

import scala.util.{Failure, Success}

/**
  * different with JProtoClient, there is not Atomic count
  */
object JProtoClientWithoutAtomicCounter extends App {
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


  case class OverviewReq(id: Int)
  case class OverviewRsp(id: Int)

  val client = new ClientEntrance("localhost", 10011, () => new CommActiveParser())
  val connect = client.connect
  connect.onComplete{
    case Failure(f) => logger.info(s"connect fail - $f")
    case Success(s) => logger.info(s"connect success - $s")
  }

  val sr = connect.map(s => (s, s.startReading))

  val jproto = sr.map { x => logger.info("hi strat reading"); new JProtocol(x._1, x._2) }

  def get(id: Int) = {
    jproto.flatMap { s =>
//      logger.info(s"send request - $name")
      val rsp = s.sendWithRsp[OverviewReq, OverviewRsp](OverviewReq(id))
      rsp
    }
  }

  logger.info(s"begin send 1000 times for make jvm hot =============")
  val testBeginTime = System.currentTimeMillis()
  val testNumber = 10000
  for(i <- -testNumber to -1) {
//    logger.info(s"send request - ha$i")
    get(i)
      .foreach(x => {
//      val count = atomCountWarmup.getAndIncrement()
//      logger.info(s"get response - $x, $count")
      if(x.id == testNumber) {
        println(s"warmup send $testNumber request-response use time total: ${System.currentTimeMillis() - testBeginTime} ms")
        println(s"warmup send $testNumber request-response use time QPS: ${testNumber * 1000 / (System.currentTimeMillis() - testBeginTime)}")
      }
    })
  }

  Thread.sleep(1000 * 10)


  /**
    * Some result example:
    *
    * send 1000000 request-response use time total: 69532 ms
    * send 1000000 request-response use time QPS: 14381
    *
    * send with Active mode:
    * send 500000 request-response use time total: 31140 ms
    * send 500000 request-response use time QPS: 16054
    *
    * send with Passive mode:
    * send 500000 request-response use time total: 33095 ms
    * send 500000 request-response use time QPS: 15106
    */
  logger.info(s"begin send times  =============")
  val beginTime = System.currentTimeMillis()
  val toNumber = 500000
  for(i <- 1 to toNumber) {
//    logger.info(s"send request - ha$i")
    get(i).foreach(x => {
//      logger.info(s"get response - $x, $count")
      if(x.id == toNumber) {
        println(s"send $toNumber request-response use time total: ${System.currentTimeMillis() - beginTime} ms")
        println(s"send $toNumber request-response use time QPS: ${toNumber * 1000 / (System.currentTimeMillis() - beginTime)}")
      }
    })
  }

  Thread.currentThread().join()
}
