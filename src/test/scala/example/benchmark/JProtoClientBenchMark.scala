package example.benchmark

import java.util.concurrent.Executors
import java.util.concurrent.atomic.LongAdder

import lorance.rxsocket.presentation.json.JProtocol
import lorance.rxsocket.session.{ClientEntrance, CommPassiveParser, CompletedProto, ConnectedSocket}

import scala.concurrent.{Await, ExecutionContext, ExecutionContextExecutor, Future}
import scala.concurrent.duration._

object JProtoClientBenchMark extends App {
  test()

  def test(serverAddress: String = "localhost",
           serverPort: Int = 10011,
           clientCount: Int = 2,
           concurrentCount: Int = 20,
           threads: Int = Runtime.getRuntime.availableProcessors() / 3,
           timeSeconds: Int = 10, groupCount: Int = 5) = {
    implicit val context: ExecutionContextExecutor = getThreadPool(threads)
    val longAdder = new LongAdder

    val connections: List[ConnectedSocket[CompletedProto]] =
      Await.result(
        Future.sequence(genConnections(serverAddress, serverPort, clientCount)),
        10 seconds
      )

    val jprotocols: List[JProtocol] = connections.map(x => {
      new JProtocol(x, x.startReading)
    })


    (1 to groupCount).toList.foreach(_ => {
      val beginTime = System.currentTimeMillis()
      val endTime = beginTime + timeSeconds * 1000L

      println("begin time: " + beginTime)
      //send forever
      (0 until concurrentCount).toList.foreach(number => {
        val jprotocol: JProtocol = jprotocols(number % clientCount)
        sendMsgForever(jprotocol, number, longAdder)
      })

      Thread.sleep(endTime - System.currentTimeMillis())
      val qps = longAdder.sum() / timeSeconds
      println("QPS: " + qps)
      longAdder.reset()
    })

  }

  private def sendMsgForever(jprotocol: JProtocol, id: Int, longAdder: LongAdder)(implicit context: ExecutionContextExecutor): Unit = {
    val a: Future[OverviewRsp] = jprotocol.sendWithRsp[OverviewReq, OverviewRsp](OverviewReq(id))
    a.map(_ => {
      longAdder.increment()
      sendMsgForever(jprotocol, id, longAdder)
    })
  }

  private def getThreadPool(threadCount: Int) = {
    ExecutionContext.fromExecutor(Executors.newWorkStealingPool(threadCount))
  }

  private def genConnections(serverAddress: String,
                             serverPort: Int,
                             count: Int) = {
    (1 to count).map(_ =>
      genConnection(serverAddress, serverPort).connect
    ).toList
  }
  private def genConnection(serverAddress: String, serverPort: Int) = {
    val client = new ClientEntrance(serverAddress, serverPort, () => new CommPassiveParser())
    client
  }

  case class OverviewReq(id: Int)//, taskId: String = "blog/index/overview")// extends IdentityTask
  case class OverviewRsp(result: Option[OverviewContent])//, taskId: String)// extends IdentityTask
  case class OverviewContent(id: Int)
}

