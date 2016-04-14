package lorance.rxscoket.presentation.json

import java.nio.ByteBuffer
import java.util.concurrent.TimeUnit

import lorance.rxscoket._
import lorance.rxscoket.session.{CompletedProto, ConnectedSocket}
import lorance.rxscoket.session.implicitpkg._
import rx.lang.scala.Observable

import scala.concurrent.duration.Duration

/**
  * create a JProtocol to dispatch all json relate info bind with socket and it's read stream
  */
class JProtocol(connectedSocket: ConnectedSocket, val read: Observable[Vector[CompletedProto]]) {
//  val readHot = read.publish

  def send(any: Any) = {
    val bytes = JsonParse.enCode(any)
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

    def containsJson(proto: CompletedProto) = if (proto.uuid == 1.toByte) Some(proto) else None
    def tryParseToJson(proto: CompletedProto) = {
      containsJson(proto).flatMap { jsonProto =>
        val jsonResult = jsonProto.loaded.array().string
        try {
          import net.liftweb.json._
          (parse(jsonResult) \ "taskId").values match {
            case task: String if task == taskId =>
            log(s"JProtocol taskId - $taskId, loaded - $jsonResult")
            Some(JsonParse.deCode[T](jsonResult))
            case _ => None
          }
        } catch {
          case e: Throwable =>
            //todo throw a special exception
            log(s"find taskId but can't extract it, $jsonResult, to class - ${mf.runtimeClass}", 3)
            None
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
