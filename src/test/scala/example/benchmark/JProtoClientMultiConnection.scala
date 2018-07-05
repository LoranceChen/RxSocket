package example.benchmark

import java.lang.management.ManagementFactory
import java.util.concurrent.Executors

import lorance.rxsocket.presentation.json.JProtocol
import lorance.rxsocket.session.{ClientEntrance, CommActiveParser}
import monix.execution.Scheduler.Implicits.global
import monix.execution.atomic.AtomicInt
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future, Promise}
import scala.util.{Failure, Success}

object JProtoClientMultiConnectionTest extends App {

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

  case class OverviewReq(id: String)//, taskId: String = "blog/index/overview")// extends IdentityTask
//  case class OverviewRsp(result: Option[OverviewContent])//, taskId: String)// extends IdentityTask
  case class OverviewRsp(id: String)

  def testOne(testCount: Int, count: Int, glAtomCount: AtomicInt, benchmarkMaxCount: Int, beginTime: Long) = {
    val client = new ClientEntrance("localhost", 10011, () => new CommActiveParser())
    val connect = client.connect
    connect.onComplete {
      case Failure(f) => logger.info(s"connect fail - $f")
      case Success(s) => logger.info(s"connect success - $s")
    }

//    sendCompletedFur.foreach(_ => connect.foreach(_.disconnect))

    val jproto = connect.map{s => logger.info("hi strat reading"); new JProtocol(s, s.startReading)}

//    val jproto = sr.map { x => logger.info("hi strat reading"); new JProtocol(x._1, x._2) }

    def get(name: String) = {
      jproto.flatMap { s =>
        logger.info(s"s.sendWithRsp - $name")
        val rsp = s.sendWithRsp[OverviewReq, OverviewRsp](OverviewReq(name))
        rsp
      }
    }

    val atomCountWarmup = AtomicInt(1)

    logger.info(s"begin send test $testCount times for make jvm hot =============")
    val testBeginTime = System.currentTimeMillis()
    val testNumber = testCount
    for (i <- -testNumber to -1) {
//      logger.info(s"send request - ha$i")
      get(s"ha$i").foreach(x => {
        val count = atomCountWarmup.getAndIncrement()
//        logger.info(s"get response - $x, $count")
        if (count == testNumber) {
          println(s"warmup send $testNumber request-response use time total: ${System.currentTimeMillis() - testBeginTime} ms")
          println(s"warmup send $testNumber request-response use time QPS: ${testNumber * 1000 / (System.currentTimeMillis() - testBeginTime)}")
        }
      })
    }

//    Thread.sleep(1000 * 10)

    val localAtomCount = AtomicInt(1)

    /**
      * scala> (1504661141248L - 1504661140886L ) / 3000.0F
      * res2: Float = 0.12066667 (ms)
      */
    logger.info(s"begin send times  =============")
//    val beginTime = System.currentTimeMillis()
    val toNumber = count
    for (i <- 1 to toNumber) {
      //testes 100w
//      logger.info(s"send request - ha$i")
      get(s"ha$i").foreach(id => {
        val count = glAtomCount.getAndIncrement()
        val localCount = localAtomCount.getAndIncrement()
        logger.info(s"get response - $id, $count")
//        if (localCount == toNumber) {
//          println(s"send $toNumber request-response use time total: ${System.currentTimeMillis() - beginTime} ms")
//          println(s"send $toNumber request-response use time QPS: ${toNumber * 1000 / (System.currentTimeMillis() - beginTime)}")
//          println(s"current glAtomCount - $count")
//        }
        if (count == benchmarkMaxCount / 5) {
          println(s"send $count request-response use time total: ${System.currentTimeMillis() - beginTime} ms")
          println(s"send $count request-response use time QPS: ${count * 1000 / (System.currentTimeMillis() - beginTime)}")
        } else
        if (count == benchmarkMaxCount / 2) {
          println(s"send $count request-response use time total: ${System.currentTimeMillis() - beginTime} ms")
          println(s"send $count request-response use time QPS: ${count * 1000 / (System.currentTimeMillis() - beginTime)}")
        } else
        if (count == benchmarkMaxCount) {
          println(s"send $count request-response use time total: ${System.currentTimeMillis() - beginTime} ms")
          println(s"send $count request-response use time QPS: ${count * 1000 / (System.currentTimeMillis() - beginTime)}")
          sendCompletedPromise.trySuccess(Unit)
        }
      })
    }
  }


  //release socket
  val sendCompletedPromise = Promise[Unit]()
  val sendCompletedFur = sendCompletedPromise.future


  //for warmup
//  val glbWarmupAtomCount = AtomicInt(1)

//  testOne(2000, 10000, glbWarmupAtomCount, 100000, System.currentTimeMillis())

//  Thread.sleep(1000  * 10)
//  println("warmup completed")
//  Thread.sleep(1000  * 5)

  val glbAtomCount = AtomicInt(1)

  val beginTime = System.currentTimeMillis()
  val clientCount = 50
  val msgCount = 1000

  (1 to clientCount).toList.foreach(_ => {
    Future(testOne(0, msgCount, glbAtomCount, msgCount * clientCount, beginTime)){
      val cpus = Runtime.getRuntime.availableProcessors
      ExecutionContext.fromExecutor(Executors.newWorkStealingPool(cpus * 2))
    }
  })

//  demo.tool.Tool.createGcThread(1000 * 10)

  Thread.currentThread().join()
}
