package lorance.rxsocket.stream

trait Observable[+T] {
  def subscribe(observer: Observer[T]): Cancelable

  def map[U](f: T => U): Observable[U]

  def flatMap[U](f: T => Observable[U]): Observable[U]

}
//
//object Observable {
//  def apply(): Observable = new Observable()
//}