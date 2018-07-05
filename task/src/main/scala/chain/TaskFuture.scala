//package chain
//
//import scala.collection.mutable
//
///**
//  * a task like a Future in Scala or a Task in Monix but support a concept of chain form first Task
//  * to final Task. That means, in backend programming, we use a chain of tasks to simulate one request.
//  * Different with other library, this implement a minimal definition for a back-pressure
//  * reactive system to handle tons of request in system but not timeout former request.
//  * Request is a chain of Task instance and the application will deal with tons of request
//  * by FIFO. Future more, we can statistic the status of request, waiting task and compose with system status
//  * to give a supervisor for the system.
//  *
//  * Why it is different?
//  * In traditional way, we usually use ForkJoinPool to handle all request. But the latter will
//  * affect the former ones in a reactive async system which means a request callback will
//  * re-enqueue the scheduler pool. this will cause a long delay if a system both deal with many request
//  * and a request is split by many callback.
//  *
//  * Some other language, such as Erlang and Golang not fit all the needs of a reactive system because of
//  * scheduler not support FIFO for a request lift-cycle and not smoothly do a async programming workflow.
//  *
//  *
//  * Task.run() will invoke the task chain with a specify priority fit for FIFO.
//  */
//abstract class Task[T]{
//  private val callbacks = mutable.MutableList[T => Unit]()
//  @volatile private var value: Option[T] = None
//  private val lock = new Object
//
//  def map[B](f: T => B): Task[B] = {
//    val rst = value match {
//      case None =>
//        val p = Promise[B]()
//        onCompleted(x => p.completeWith(f(x)))
//        p
//      case Some(v) =>
//        Eager(f(v))
//    }
//
//    rst
//
//  }
//
//  def flatmap[B](f: T => Task[B]): Task[B] = {
//    val rst = value match {
//      case None =>
//        val p = Promise[B]()
//        onCompleted(x => {
//          f(x).onCompleted(y => p.completeWith(y))
//        })
//        p
//      case Some(v) =>
//        f(v)
//    }
//
//    rst
//  }
//
//  def foreach(f: T => Unit): Unit = {
//    onCompleted(x => f(x))
//  }
//
//  def completeWith(value: T): Boolean = lock.synchronized {
//    this.value match {
//      case None =>
//        this.value = Some(value)
//        callbacks.foreach(x => x(value))
//        callbacks.clear()
//        true
//      case Some(v) =>
//        false
//    }
//  }
//
//  def onCompleted(f: T => Unit): Unit = lock.synchronized {
//    value match {
//      case None =>
//        callbacks += f
//      case Some(v) =>
//        f(v)
//    }
//
//  }
//
//
//}
//
//case class Eval[T](value: () => T) extends Task[T]
//case class Eager[T](value: T) extends Task[T] {
//  completeWith(value)
//}
//case class Promise[T]() extends Task[T]
//
//object Task {
//  def apply[T](value: => T): Task[T] = new Eval[T](() => value)
//
//}
