package lorance.rxscoket.presentation.json

import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets

import lorance.rxscoket._
import lorance.rxscoket.session.{CompletedProto, ConnectedSocket}
import rx.lang.scala.Observable

import scala.concurrent.{Promise, Future}

class JProtocol(connectedSocket: ConnectedSocket, read: Observable[Vector[CompletedProto]]) {//extends ConnectedSocket {
  def send(any: Any) = {
    val bytes = JsonParse.enCode(any)
  connectedSocket.send(ByteBuffer.wrap(bytes))
  }

  /**
    * if need a response should has a taskId
    *
    * @param any
    * @tparam Result dao return json extractable class
    * @return
    */
  def sendWithResult[Result <: IdentityTask, Req <: IdentityTask](any: Req)(implicit mf: Manifest[Result]): Future[Result] = {
    val bytes = JsonParse.enCode(any)
    connectedSocket.send(ByteBuffer.wrap(bytes))
    taskResult[Result](any.taskId)
  }

  /**
    * TODO
    * Q: when gc deal with those temp observable?
    */
  private def taskResult[T <: IdentityTask](taskId: String)(implicit mf: Manifest[T]): Future[T] = {
    log(s"to taskResult - $taskId", 2)
    val p = Promise[T]
    /**
      * Vector[CompletedProto] -> Vector[T]
      * 1. Vector[CompletedProto] -> map -> Option[Vector[T]]
      * 2. Vector[Option[T]] -> filter -> Vector[T]
      */
    val tProtos = read.map { protos =>
      def containsJson(proto: CompletedProto) = if (proto.uuid == 1.toByte) Some(proto) else None
      def tryParseToJson(proto: CompletedProto) = {
        containsJson(proto).flatMap { jsonProto =>
          val jsonResult = new String(jsonProto.loaded.array(), StandardCharsets.UTF_8)
          try {
            Some(JsonParse.deCode[T](jsonResult))
          } catch {
            case e: Throwable => None
          }
        }
      }
      // make sure all elem is Json extracted T class
      protos.map(tryParseToJson).filter(_.nonEmpty).map(_.get)
    }

    val obvTask = tProtos.map(_.find(_.taskId == taskId))
    obvTask.subscribe{task =>
      if(task.nonEmpty) {
        p.trySuccess(task.get)
      }
    }

    p.future
  }
}

