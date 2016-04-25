package lorance.rxscoket.presentation.json

import java.nio.ByteBuffer
import java.util.concurrent.TimeUnit

import lorance.rxscoket._
import lorance.rxscoket.session.{CompletedProto, ConnectedSocket}
import lorance.rxscoket.session.implicitpkg._
import net.liftweb.json.JsonAST.JValue
import rx.lang.scala.{Subject, Observable}
import scala.collection.mutable
import scala.concurrent.duration.Duration
import net.liftweb.json._

/**
  * create a JProtocol to dispatch all json relate info bind with socket and it's read stream
  */
class JProtocol(connectedSocket: ConnectedSocket, read: Observable[Vector[CompletedProto]]) {

  private val tasks = mutable.HashMap[String, Subject[JValue]]()
//  private val tasks = mutable.HashMap[String, Subject[_ <: IdentityTask]]()
//  def addTask[T <: IdentityTask](taskId: String, taskStream: Subject[T], event: Tuple2[JValue , Subject[T]] => Unit) = tasks.synchronized(tasks.+=(taskId -> ((taskStream, event))))
  def addTask(taskId: String, taskStream: Subject[JValue]) = tasks.synchronized(tasks.+=(taskId -> taskStream))
  def removeTask(taskId: String) = tasks.synchronized(tasks.-=(taskId))
  def getTask(taskId: String) = tasks(taskId)

  var high = 0
  val jRead = read.flatMap { cps =>
    val jsonProto = cps.filter(_.uuid == 1.toByte)
    val p = Observable.from(
      jsonProto.map{cp =>
//        log("jread - ", -20)
//        if (tasks.size > high) high = tasks.size
//        log("count - " + high.toString, -30)

        parseOpt(cp.loaded.array.string)}.
        filter(_.nonEmpty).
//        map(_.get)
        map{j =>
          try{
            val taskId = (j.get \ "taskId").values.asInstanceOf[String]
            val subj = this.getTask(taskId)
            subj.onNext(j.get)
          } catch {
            case e : Throwable => Unit
          }
          j.get
        }
//    .observeOn(ExecutionContextScheduler(global))//.onBackpressureBuffer//.publish
    )//.publish todo why publish - connect will make server's subsriber doesn't work?
    //p.connect
    p
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

//    def taskResult[T <: IdentityTask](taskId: String)
//                                     (implicit mf: Manifest[T]) = {
//      jRead.map { jsonProto =>
//        log(s"any JProtocol taskId - $taskId - $jsonProto - class - ${this}", 1000)
//        jsonProto \ "taskId" match {
//          case JString(task) if task == taskId =>
//            log(s"specify JProtocol taskId - $taskId, loaded - $jsonProto", 25)
//            try {
//              Some(JsonParse.deCode[T](jsonProto))
//            } catch {
//              case e: MappingException =>
//                log(s"find taskId but can't extract it, $jsonProto, to class - ${mf.runtimeClass}", 3)
//                None
//            }
//          case _ =>
//            log(s"other JProtocol taskId - $taskId, loaded - $jsonProto", 1000)
//            None
//        }
//      }.filter(_.isDefined).map(_.get).
//        timeout(Duration(presentation.JPROTO_TIMEOUT, TimeUnit.SECONDS)).
//        doOnError { e => log(s"[Throw] JProtocol.taskResult - $taskId - $e", 1) }
//    }
//
//    val x = taskResult[Result](any.taskId)
//    val finalObv = additional.map(_(x)).getOrElse(x)

    val register = Subject[JValue]()
    this.addTask(any.taskId, register)

    val extract = register.map{s => s.extractOpt[Result]}.filter(_.isDefined).map(_.get)
    val resultStream = additional.map(_(extract)).getOrElse(extract).
      timeout(Duration(presentation.JPROTO_TIMEOUT, TimeUnit.SECONDS)).
      doOnError { e => log(s"[Throw] JProtocol.taskResult - ${any.taskId} - $e", -101) }.
      doOnCompleted(this.removeTask(any.taskId))

    //send msg after prepare stream
    val bytes = JsonParse.enCode(any)
    connectedSocket.send(ByteBuffer.wrap(bytes))
//    sub2
    resultStream
//    finalObv
  }
}
