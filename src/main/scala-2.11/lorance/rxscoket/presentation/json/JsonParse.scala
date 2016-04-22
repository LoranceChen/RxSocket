package lorance.rxscoket.presentation.json

import java.nio.charset.StandardCharsets

import lorance.rxscoket.session
import net.liftweb.json.Extraction._
import net.liftweb.json._

object JsonParse {

  /**
    * todo try-catch
    * @param obj case class
    */
  def enCode(obj: Any) = {
    val jStr = compactRender(decompose(obj))
    session.enCode(1.toByte, jStr)
  }
  def enCode(jValue: JValue) = session.enCode(1.toByte, compactRender(jValue))
  def enCode(jStr: String) = session.enCode(1.toByte, jStr)

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
