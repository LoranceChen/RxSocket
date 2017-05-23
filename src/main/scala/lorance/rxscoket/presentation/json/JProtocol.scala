package lorance.rxscoket.presentation.json

import java.nio.ByteBuffer
import java.util.concurrent.{ConcurrentHashMap, TimeUnit}

import lorance.rxscoket._
import lorance.rxscoket.session.{CompletedProto, ConnectedSocket}
import lorance.rxscoket.session.implicitpkg._
import net.liftweb.json.Extraction._
import net.liftweb.json.JsonAST.JValue
import rx.lang.scala.{Observable, Subject}

import scala.concurrent.duration.Duration
import net.liftweb.json._
import lorance.rxscoket.`implicit`.ObvsevableImplicit._

import scala.concurrent.Future
import scala.util.{Failure, Success}

import scala.concurrent.ExecutionContext.Implicits.global

/**
  * create a JProtocol to dispatch all json relate info bind with socket and it's read stream
  */
class JProtocol(val connectedSocket: ConnectedSocket, read: Observable[CompletedProto]) {

  private val tasks = new ConcurrentHashMap[String, Subject[JValue]]()
  private def addTask(taskId: String, taskStream: Subject[JValue]) = tasks.put(taskId, taskStream)
  private def removeTask(taskId: String) = tasks.remove(taskId)
  private def getTask(taskId: String) = Option(tasks.get(taskId))

  val jRead = {
    val j_read = read.map{cp =>
        if(cp.uuid == 1.toByte) {
          val load = cp.loaded.array.string
          rxsocketLogger.log(s"$load", 46, Some("proto-json"))
          parseOpt(load)
        } else  None
      }.filter(_.nonEmpty).map(_.get)

    j_read.subscribe{j =>
      try{
        val taskId = (j \ "taskId").values.asInstanceOf[String]
        val subj = this.getTask(taskId)
        subj.foreach{
          rxsocketLogger.log(s"${compactRender(j)}", 18, Some("taskId-onNext"))
          _.onNext(j)
        }
        rxsocketLogger.log(s"${compactRender(j)}", 78, Some("task-json"))
      } catch {
        case e : Throwable => Unit
      }
    }

    j_read
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
  @deprecated("replaced by sendWithStream", "0.10.1")
  def sendWithResult[Result <: IdentityTask, Req <: IdentityTask]
    (any: Req, additional: Option[Observable[Result] => Observable[Result]])
    (implicit mf: Manifest[Result]): Observable[Result] = {
    val register = Subject[JValue]()
    this.addTask(any.taskId, register)

    val extract = register.map{s => s.extractOpt[Result]}.filter(_.isDefined).map(_.get)
    val resultStream = additional.map(_(extract)).getOrElse(extract).
      timeout(Duration(presentation.JPROTO_TIMEOUT, TimeUnit.SECONDS)).
      doOnError { e => rxsocketLogger.log(s"[Throw] JProtocol.taskResult - ${any.taskId} - $e") }.
      doOnCompleted{
        this.removeTask(any.taskId)
      }

    //send msg after prepare stream
    val bytes = JsonParse.enCode(any)
    connectedSocket.send(ByteBuffer.wrap(bytes))

    resultStream.hot
  }

  def sendWithRsp[Result, Req]
  (any: Req)
  (implicit mf: Manifest[Result]): Future[Result] = {
    val register = Subject[JValue]()
    val taskId = presentation.getTaskId
    this.addTask(taskId, register)

    val extract = register.map{s => s.extractOpt[Result]}.filter(_.isDefined).map(_.get)
    val resultFur = extract.
      timeout(Duration(presentation.JPROTO_TIMEOUT, TimeUnit.SECONDS)).
      future

    resultFur.onComplete({
      case Failure(ex) => rxsocketLogger.log(s"[Throw] JProtocol.taskResult - ${taskId} - $ex")
      case Success(_) => this.removeTask(taskId)
    })

    //send msg after prepare stream
    val bytes = JsonParse.enCode(any)
    connectedSocket.send(ByteBuffer.wrap(bytes))

    resultFur
  }
  /**
    * holder taskId inner the method
    * @param any should be case class
    */
  def sendWithStream[Req, Rsp](any: Req, additional: Option[Observable[Rsp] => Observable[Rsp]])(implicit mf: Manifest[Rsp]) = {
    val register = Subject[JValue]()
    val taskId = presentation.getTaskId
    this.addTask(taskId, register)

    val extract = register.map{s =>
      val removed = s.removeField{
        case JField("taskId", _) => true
        case _          => false
      }
      removed.extractOpt[Rsp]
    }.filter(_.isDefined).map(_.get)
    val resultStream = additional.map(_(extract)).getOrElse(extract).
      timeout(Duration(presentation.JPROTO_TIMEOUT, TimeUnit.SECONDS)).
      doOnError { e => rxsocketLogger.log(s"[Throw] JProtocol.taskResult - $any - $e") }.
      doOnCompleted{
        this.removeTask(taskId)
        //        connectedSocket.netMsgCountBuf.dec
      }

    //send msg after prepare stream
    val mergeTaskId = decompose(any).merge(JObject(JField("taskId", JString(taskId))))
    val bytes = JsonParse.enCode(mergeTaskId)
    connectedSocket.send(ByteBuffer.wrap(bytes))

    resultStream.hot
  }

}
