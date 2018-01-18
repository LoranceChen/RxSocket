package benchmark

import lorance.rxsocket.presentation.json.JProtocol
import lorance.rxsocket.session.ClientEntrance
import monix.execution.Ack.Continue
import monix.reactive.Observable
import org.slf4j.LoggerFactory

import monix.execution.Scheduler.Implicits.global
import scala.concurrent.{Future, Promise}
import scala.util.{Failure, Success}

object JProtoClient extends App {
  val logger = LoggerFactory.getLogger(getClass)
  private def toFuture(observable: Observable[OverviewRsp]): Future[List[OverviewRsp]] = {
    val p = Promise[List[OverviewRsp]]
    val lst = scala.collection.mutable.ListBuffer[OverviewRsp]()
    observable.subscribe(
      s => {
        lst.synchronized(lst.+=(s))
        logger.info(s"overview Rsp ready - " + lst.mkString("\n"))
        Continue
      },
      e => p.tryFailure(e),
      () => p.trySuccess(lst.toList)
    )

    p.future
  }
  case class OverviewReq(penName: String)//, taskId: String = "blog/index/overview")// extends IdentityTask
  case class OverviewRsp(result: Option[OverviewContent])//, taskId: String)// extends IdentityTask
  case class OverviewContent(id: String)

  val client = new ClientEntrance("localhost", 10011)
  val connect = client.connect
  connect.onComplete{
    case Failure(f) => logger.info(s"connect fail - $f", -10)
    case Success(s) => logger.info(s"connect success - $s", -10)
  }

  val sr = connect.map(s => (s, s.startReading))

  val jproto = sr.map { x => logger.info("hi strat reading"); new JProtocol(x._1, x._2) }

  def get(name: String) = {
    jproto.flatMap { s =>
//      val rsp = s.sendWithResult[OverviewRsp, OverviewReq](OverviewReq(name, name), Some((x: Observable[OverviewRsp]) => x.takeWhile(_.result.nonEmpty)))
      val rsp = s.sendWithStream[OverviewReq, OverviewRsp](OverviewReq(name), Some((x: Observable[OverviewRsp]) => x.takeWhile(_.result.nonEmpty)))
      toFuture(rsp)
    }
  }

  def justSend(penName: String) = {
    jproto.flatMap { s =>
      s.send(OverviewReq(penName))
    }
  }

  logger.info(s"begin send 1000 times for make jvm hot =============", -15)
  for(i <- -1000 to -1) {
    get(s"ha${i}")
//    justSend(s"ha${i}")
  }

  Thread.sleep(1000 * 10)

  /**
    * scala> (1504661141248L - 1504661140886L ) / 3000.0F
    * res2: Float = 0.12066667 (ms)
    */
  logger.info(s"begin send times  =============", -15)
  for(i <- 1 to 100000) {
    get(s"ha${i}")
//    justSend(s"ha${i}")
  }

//  var str = new StringBuilder()
//  for( i <- 1 to 1024000) yield {
//    str.append("1")
//  }
//
//  get(str.toString)

  Thread.currentThread().join()
}
