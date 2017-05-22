import lorance.rxscoket.presentation.json.{JsonParse, IdentityTask}
import net.liftweb.json.JsonAST.{JObject, JString, JField}
import net.liftweb.json._

object ExtractorTest extends App {
  case class ObjectId(`$oid`: String)
  case class AccountVerifyByAggregateResult(taskId: String, _id: Option[ObjectId]) extends IdentityTask

  val x = JsonParse.deCode[AccountVerifyByAggregateResult]("""{"taskId":"application-akka.actor.default-dispatcher-314131996972866"}""")

  val containsId = """{"_id":{"$oid":"56ec127610a686bf90467c57"},"taskId":"application-akka.actor.default-dispatcher-314131996972866"}"""
  val y = JsonParse.deCode[AccountVerifyByAggregateResult](containsId)
}

object JFildToJObject extends App {
  val x = JField("taskId", JString("therad-nanotime"))
  val y = JObject(x)
  println(prettyRender(y))
}