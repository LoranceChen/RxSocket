package lorance.rxscoket.session.implicitpkg

import lorance.rxscoket.rxsocketLogger
import scala.concurrent.{Promise, ExecutionContext, Future, blocking}

sealed class FutureTimeoutException extends RuntimeException
sealed class FutureTimeoutNotOccur extends RuntimeException

class FutureEx[T](f: Future[T]) {
  def withTimeout(ms: Long = 2000)(implicit executor: ExecutionContext): Future[T] = Future.firstCompletedOf(List(f, {
    val p = Promise[T]
    Future {
      blocking(Thread.sleep(ms))
      if(!f.isCompleted) {
        rxsocketLogger.log(s"[Throw] - FutureTimeoutException after - ${ms}ms", 15)
        p.tryFailure(new FutureTimeoutException)
      } else {
        p.tryFailure(new FutureTimeoutNotOccur)
      }
    }
    p.future
  }))
}
