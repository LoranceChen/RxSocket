package lorance.rxsocket.session


import monix.eval.Task
import monix.reactive._
/**
  * ConcurrentStream handle all message/event of the system.
  * Give a back-pressure to protect the system and
  *
  *
  */
class ConcurrentStream[Proto] {
  val cpus = Runtime.getRuntime.availableProcessors

  val size = cpus
  def add(observable: Observable[Proto]) = {
    observable.mapParallelUnordered(cpus){x => Task(x)}
  }
}
