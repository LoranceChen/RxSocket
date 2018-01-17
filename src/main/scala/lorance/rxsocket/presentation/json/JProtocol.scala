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
import rx.lang.scala.{Observable, Subject}

import scala.concurrent.duration.Duration
import lorance.rxsocket.`implicit`.ObvsevableImplicit._

import scala.concurrent.Future
import scala.util.{Failure, Success}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.control.NonFatal

/**
  * create a JProtocol to dispatch all json relate info bind with socket and it's read stream
  */
class JProtocol(val connectedSocket: ConnectedSocket, read: Observable[CompletedProto]) {
  private val logger: Logger = LoggerFactory.getLogger(getClass)

  private val tasks = new ConcurrentHashMap[String, Subject[JValue]]()
  private def addTask(taskId: String, taskStream: Subject[JValue]) = tasks.put(taskId, taskStream)
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
                    sub.onCompleted()
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
              logger.warn(s"un known jproto type $otherTyp in json result: ${compact(render(jsonRsp))}")
          }
      }

    } catch {
      case NonFatal(e)=>
        logger.warn(s"jread fail: ${compact(render(jsonRsp))} - ", e)
        Unit
    }
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
    val register = Subject[JValue]()
    val taskId = presentation.getTaskId
    this.addTask(taskId, register)

    val resultFur = register.map{s => s.extract[Rsp]}.
      timeout(Duration(presentation.JPROTO_TIMEOUT, TimeUnit.SECONDS)).
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
    val register = Subject[JValue]()
    val taskId = presentation.getTaskId
    this.addTask(taskId, register)

    val extract = register.map{load =>
      load.extract[Rsp]
    }

    val resultStream = additional.map(_(extract)).getOrElse(extract).
      timeout(Duration(presentation.JPROTO_TIMEOUT, TimeUnit.SECONDS)).
      doOnError { e => logger.error(s"[Throw] JProtocol.taskResult - $any", e) }.
      doOnCompleted{
        this.removeTask(taskId)
      }

    //send msg after prepare stream
    val mergeTaskId =
      ("taskId" -> taskId) ~
      ("load" -> decompose(any))
    val bytes = JsonParse.enCode(mergeTaskId)
    connectedSocket.send(ByteBuffer.wrap(bytes))

    resultStream.hot
  }

}

case class StreamOccurredFail(msg: String) extends RuntimeException