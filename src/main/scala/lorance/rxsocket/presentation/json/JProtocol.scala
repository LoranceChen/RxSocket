package lorance.rxsocket.presentation.json

import java.nio.ByteBuffer
import java.util.concurrent.{ConcurrentHashMap, TimeUnit}

import org.json4s._
import org.json4s.Extraction._
import org.json4s.JsonAST.JValue
import org.json4s.native.JsonMethods._
import org.json4s.JsonDSL._
import org.slf4j.{Logger, LoggerFactory}
import lorance.rxsocket._
import lorance.rxsocket.session.{CompletedProto, ConnectedSocket}
import lorance.rxsocket.session.implicitpkg._

import scala.concurrent.duration.Duration
import lorance.rxsocket.`implicit`.ObvsevableImplicit._
import monix.execution.Ack.Continue
import monix.reactive.Observable
import monix.reactive.subjects.PublishSubject

import scala.concurrent.Future
import scala.util.{Failure, Success}
import scala.util.control.NonFatal
import monix.execution.Scheduler.Implicits.global

/**
  * create a JProtocol to dispatch all json relate info bind with socket and it's read stream
  */
class JProtocol(val connectedSocket: ConnectedSocket, read: Observable[CompletedProto]) {
  private val logger: Logger = LoggerFactory.getLogger(getClass)

  private val tasks = new ConcurrentHashMap[String, PublishSubject[JValue]]()
  private def addTask(taskId: String, taskStream: PublishSubject[JValue]) = tasks.put(taskId, taskStream)
  private def removeTask(taskId: String) = tasks.remove(taskId)
  private def getTask(taskId: String) = Option(tasks.get(taskId))

  /**
    * parse to json if uuid == 1
    */
  val jRead: Observable[JValue] = {
    read.map{cp =>
        if(cp.uuid == 1.toByte) {
          val load = cp.loaded.array.string
          logger.debug(s"$load", 46, Some("proto-json"))
          Some(parse(load))
        } else  None
      }.filter(_.nonEmpty).map(_.get)
  }

  /**
    * two module support:
    * 1. has type field:
    * this subscription distinct different JProtocols with type field.
    * type == once: response message only once
    * type == stream: response message multi-times.
    *
    * 2. not type field
    * use simple dispatch mode
    *
    * TODO: this place should be scalable.
    */
  jRead.subscribe{jsonRsp =>
    try{
      logger.debug(s"jRead get msg: ${compact(render(jsonRsp))}")

      val JString(taskId) = jsonRsp \ "taskId"

      /**
        * TODO: consider moving "type"'s logic to `sendWithRsp` or `sendWithStream`,
        *       because this code block shouldn't care about how to deal with single
        *       or stream result.It handle taskId is OK.
        *       On the other hand, `tasks` should remove the place where it was added,
        *       so `sendWithRsp` or `sendWithStream` is the place to add and remove.
        */
      jsonRsp \ "type" match {
        case JNothing =>
          val subj = this.getTask(taskId)
          subj.foreach{
            _.onNext(jsonRsp)
          }
        case JString(typ) =>
          typ match {
            case "once" =>
              val load = jsonRsp \ "load"
              val subj = this.getTask(taskId)
              subj.foreach{ sub =>
                sub.onNext(load)
              }
              removeTask(taskId)
            case "stream" =>
              val load = jsonRsp \ "load"
              jsonRsp \ "status" match {
                case JString("end") =>
                  val subj = this.getTask(taskId)
                  subj.foreach{ sub =>
                    removeTask(taskId)
                    sub.onComplete()
                  }
                case JString("on") =>
                  val subj = this.getTask(taskId)
                  subj.foreach{ sub =>
                    sub.onNext(load)
                  }
                case JString("error") =>
                  val subj = this.getTask(taskId)
                  subj.foreach{ sub =>
                    removeTask(taskId)
                    sub.onError(StreamOccurredFail(compact(render(load))))
                  }

                case unknown =>
                  logger.warn(s"get unknown jproto status: $unknown in json result: ${compact(render(jsonRsp))}")
              }

            case otherTyp =>
              logger.warn(s"un known jproto string type $otherTyp in json result: ${compact(render(jsonRsp))}")
          }
        case otherType =>
          logger.warn(s"un known jproto JValue type $otherType in json result: ${compact(render(jsonRsp))}")

      }

    } catch {
      case NonFatal(e)=>
        logger.warn(s"jread fail: ${compact(render(jsonRsp))} - ", e)
        Unit
    }

    Continue
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
    * protocol:
    * {
    *   taskId:
    *   //type: once, //to consider(for security, such as client need a stream but server only send one message)
    *   load: @param any
    * }
    *
    */
  def sendWithRsp[Req, Rsp]
  (any: Req)
  (implicit mf: Manifest[Rsp]): Future[Rsp] = {
    val register = PublishSubject[JValue]
    val taskId = presentation.getTaskId
    this.addTask(taskId, register)

    val resultFur = register.map{s => s.extract[Rsp]}
      .timeoutOnSlowDownstream(Duration(presentation.JPROTO_TIMEOUT, TimeUnit.SECONDS)).
      future

    resultFur.onComplete({
      case Failure(ex) => logger.error(s"[Throw] JProtocol.taskResult - $taskId", ex)
      case Success(_) => this.removeTask(taskId)
    })

    //send msg after prepare stream
    val mergeTaskId: JObject =
      ("taskId" -> taskId) ~
      ("load" -> decompose(any))

    val bytes = JsonParse.enCode(mergeTaskId)
    connectedSocket.send(ByteBuffer.wrap(bytes))

    resultFur
  }

  /**
    * holder taskId inner the method
    * @param any should be case class
    * @param additional complete the stream ahead of time
    */
  def sendWithStream[Req, Rsp](any: Req, additional: Option[Observable[Rsp] => Observable[Rsp]] = None)(implicit mf: Manifest[Rsp]) = {
    val register = PublishSubject[JValue]()
    val taskId = presentation.getTaskId
    this.addTask(taskId, register)

    val extract = register
      .map{load =>
      load.extract[Rsp]
    }

    val resultStream = additional.map(f => f(extract)).getOrElse(extract)
      .timeoutOnSlowUpstream(Duration(presentation.JPROTO_TIMEOUT, TimeUnit.SECONDS))
      .doOnError { e => logger.error(s"[Throw] JProtocol.taskResult - $any", e) }
      .doOnComplete{ () =>
        this.removeTask(taskId)
      }
      .doOnEarlyStop(() => this.removeTask(taskId))

    //send msg after prepare stream
    val mergeTaskId =
      ("taskId" -> taskId) ~
      ("load" -> decompose(any))
    val bytes = JsonParse.enCode(mergeTaskId)
    connectedSocket.send(ByteBuffer.wrap(bytes))

    resultStream.share
  }

}

case class StreamOccurredFail(msg: String) extends RuntimeException