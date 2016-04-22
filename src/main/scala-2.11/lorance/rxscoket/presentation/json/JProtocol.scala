package lorance.rxscoket.presentation.json

import java.nio.ByteBuffer
import java.util.concurrent.TimeUnit

import lorance.rxscoket._
import lorance.rxscoket.session.{CompletedProto, ConnectedSocket}
import lorance.rxscoket.session.implicitpkg._
import net.liftweb.json.JsonAST.JValue
import net.liftweb.json.JsonParser.ParseException
import rx.lang.scala.Observable

import scala.concurrent.duration.Duration
import net.liftweb.json._

/**
  * create a JProtocol to dispatch all json relate info bind with socket and it's read stream
  */
class JProtocol(connectedSocket: ConnectedSocket, val read: Observable[Vector[CompletedProto]]) {
  def send(any: Any) = {
    val bytes = JsonParse.enCode(any)
    connectedSocket.send(ByteBuffer.wrap(bytes))
  }

  def send(jValue: JValue) = {
    val bytes = JsonParse.enCode(jValue)
    connectedSocket.send(ByteBuffer.wrap(bytes))
  }

  /**
    * if need a response take taskId please
    *
    * @tparam Result return json extractable class
    * @return
    */
  def sendWithResult[Result <: IdentityTask, Req <: IdentityTask]
                    (any: Req, additional: Option[Observable[Result] => Observable[Result]])
                    (implicit mf: Manifest[Result]) = {
    val bytes = JsonParse.enCode(any)

    //prepare stream before send msg
    val o = taskResult[Result](any.taskId, additional)
    connectedSocket.send(ByteBuffer.wrap(bytes))
    o
  }

  /**
    * Q: when gc deal with those temp observable?
    * A: Yes, because we use whileOpt parameter and timeout limit.It will become non-refer.
    * return: Observable[T]
    */
  private def taskResult[T <: IdentityTask](taskId: String,
                                            additional: Option[Observable[T] => Observable[T]])
                                           (implicit mf: Manifest[T]): Observable[T] = {
    log(s"enter `taskResult` - $taskId", 15)

    def jsonLoadOpt(proto: CompletedProto) = if (proto.uuid == 1.toByte) {
      try{
        Some(parse(proto.loaded.array().string))
      } catch {
        case e: ParseException => None
      }
    } else None

    def tryParseToJson(proto: CompletedProto) = {
      jsonLoadOpt(proto).flatMap { jsonProto =>
        (jsonProto \ "taskId").values match {
          case task: String if task == taskId =>
            log(s"JProtocol taskId - $taskId, loaded - $jsonProto")
            try {
              Some(JsonParse.deCode[T](jsonProto))
            } catch {
              case e: MappingException =>
                log(s"find taskId but can't extract it, $jsonProto, to class - ${mf.runtimeClass}", 3)
                None
            }
          case _ => None
        }
      }
    }

    val theTaskEvent = read.flatMap(s => Observable.from(s)).
      map(tryParseToJson).
      filter(x => x.nonEmpty).
      map(_.get).
      timeout(Duration(presentation.TIMEOUT, TimeUnit.SECONDS))

    if(additional.isEmpty) theTaskEvent
    else additional.get(theTaskEvent)
  }
}
