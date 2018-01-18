package lorance.rxsocket.presentation.json

import monix.reactive.Observable
import org.json4s.JsonAST.JValue

import scala.concurrent.Future
import scala.util.Try

/**
  * EndPoint represent request and response
  */
trait EndPoint
case object EmptyEndPoint extends EndPoint
case class RawEndPoint(value: Try[JValue]) extends EndPoint
case class FurEndPoint(value: Future[JValue]) extends EndPoint
case class StreamEndPoint(value: Observable[JValue]) extends EndPoint
