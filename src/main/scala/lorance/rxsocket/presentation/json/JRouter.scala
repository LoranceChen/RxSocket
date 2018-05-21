package lorance.rxsocket.presentation.json

import org.json4s.JsonAST.{JString, JValue}


/**
  * router define how to deal with received data
  */
trait JRouter extends (JValue => EndPoint)
//{
//  val path: String
//
//  lazy val register: Unit = {
//    Router.routes += (path -> this)
//  }
//
//}

class JRouterManager {

  val routes = collection.mutable.HashMap[String, JRouter]()

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
    route(load)
  }
}

