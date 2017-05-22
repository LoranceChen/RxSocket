package lorance.rxscoket.presentation.json

/**
  * if require response use this type as apart of json filed
  * use thread and system nano time as identify id
  */
trait IdentityTask {
  val taskId: String
}
