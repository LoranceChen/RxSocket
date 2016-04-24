package lorance.rxscoket.presentation.json

import java.nio.ByteBuffer
import java.util.concurrent.TimeUnit

import lorance.rxscoket._
import lorance.rxscoket.session.{CompletedProto, ConnectedSocket}
import lorance.rxscoket.session.implicitpkg._
import net.liftweb.json.JsonAST.JValue
import net.liftweb.json.JsonParser.ParseException
import rx.lang.scala.Observable
import rx.lang.scala.schedulers.ExecutionContextScheduler

import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.duration.Duration
import net.liftweb.json._

/**
  * create a JProtocol to dispatch all json relate info bind with socket and it's read stream
  */
class JProtocol(connectedSocket: ConnectedSocket, read: Observable[Vector[CompletedProto]]) {
  val jRead = read.flatMap { cps =>
    val jsonProto = cps.filter(_.uuid == 1.toByte)
    val x = Observable.from(
      jsonProto.map{cp =>
//        log("jread - ", -20)
        parseOpt(cp.loaded.array.string)}.
        filter(_.nonEmpty).
        map(_.get)
//        map{x =>
//          (x.get, try{Some((x.get \ "taskId").values.asInstanceOf[String] )} catch {case e : Throwable => None})}
    )//.observeOn(ExecutionContextScheduler(global))//.onBackpressureBuffer//.publish
//    x.connect
    x
  }

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
    /**
      * Q: when gc deal with those temp observable?
      * A: Yes, because we use whileOpt parameter and timeout limit.It will become non-refer.
      * return: Observable[T]
      */
    def taskResult[T <: IdentityTask](taskId: String)
                                      //additional: Option[Observable[T] => Observable[T]])
                                     (implicit mf: Manifest[T]) = {
//      log(s"enter `taskResult` - $taskId", -15)

      //todo publish could save for multi execute map body.
      val pub = jRead.map { jsonProto =>
        log(s"any JProtocol taskId - $taskId - $jsonProto - class - ${this}", 80)

//        jsonProto._2 match {
//          case Some(`taskId`) =>
//            log(s"specify JProtocol taskId - $taskId, loaded - $jsonProto",-10 )
//            try {
//              Some(JsonParse.deCode[T](jsonProto._1))
//            } catch {
//              case e: MappingException =>
//                log(s"find taskId but can't extract it, $jsonProto, to class - ${mf.runtimeClass}", 3)
//                None
//            }
//          case _ =>
//            log(s"other JProtocol taskId - $taskId, loaded - $jsonProto")
//            None
//        }

        jsonProto \ "taskId" match {
          case JString(task) if task == taskId =>
            log(s"specify JProtocol taskId - $taskId, loaded - $jsonProto")
            try {
              Some(JsonParse.deCode[T](jsonProto))
            } catch {
              case e: MappingException =>
                log(s"find taskId but can't extract it, $jsonProto, to class - ${mf.runtimeClass}", 3)
                None
            }
          case _ =>
            log(s"other JProtocol taskId - $taskId, loaded - $jsonProto")
            None
        }
      }//.onBackpressureBuffer
//      pub.connect
      pub.filter(_.isDefined).map(_.get).
        timeout(Duration(presentation.TIMEOUT, TimeUnit.SECONDS)).
        doOnError { e => log(s"throw to JProtocol Obv - $taskId - $e", -17) }
    }

    val x = taskResult[Result](any.taskId)
    val finalObv = additional.map(_(x)).getOrElse(x)

    //send msg after prepare stream
    val bytes = JsonParse.enCode(any)
    connectedSocket.send(ByteBuffer.wrap(bytes))

    finalObv
  }
}
