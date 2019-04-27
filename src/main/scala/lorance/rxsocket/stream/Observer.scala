package lorance.rxsocket.stream

trait Observer[-A] {
  def onNext(elem: A): Unit //Future[Ack]

  def onError(ex: Throwable): Unit

  def onComplete(): Unit

}
