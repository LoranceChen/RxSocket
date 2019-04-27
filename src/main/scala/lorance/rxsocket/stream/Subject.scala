//package lorance.rxsocket.stream
//
//import java.util.concurrent.atomic.AtomicReference
//
//import monix.execution.atomic.Atomic
//
//trait Subject[I, +O] extends Observable[O] with Observer[I] {
//
//}
//
//class DefaultSubject[A] extends Subject[A, A] {
//  private[this] val stateRef = Atomic[A]()
//  override def onNext(elem: A): Unit = {
//
//  }
//
//  override def onError(ex: Throwable): Unit = ???
//
//  override def onComplete(): Unit = ???
//
//  override def subscribe(observer: Observer[A]): Cancelable = ???
//
//  override def map[U](f: A => U): Observable[U] = ???
//
//  override def flatMap[U](f: A => Observable[U]): Observable[U] = ???
//}
