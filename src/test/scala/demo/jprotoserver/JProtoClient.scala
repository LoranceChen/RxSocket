package demo.jprotoserver

import lorance.rxsocket.presentation.json.JProtocol
import lorance.rxsocket.session.ClientEntrance
import org.json4s.JsonAST.JValue
import rx.lang.scala.Observable

import scala.concurrent.ExecutionContext.Implicits.global

/**
  *
  */
object JProtoClient extends App {

  val client = new ClientEntrance("localhost", 10020).connect
  val jproto = client.map { x => new JProtocol(x, x.startReading) }

  RxSocketAPI.login.foreach(loginSuccess => println("loginSuccess: " + loginSuccess))
  RxSocketAPI.register.foreach(result => println("register result: " + result))
  RxSocketAPI.position.subscribe(p => println("current position - " + p))

  Thread.currentThread().join()

  object RxSocketAPI {
    def login = {
      println("do login")
      jproto.flatMap { s =>
        s.sendWithRsp[Request[LoginOrRegReq], Boolean](Request("login", "LOGIN_PROTO", LoginOrRegReq("admin", "admin")))
      }
    }

    def register = {
      println("do register")
      jproto.flatMap { s =>
        s.sendWithRsp[Request[LoginOrRegReq], JValue](Request("login", "REGISTER_PROTO", LoginOrRegReq("admin", "admin")))
      }
    }

    def position = {
      println("my current position")
      Observable.from(jproto).flatMap { s =>
        s.sendWithStream[Request[Boolean], JValue](Request("login", "POSITION_PROTO", true))
      }
    }
  }
  case class Request[T](path: String, protoId: String, load: T)

  case class Rst(status: Int, userId: String)
}
