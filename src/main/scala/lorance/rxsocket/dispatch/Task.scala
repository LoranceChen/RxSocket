package lorance.rxsocket.dispatch

case class TaskKey(id: String, systemTime: Long)

trait Task {
  val taskId: TaskKey //account and custom name
  def execute(): Unit
  def nextTask: Option[Task] //able to execute next time, completed as None
  override def toString = {
    super.toString + s"-$taskId"
  }
}
