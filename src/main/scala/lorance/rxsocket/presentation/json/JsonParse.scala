package lorance.rxsocket.presentation.json

import java.nio.charset.StandardCharsets

import org.json4s.JValue
import org.json4s._
import org.json4s.Extraction._
import org.json4s.native.JsonMethods._
import org.slf4j.LoggerFactory
import lorance.rxsocket.session
import lorance.rxsocket._

object JsonParse {
  private val logger = LoggerFactory.getLogger(getClass)
  /**
    * todo try-catch
    * @param obj case class
    */
  def enCode(obj: Any): Array[Byte] = {
    enCode(compact(render(decompose(obj))))
  }

  def enCode(jValue: JValue): Array[Byte] = enCode(compact(render(jValue)))

  def enCode(jStr: String): Array[Byte] = {
    logger.trace("encode jtr - " + jStr)
    session.enCode(1.toByte, jStr)
  }

  def deCode[A](jValue: JValue)(implicit mf: scala.reflect.Manifest[A]): A = {
    jValue.extract[A]
  }

  def deCode[A](jsonString: String)(implicit mf: scala.reflect.Manifest[A]): A = {
    deCode(parse(jsonString))
  }

  def deCode[A](jsonArray: Array[Byte])(implicit mf: scala.reflect.Manifest[A]): A = {
    deCode(parse(new String(jsonArray, StandardCharsets.UTF_8)))
  }
}
