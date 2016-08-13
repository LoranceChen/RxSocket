package demo

import lorance.rxscoket.presentation.json.{IdentityTask, JProtocol}
import lorance.rxscoket.session.ServerEntrance
import net.liftweb.json.JsonAST.JString

/**
  * json
  */
object JProtoServer extends App{
  val socket = new ServerEntrance("127.0.0.1", 10011).listen

  val jprotoSocket = socket.map(connection => new JProtocol(connection, connection.startReading))

  case class Response(result: Option[String], taskId: String) extends IdentityTask

  jprotoSocket.subscribe ( s =>
    s.jRead.subscribe{ j =>
      println(s"GET_INFO - ${net.liftweb.json.compactRender(j)}")
      val JString(tskId) = j \ "taskId"
      s.send(Response(Some("foo"), tskId))
      s.send(Response(Some("boo"), tskId))
      s.send(Response(None, tskId))
    }
  )

  Thread.currentThread().join()
}
