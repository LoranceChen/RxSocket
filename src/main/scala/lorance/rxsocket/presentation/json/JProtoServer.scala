package lorance.rxsocket.presentation.json

import monix.execution.Ack
import monix.execution.Ack.Continue
import monix.reactive.Observable
import org.slf4j.LoggerFactory
import org.json4s.JsonAST.JValue
import org.json4s.JsonDSL._

import scala.util.{Failure, Success}
import monix.execution.Scheduler.Implicits.global

import scala.concurrent.{Future, Promise}

class JProtoServer(jProtos: Observable[JProtocol], routes: List[JRouter]) {
  protected val logger = LoggerFactory.getLogger(getClass)

  val jRouterManager = new JRouterManager()

  jRouterManager.routes ++= routes.map(x => x.path -> x).toMap

  //handle streams
  jProtos.subscribe { skt =>
    skt.jRead.subscribe { jValue =>

      /**
        * protocol form client:
        * {
        *   taskId: ...
        *   load: {
        *     path: ...
        *     protoId: ...
        *     load: {
        *       ...
        *     }
        *   }
        * }
        */
      val load = jValue \ "load"
      val taskId = jValue \ "taskId"
      val endPoint = jRouterManager.dispatch(load)

      logger.debug("result message - " + endPoint)

      /**
        * protocol to client:
        * {
        *   taskId: ...
        *   type: once/stream
        *   status: error/end/on
        *   load: {
        *     ...
        *   }
        * }
        */
      val rst: Future[Ack] = endPoint match {
        case RawEndPoint(jValRst) =>
          val rst: Future[Ack] = jValRst match {
            case Failure(e) =>
              val finalJson =
                ("taskId" -> taskId) ~
                  ("type" -> "once") ~
                  ("status" -> "error") ~
                  ("load" -> e.getStackTrace.toString)
              skt.sendRaw(finalJson).flatMap(_ => Continue)
            case Success(rst) =>
              val finalJson =
                ("taskId" -> taskId) ~
                  ("type" -> "once") ~
                  ("status" -> "end") ~
                  ("load" -> rst)
              skt.sendRaw(finalJson).flatMap(_ => Continue)
          }
          rst
        case FurEndPoint(jValRstFur) =>
          val p = Promise[Unit]
          jValRstFur.map(jValRst => {
            val finalJson =
              ("taskId" -> taskId) ~
                ("type" -> "once") ~
                ("status" -> "end") ~
                ("load" -> jValRst)
            p.tryCompleteWith(skt.sendRaw(finalJson))

          })
          jValRstFur.failed.map { error =>
            logger.error("FurEndPoint failed:", error)

            val finalJson =
              ("taskId" -> taskId) ~
                ("type" -> "once") ~
                ("status" -> "error") ~
                ("load" -> error.getStackTrace.mkString)
            p.tryCompleteWith(skt.sendRaw(finalJson))
          }

          val rst: Future[Ack] = p.future.flatMap(_ => Continue)
          rst
        case StreamEndPoint(jValRst) =>
          val promise = Promise[Ack]
          jValRst.subscribe(
            event => {
              val finalJson: JValue =
                ("taskId" -> taskId) ~
                ("type" -> "stream") ~
                ("status" -> "on") ~
                ("load" -> event)

              //this stream i
              skt.sendRaw(finalJson).flatMap(_ => Continue)
            },
            error => {
              logger.error("StreamEndPoint failed:", error)
              val finalJson: JValue =
                ("taskId" -> taskId) ~
                ("type" -> "stream") ~
                ("status" -> "error") ~
                ("load" -> error.getStackTrace.mkString)

              val rst: Future[Ack] = skt.sendRaw(finalJson).flatMap(_ => Continue)

              promise.tryCompleteWith(Continue)
              Unit
            },
            () => {
              val finalJson: JValue =
                ("taskId" -> taskId) ~
                ("type" -> "stream") ~
                ("status" -> "end")

              skt.sendRaw(finalJson)

              promise.tryCompleteWith(Continue)
              Unit
            }
          )
          promise.future
        case EmptyEndPoint => Unit
          Continue
      }
      rst
    }

    Continue
  }

}
