package lorance.rxsocket.presentation.json

import org.json4s.JsonAST.{JString, JValue}

import scala.collection.mutable


/**
  * router define how to deal with received data
  */
trait JRouter {
  val path: String

  def jsonRoute(jValue: JValue): EndPoint

}

class JRouterManager {

  val routes: mutable.HashMap[String, JRouter] = collection.mutable.HashMap[String, JRouter]()

  /**
    * one request map to a observable stream
    * load protocol, eg:
    * {
    *   path: "Login",
    *   protoId: "LOGIN_PROTO",
    *   load: {
    *     ...
    *   }
    * }
    * @param load: message client send here
    * @return
    */
  def dispatch(load: JValue): EndPoint = {
    val JString(path) = load \ "path"

    val route = routes(path)
    route.jsonRoute(load)
  }
}

