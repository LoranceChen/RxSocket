package demo.jprotoserver

import lorance.rxsocket.presentation.json.{JProtoServer, JProtocol, Router}
import lorance.rxsocket.session.{ConnectedSocket, ServerEntrance}
import monix.reactive.Observable
import org.slf4j.LoggerFactory


/**
  *
  */
object AdvanceJProtoServer extends App {
  val routes = List(new ExampleRouter)
  new SimpleServer("localhost", 10020, routes)

  Thread.currentThread().join()

}


/**
  * simple warp for `JProtoServer`
  */
class SimpleServer(host: String, port: Int, routes: List[Router]) {
  val logger = LoggerFactory.getLogger(getClass)
  //socket init
  val connectedStream: Observable[ConnectedSocket] = new ServerEntrance(host, port).listen
  val jsonProtoStream: Observable[JProtocol] = connectedStream.map(c =>  new JProtocol(c, c.startReading))

  //register service
  val jProtoServer = new JProtoServer(jsonProtoStream, routes)

}