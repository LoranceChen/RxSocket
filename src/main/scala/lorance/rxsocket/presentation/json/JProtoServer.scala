package lorance.rxsocket.presentation.json

import org.slf4j.LoggerFactory
import rx.lang.scala.Observable
import org.json4s.JsonAST.JValue
import org.json4s.JsonDSL._
import scala.util.{Failure, Success}
import scala.concurrent.ExecutionContext.Implicits.global

class JProtoServer(jProtos: Observable[JProtocol], routes: List[Router]) {
  val logger = LoggerFactory.getLogger(getClass)

  routes.foreach(_.register)
  //handle streams
  jProtos.subscribe ( skt =>
    skt.jRead.subscribe{ jValue =>

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
      val endPoint = Router.dispatch(load)

      logger.debug("result message - " + endPoint)
      /**
        * protocol to client:
        * {
        *   taskId: ...
        *   type: ...
        *   status: ...
        *   load: {
        *     ...
        *   }
        * }
        */
      endPoint match {
        case RawEndPoint(jValRst) =>
          jValRst match {
            case Failure(e) =>
              val finalJson =
                ("taskId" -> taskId) ~
                  ("type" -> "once") ~
                  ("status" -> "error") ~
                  ("load" -> e.getStackTrace.toString)
              skt.send(finalJson)
            case Success(rst) =>
              val finalJson =
                ("taskId" -> taskId) ~
                  ("type" -> "once") ~
                  ("status" -> "end") ~
                  ("load" -> rst)
              skt.send(finalJson)
          }
        case FurEndPoint(jValRstFur) =>
          jValRstFur.foreach(jValRst => {
            val finalJson =
              ("taskId" -> taskId) ~
                ("type" -> "once") ~
                ("status" -> "end") ~
                ("load" -> jValRst)
            skt.send(finalJson)
          })
          jValRstFur.failed.foreach{ error =>
            logger.error("FurEndPoint failed:", error)

            val finalJson =
              ("taskId" -> taskId) ~
                ("type" -> "once") ~
                ("status" -> "error") ~
                ("load" -> error.getStackTrace.mkString)
            skt.send(finalJson)
          }
        case StreamEndPoint(jValRst) =>
          jValRst.subscribe(onNext = event => {
            val finalJson: JValue =
              ("taskId" -> taskId) ~
                ("type" -> "stream") ~
                ("status" -> "on") ~
                ("load" -> event)

            skt.send(finalJson)
          },
            onError = error => {
              logger.error("StreamEndPoint failed:", error)
              val finalJson: JValue =
                ("taskId" -> taskId) ~
                  ("type" -> "stream") ~
                  ("status" -> "error") ~
                  ("load" -> error.getStackTrace.mkString)

              skt.send(finalJson)
            },
            onCompleted = () => {
              val finalJson: JValue =
                ("taskId" -> taskId) ~
                  ("type" -> "stream") ~
                  ("status" -> "end")

              skt.send(finalJson)
            })
        case EmptyEndPoint => Unit
      }
    }
  )
}
