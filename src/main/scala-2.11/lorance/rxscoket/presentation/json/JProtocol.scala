package lorance.rxscoket.presentation.json

import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit

import lorance.rxscoket._
import lorance.rxscoket.session.{CompletedProto, ConnectedSocket}
import lorance.rxscoket.session.implicitpkg._
import rx.lang.scala.{Subject, Observable}

import scala.concurrent.duration.Duration

/**
  * create a JProtocol to dispatch all json relate info bind with socket and it's read stream
  *
  * @param connectedSocket
  * @param read
  */
class JProtocol(connectedSocket: ConnectedSocket, val read: Observable[Vector[CompletedProto]]) {
//  val readHot = read.publish

  def send(any: Any) = {
    val bytes = JsonParse.enCode(any)
    connectedSocket.send(ByteBuffer.wrap(bytes))
  }

  /**
    * if need a response should has a taskId
    * todo add receive method deal with protocol
    *
    * @param any
    * @tparam Result dao return json extractable class
    * @return
    */
  def sendWithResult[Result <: IdentityTask, Req <: IdentityTask](any: Req, whileOpt: Option[Result => Boolean] = None)(implicit mf: Manifest[Result]) = {
    val bytes = JsonParse.enCode(any)
    //prepare stream before send msg
    val o = taskResult[Result](any.taskId)(whileOpt)
    connectedSocket.send(ByteBuffer.wrap(bytes))
    o
  }
//
//  def sendWithResultTest[Result <: IdentityTask, Req <: IdentityTask](any: Req)(implicit mf: Manifest[Result]) = {
//    val bytes = JsonParse.enCode(any)
//    connectedSocket.send(ByteBuffer.wrap(bytes))
//
////    val subject = Subject[Vector[CompletedProto]]()
////    val sed = read.publish
//    val x = read.map(s =>
//      log(s"sendWithResultTest - ${s.map(x => x.loaded.array().string)}")
//    )
////    sed.connect
//    x
//  }


  /**
    * TODO
    * Q: when gc deal with those temp observable?
    *
    * return: Observable[T]
    */
  private def taskResult[T <: IdentityTask](taskId: String)(whileOpt: Option[T => Boolean])(implicit mf: Manifest[T]): Observable[T] = {
    log(s"enter `taskResult` - $taskId", 2)
//    val p = Promise[T]
    /**
      * Vector[CompletedProto] -> Vector[T]
      * 1. Vector[CompletedProto] -> map -> Option[Vector[T]]
      * 2. Vector[Option[T]] -> filter -> Vector[T]
      */

    def containsJson(proto: CompletedProto) = if (proto.uuid == 1.toByte) Some(proto) else None
    def tryParseToJson(proto: CompletedProto) = {
      containsJson(proto).flatMap { jsonProto =>
        val jsonResult = jsonProto.loaded.array().string
        log(s"JProtocol taskId - $taskId" + jsonResult)
        try {
          Some(JsonParse.deCode[T](jsonResult))
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

    if (whileOpt.isEmpty) theTaskEvent
    else theTaskEvent.takeWhile(whileOpt.get)


    //    val tProtos = read.map { protos =>
//      protos.map(tryParseToJson).filter(x => x.nonEmpty).map(_.get)
//    }//.takeUntil(x => x.exists(_.taskId == taskId))// todo does it matter for memory leaking?

//    tProtos.map(_.find(_.taskId == taskId)).filter(_.isDefined).map(_.get).takeUntil(until).timeout(Duration(presentation.TIMEOUT, TimeUnit.SECONDS))
//    val r = tProtos.map(_.find(_.taskId == taskId)).filter(_.isDefined).map(_.get).timeout(Duration(presentation.TIMEOUT, TimeUnit.SECONDS))
//    r

//    obvTask.subscribe((task: Option[T]) =>
//      if (task.nonEmpty) {
//        p.trySuccess(task.get)
//      }, (error: Throwable) => error match {
//      case te: TimeoutException => log(s"wait task - $taskId timeout")
//      todo should throw out?
//    })

//    p.future
  }

  //TODO need a raw sender
//  def
}
