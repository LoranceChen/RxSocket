package lorance.rxsocket.session.implicitpkg

import lorance.rxsocket.dispatch.{Task, TaskKey, TaskManager}
import lorance.rxsocket.session.execution

import scala.concurrent.duration.Duration
import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.global

case object FutureTimeoutException extends RuntimeException
case object FutureTimeoutNotOccur extends RuntimeException

object FutureEx {
  val timerManager = new TaskManager()

}
class FutureEx[T](f: Future[T]){

  private class FutureTimeoutTask[T](id: String, delayMilliTime: Long, promise: Promise[T]) extends Task {
    override val taskId: TaskKey = TaskKey(id, System.currentTimeMillis() + delayMilliTime)

    override def execute(): Unit = promise.tryFailure(FutureTimeoutException)

    override def nextTask: Option[Task] = None
  }

  // use a waitExecutor to handle sleep
  def withTimeout(ms: Long = 2000): Future[T] = {
    // 注意： 將ex放在外面会造成对firstCompletedOf的执行上下文有影响，所以尽量将阻塞操作隔离开
//    val ex: ExecutionContextExecutor = execution.waitExecutor

    Future.firstCompletedOf(List(f, {
      val p = Promise[T]

      val id = Task.getId
      //add time task to a scheduler
      FutureEx.timerManager.addTask(new FutureTimeoutTask(id, ms, p))
//      Future {
//
//        blocking(Thread.sleep(ms))
//        if(!f.isCompleted) {
//          p.tryFailure(new FutureTimeoutException)
//        } else {
//          p.tryFailure(new FutureTimeoutNotOccur)
//        }
//      }(ex)
      f.onComplete(_ => {
        // 源Futrue完成之后，如果定时器Future没有完成，则取消定时器的调度工作
        if(!p.isCompleted) {
          FutureEx.timerManager.cancelTask(id)
//          p.tryFailure(FutureTimeoutNotOccur)
        }
      })
      p.future
    }))

  }

  def withTimeout(duration: Duration)
//                 (implicit executor: ExecutionContext)
                  : Future[T] = withTimeout(duration.toMillis)
}
