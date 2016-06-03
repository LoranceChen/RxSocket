# RxSocket - v0.9.3
socket with reactive style.

## Why do this project?
Reactive programming has important concept of stream.It's very ease to apply data similar task, furthermore, network 
programming is designed as many layer for different data types. It's nature and useful if use Reactive Style to practice
a network data stream.At upper of TCP, I will beginning to complete basic function of Session Layer and Json Presentation 
Layer, it also allow anyone to redesign Presentation Layer easily.

## Example

### Client
See `DemoClientMainTest.scala` in test directory

#### Output
```
Thread-9: linked to server success
Thread-9: send completed result - 37
```

### Server
See `DemoServerMain.scala` in test directory

####Output
```
main: Server is listening at - localhost/127.0.0.1:10002
Thread-9: connect - success
ForkJoinPool-1-worker-13: client connected
ForkJoinPool-1-worker-13: Hi, Mike, someone connected - 
ForkJoinPool-1-worker-13: Hi, John, someone connected - 
ForkJoinPool-1-worker-13: first subscriber get protocol - hello server!
ForkJoinPool-1-worker-13: first subscriber get protocol - 北京,你好!
ForkJoinPool-1-worker-13: second subscriber get protocol - hello server!
ForkJoinPool-1-worker-13: second subscriber get protocol - 北京,你好!
```  

####JProtocol Example and Performance test
#####Server side
```
import lorance.rxscoket._
import lorance.rxscoket.presentation.json.{IdentityTask, JProtocol}
import lorance.rxscoket.session.ServerEntrance
import net.liftweb.json.JsonAST.JObject

object JProtoServer extends App {
  logLevel = -1000
  val x = logAim ++= List[String]("read success", "send completed")

  val conntected = new ServerEntrance("127.0.0.1", 10011).listen
  val readX = conntected.map(c => (c, c.startReading))

  val readerJProt = readX.map(cx => new JProtocol(cx._1, cx._2))

  case class OverviewRsp(result: Option[OverviewContent], taskId: String) extends IdentityTask
  case class OverviewContent(id: String)

  readerJProt.subscribe ( s =>
    s.jRead.subscribe{ j =>
      val jo = j.asInstanceOf[JObject]
      val tsk = jo.\("taskId").values.toString
      log(s"get jProto - $tsk")
      s.send(OverviewRsp(Some(OverviewContent("id")), tsk))
      s.send(OverviewRsp(None, tsk))
    }
  )
  Thread.currentThread().join()
}
```

#####Client side
```
import lorance.rxscoket._
import lorance.rxscoket.presentation.json.{JProtocol, IdentityTask}
import lorance.rxscoket.session.ClientEntrance
import rx.lang.scala.Observable

import scala.concurrent.{Promise, Future}
import scala.util.{Success, Failure}
import scala.concurrent.ExecutionContext.Implicits.global

object JProtoClient extends App {
  private def toFuture(observable: Observable[OverviewRsp]): Future[List[OverviewRsp]] = {
    val p = Promise[List[OverviewRsp]]
    val lst = scala.collection.mutable.ListBuffer[OverviewRsp]()
    observable.subscribe(
      s => {
        lst.synchronized(lst.+=(s))
        lorance.rxscoket.log("overview Rsp ready - " + lst.mkString("\n"), -16)
      },
      e => p.tryFailure(e),
      () => p.trySuccess(lst.toList)
    )

    p.future
  }
  case class OverviewReq(penName: String, taskId: String = "blog/index/overview") extends IdentityTask
  case class OverviewRsp(result: Option[OverviewContent], taskId: String) extends IdentityTask
  case class OverviewContent(id: String)

  logLevel = 1

  val client = new ClientEntrance("localhost", 10011)
  val connect = client.connect
  connect.onComplete{
    case Failure(f) => log(s"connect fail - $f", -10)
    case Success(s) => log(s"connect success - $s", -10)
  }

  val sr = connect.map(s => (s, s.startReading))

  val jproto = sr.map { x => log("hi strat reading"); new JProtocol(x._1, x._2) }

  def get(name: String) = {
    jproto.flatMap { s =>
      val rsp = s.sendWithResult[OverviewRsp, OverviewReq](OverviewReq(name, name), Some(x => x.takeWhile(_.result.nonEmpty)))
      toFuture(rsp)
    }
  }

  log(s"begin send 1000 times for make jvm hot =============", -15)
  for(i <- 1 to 1000) {
    get(s"ha${i}")
  }

  Thread.sleep(10000)

  log(s"begin send 1000 times  =============", -15)
  for(i <- 1001 to 20000) {
    get(s"ha${i}")
  }

  Thread.currentThread().join()
}

```

####UPDATE  
1. catch disconnected exception
2. add loop send msg simulate
3. fix Negative Exception when msg length capacity over 7 byte.
4. fix socket send message loss bug under multi-thread.
5. open limit length of load.  

2016.03.25  
1. change connect and read operation to a real observable stream  
2. test use case fix cold observable caused `ReadPendingException` by multi reading same socket.

v0.7.1 - 0.7.3
* adds json presentation extractor error log
* keep temp json task observable form leak.(can i call it will lead a gpu leak?)

v0.8.1
* fix bug: json presentation `sendWithResult` method NOT filter specify taskId.

v0.9.0
* fix bug: `ReaderDispatch` can't works in some situation...(so sorry)
* add concurrent dispatch `CompleteProto`

v0.9.1
* fix bug: `socket.write` operation NOT wait complete

v0.9.2
* use java ConcurrentHashMap at `JProtocol.sendWithResult` avoid lock data frequency.

v0.9.3
* extract Logger to `lorance.rxsocket.rxsocketLogger` class instance
* add heart beat in session

####Roadmap
* adds useful observable on special event
* handle reconnect and relative notification
* need read Queue and write Queue - ensure same request thread i/o socket with FIFO
* log method add class path: replace Int log level by readable words. Related by package, class and importance.
* to solve head-of-line blocking with multi socket
* add heart beat to test connect on works
* encapsulate taskId because it must be unique.
* add Model and Service concept to support specify Req an Rsp.

bug  
1. 发送以后立即检测不能解决被立刻接收
检测和发送是两个任务

2. 断开以后无法取消任务

###坑
如果取消一个任务必须在execute()中指定控制nextTask返回None