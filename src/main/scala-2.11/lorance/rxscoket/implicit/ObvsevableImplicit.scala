package lorance.rxscoket.`implicit`

import rx.lang.scala.Observable

import scala.concurrent.{Future, Promise}

/**
  *
  */
object ObvsevableImplicit {
  implicit class ObservableEx[T](src: Observable[T]) {
    def hot: Observable[T] = src.publish.refCount

    def future: Future[T] = {
      val p = Promise[T]
      src.headOption.subscribe(_ match {
        case None => p.tryFailure(new NoSuchElementException)
        case Some(value) => p.trySuccess(value)
      })
      p.future
    }
  }
}

