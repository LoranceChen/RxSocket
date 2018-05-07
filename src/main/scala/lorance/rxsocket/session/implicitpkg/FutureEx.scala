package lorance.rxsocket.session.implicitpkg

import lorance.rxsocket.session.execution

import scala.concurrent.duration.Duration
import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.global

sealed class FutureTimeoutException extends RuntimeException
sealed class FutureTimeoutNotOccur extends RuntimeException

class FutureEx[T](f: Future[T]) {
  // use a waitExecutor to handle sleep
  def withTimeout(ms: Long = 2000): Future[T] = {
    val ex: ExecutionContextExecutor = execution.waitExecutor

    Future.firstCompletedOf(List(f, {
      //todo 看看monix库中有类似Cancel Task的东西能否优化这里的Thread.sleep
      val p = Promise[T]
      Future {
        blocking(Thread.sleep(ms))
        if(!f.isCompleted) {
          p.tryFailure(new FutureTimeoutException)
        } else {
          p.tryFailure(new FutureTimeoutNotOccur)
        }
      }(ex)
      p.future
    }))
  }

  def withTimeout(duration: Duration)
//                 (implicit executor: ExecutionContext)
                  : Future[T] = withTimeout(duration.toMillis)
}
