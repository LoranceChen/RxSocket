package lorance.rxsocket.`implicit`

import monix.execution.Ack.{Continue, Stop}
import monix.execution.{Ack, Scheduler}
import monix.reactive.Observable
import monix.reactive.observers.Subscriber

import scala.concurrent.{Future, Promise}
import monix.execution.Scheduler.Implicits.global

/**
  *
  */
object ObvsevableImplicit {
  implicit class ObservableEx[T](src: Observable[T]) {
    def hot: Observable[T] =
      src.share
    // same as: src.publish.refCount

    def future: Future[T] = {
      val p = Promise[T]
      src.take(1)
        .subscribe(
          x => {p.trySuccess(x); Stop},
          e => p.tryFailure(e),
          () => p.tryFailure(new NoSuchElementException)
        )

      p.future
    }
  }
}

