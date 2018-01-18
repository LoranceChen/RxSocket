package demo.jprotoserver

import lorance.rxsocket.presentation.json.{JProtoServer, JProtocol, Router}
import lorance.rxsocket.session.ServerEntrance
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
  val conntected = new ServerEntrance(host, port).listen
  val readX = conntected.map(c => (c, c.startReading))

  val readerJProt: Observable[JProtocol] = readX.map(cx => new JProtocol(cx._1, cx._2))

  //register service
  val jProtoServer = new JProtoServer(readerJProt, routes)

}