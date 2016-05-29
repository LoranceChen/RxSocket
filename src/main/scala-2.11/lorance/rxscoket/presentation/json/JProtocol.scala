package lorance.rxscoket.presentation.json

import java.nio.ByteBuffer
import java.util.concurrent.{ConcurrentHashMap, TimeUnit}

import lorance.rxscoket._
import lorance.rxscoket.session.{CompletedProto, ConnectedSocket}
import lorance.rxscoket.session.implicitpkg._
import net.liftweb.json.JsonAST.JValue
import rx.lang.scala.{Subject, Observable}
import scala.concurrent.duration.Duration
import net.liftweb.json._

/**
  * create a JProtocol to dispatch all json relate info bind with socket and it's read stream
  */
class JProtocol(connectedSocket: ConnectedSocket, read: Observable[CompletedProto]) {

  private val tasks = new ConcurrentHashMap[String, Subject[JValue]]()
  def addTask(taskId: String, taskStream: Subject[JValue]) = tasks.put(taskId, taskStream)
  def removeTask(taskId: String) = tasks.remove(taskId)
  def getTask(taskId: String) = Option(tasks.get(taskId))

  val jRead = {
    val j_read = read.map{cp =>
        if(cp.uuid == 1.toByte) {
          val load = cp.loaded.array.string
          log(s"$load", 46, Some("proto-json"))
          parseOpt(load)
        } else  None
      }.filter(_.nonEmpty).map(_.get)

    j_read.subscribe{j =>
      try{
        val taskId = (j \ "taskId").values.asInstanceOf[String]
        val subj = this.getTask(taskId)
        subj.foreach{
          log(s"${compactRender(j)}", 18, Some("taskId-onNext"))
          _.onNext(j)
        }
        log(s"${compactRender(j)}", 78, Some("task-json"))
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
  def sendWithResult[Result <: IdentityTask, Req <: IdentityTask]
    (any: Req, additional: Option[Observable[Result] => Observable[Result]])
    (implicit mf: Manifest[Result]): Observable[Result] = {
    val register = Subject[JValue]()
    this.addTask(any.taskId, register)

    val extract = register.map{s => s.extractOpt[Result]}.filter(_.isDefined).map(_.get)
    val resultStream = additional.map(_(extract)).getOrElse(extract).
      timeout(Duration(presentation.JPROTO_TIMEOUT, TimeUnit.SECONDS)).
      doOnError { e => log(s"[Throw] JProtocol.taskResult - ${any.taskId} - $e") }.
      doOnCompleted{
        this.removeTask(any.taskId)
//        connectedSocket.netMsgCountBuf.dec
      }

    //send msg after prepare stream
    val bytes = JsonParse.enCode(any)
    connectedSocket.send(ByteBuffer.wrap(bytes))

    resultStream
  }

  /**
    * auto attach taskId
    *
    * @tparam Result return json extractable class, should be case class
    * @tparam Req should be case class
    * @return
    */
  private def sendWithResult2[Result, Req](any: Req)
  (implicit mf: Manifest[Result]): Observable[Result] = {
    val register = Subject[JValue]()
    val taskId = presentation.getTaskId
    this.addTask(taskId, register)

    case class ResultWithTaskId(result: Option[Result], taskId: String)
    val extract = register.map{s => s.extractOpt[ResultWithTaskId]}.filter(_.isDefined).map(_.get)
    val resultStream = extract.takeWhile(_.result.nonEmpty).map(_.result.get).
      timeout(Duration(presentation.JPROTO_TIMEOUT, TimeUnit.SECONDS)).
      doOnError { e => log(s"[Throw] JProtocol.taskResult - $any - $e") }.
      doOnCompleted{
        this.removeTask(taskId)
      }

    //send msg after prepare stream
    val bytes = JsonParse.enCode(any)
    connectedSocket.send(ByteBuffer.wrap(bytes))

    resultStream
  }

//  private val serviceModel = mutable.HashMap[String, JValue => Unit]()
//  def addService(taskId: String, taskStream: JValue => Unit) = serviceModel.synchronized(serviceModel.+=(taskId -> taskStream))
//  def removeService(taskId: String) = serviceModel.synchronized(serviceModel.-=(taskId))
//  def getService(taskId: String) = serviceModel.get(taskId)
//
//  /**
//    * register service able to deal with `event` when receive.
//    */
//  def serviceAdd(model: String, event: JValue => Unit): Unit = {
//    addService(model, event)
//  }
}
