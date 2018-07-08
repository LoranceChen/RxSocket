package demo.jprotoserver

import lorance.rxsocket.presentation.json.{EmptyEndPoint, EndPoint, RawEndPoint, JRouter, StreamEndPoint}
import monix.reactive.Observable
import org.json4s.DefaultFormats
import org.json4s.JsonAST._
import org.json4s.JsonDSL._
import org.slf4j.LoggerFactory

import scala.util.Success

/**
  *
  */
class ExampleRouter extends JRouter {
  private val logger = LoggerFactory.getLogger(getClass)
  implicit val formats: DefaultFormats.type = DefaultFormats

  override val jsonPath = "login"

  override def jsonRoute(protoId: String, reqJson: JValue): EndPoint = {
    logger.debug("get_json:" + reqJson)
    protoId match {
      case "LOGIN_PROTO" =>
        val LoginOrRegReq(username, password) = (reqJson \ "load").extract[LoginOrRegReq]

        val jsonRsp: JValue = if(username == "admin" && password == "admin") {
          true
        } else {
          false
        }

        RawEndPoint(Success(jsonRsp))
      case "REGISTER_PROTO" =>
        val LoginOrRegReq(username, password) = (reqJson \ "load").extract[LoginOrRegReq]

        val jsonRsp: JValue = if(username == "admin" && password == "admin") {
          ("status" -> 200) ~
          ("userId" -> "userId001")
        } else {
          ("status" -> 400) ~
          ("reason" -> "fail-reason-001")
        }

        logger.debug(s"register_rsp: $jsonRsp")
        RawEndPoint(Success(jsonRsp))
      case "POSITION_PROTO" =>
        import concurrent.duration._
        StreamEndPoint(Observable.interval(2 seconds).map(x => x: JValue))
      case undefined =>
        logger.warn(s"undefined_protoId: $undefined")
        EmptyEndPoint
    }
  }
}

case class LoginOrRegReq(userName: String, password: String)