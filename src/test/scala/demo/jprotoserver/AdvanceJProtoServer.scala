package demo.jprotoserver

import demo.tool.Tool
import lorance.rxsocket.presentation.json.{JProtoServer, JProtocol, JRouter}
import lorance.rxsocket.session._
import monix.reactive.Observable
import org.slf4j.LoggerFactory

/**
  *
  */
object AdvanceJProtoServer extends App {
  Tool.showPid

  val routes = List(
    new ExampleRouter,

  )

  new SimpleServer("localhost", 10020, routes)

//  Tool.createGcThread(1000 * 10)
  Thread.currentThread().join()

}


/**
  * simple warp for `JProtoServer`
  */
class SimpleServer(host: String, port: Int, routes: List[JRouter]) {
  val logger = LoggerFactory.getLogger(getClass)
  @volatile var sub1: Observable[Unit] = null

  //socket init
  val connectedStream: Observable[ConnectedSocket[CompletedProto]] = new ServerEntrance(host, port, () => new CommPassiveParser()).listen
  val jsonProtoStream: Observable[JProtocol] = connectedStream.map(c =>  {
    new JProtocol(c, c.startReading)
  })

  //register service
  val jProtoServer = new JProtoServer(jsonProtoStream, routes)

}