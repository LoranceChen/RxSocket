package demo

import java.lang.management.ManagementFactory

import lorance.rxsocket.presentation.json.{IdentityTask, JProtocol}
import lorance.rxsocket.session.{CommActiveParser, CommPassiveParser, ServerEntrance}
import monix.execution.Ack.Continue
import org.json4s.JsonAST.JString
import org.json4s.native.JsonMethods._
import lorance.rxsocket.execution.global

/**
  * Json presentation Example
  */
object SimpleJProtoServer extends App {


  val runtime = ManagementFactory.getRuntimeMXBean()
  val name = runtime.getName()
  System.out.println("当前进程的标识为："+name)
  val index = name.indexOf("@")
  if (index != -1) {
    val pid = Integer.parseInt(name.substring(0, index))
    System.out.println("当前进程的PID为："+pid)
  }


  val socket = new ServerEntrance("127.0.0.1", 10011, () => new CommPassiveParser()).listen

  val jprotoSocket = socket.map(connection => new JProtocol(connection, connection.startReading))

  case class Response(load: Load, taskId: String)
  case class Load(result: Option[String])

  val  x = jprotoSocket.subscribe { s =>
    val xx =  s.jRead.subscribe{ j =>
      println(s"GET_INFO - ${ compact(render(j))}")
      Thread.sleep(1000)
      val JString(tskId) = j \ "taskId" //assume has taskId for simplify
      //send multiple msg with same taskId as a stream
      s.sendRaw(Response(Load(Some("foo")), tskId))
//      s.send(Response(Some("boo"), tskId))
      s.sendRaw(Response(Load(None), tskId))
      Continue
    }

    s.connectedSocket.onDisconnected.foreach(_ => xx.cancel() )
    Continue
  }

  object GcThread extends Thread {
    override def run(): Unit = {
      while(true) {
        Thread.sleep(10000)
        println("do gc")
        System.gc()

      }
    }
  }

  GcThread.start()

  Thread.currentThread().join()
}

/**
OUTPUT:
Thread-11:1471133677663 - connect - success
GET_INFO - {"accountId":"admin","taskId":"ForkJoinPool-1-worker-9197464411151476"}
*/