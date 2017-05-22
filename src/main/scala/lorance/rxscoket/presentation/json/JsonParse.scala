package lorance.rxscoket.presentation.json

import java.nio.charset.StandardCharsets

import lorance.rxscoket.session
import lorance.rxscoket._
import net.liftweb.json.Extraction._
import net.liftweb.json._

object JsonParse {

  /**
    * todo try-catch
    * @param obj case class
    */
  def enCode(obj: Any): Array[Byte] = enCode(compactRender(decompose(obj)))

  def enCode(jValue: JValue): Array[Byte] = enCode(compactRender(jValue))

  def enCode(jStr: String): Array[Byte] = {
    rxsocketLogger.log("encode jtr - " + jStr, 70)
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
