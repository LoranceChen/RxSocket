package demo

import lorance.rxscoket.presentation.json.{IdentityTask, JProtocol}
import lorance.rxscoket.session.ServerEntrance
import net.liftweb.json.JsonAST.JString

/**
  * Json presentation Example
  */
object JProtoServer extends App{
  val socket = new ServerEntrance("127.0.0.1", 10011).listen

  val jprotoSocket = socket.map(connection => new JProtocol(connection, connection.startReading))

  case class Response(result: Option[String], taskId: String) extends IdentityTask

  jprotoSocket.subscribe ( s =>
    s.jRead.subscribe{ j =>
      println(s"GET_INFO - ${net.liftweb.json.compactRender(j)}")
      val JString(tskId) = j \ "taskId" //assume has taskId for simplify
      //send multiple msg with same taskId as a stream
      s.send(Response(Some("foo"), tskId))
      s.send(Response(Some("boo"), tskId))
      s.send(Response(None, tskId))
    }
  )

  Thread.currentThread().join()
}

/**
OUTPUT:
Thread-11:1471133677663 - connect - success
GET_INFO - {"accountId":"admin","taskId":"ForkJoinPool-1-worker-9197464411151476"}
*/