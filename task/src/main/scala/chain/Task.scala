package chain

import scala.collection.mutable

/**
  * a task like a Future in Scala or a Task in Monix but support a concept of chain form first Task
  * to final Task. That means, in backend programming, we use a chain of tasks to simulate one request.
  * Different with other library, this implement a minimal definition for a back-pressure
  * reactive system to handle tons of request in system but not timeout former request.
  * Request is a chain of Task instance and the application will deal with tons of request
  * by FIFO. Future more, we can statistic the status of request, waiting task and compose with system status
  * to give a supervisor for the system.
  *
  * Why it is different?
  * In traditional way, we usually use ForkJoinPool to handle all request. But the latter will
  * affect the former ones in a reactive async system which means a request callback will
  * re-enqueue the scheduler pool. this will cause a long delay if a system both deal with many request
  * and a request is split by many callback.
  *
  * Some other language, such as Erlang and Golang not fit all the needs of a reactive system because of
  * scheduler not support FIFO for a request lift-cycle and not smoothly do a async programming workflow.
  *
  *
  * Task.run() will invoke the task chain with a specify priority fit for FIFO.
  *
  * Some notice:
  * - distinct with Scala Future: when the run method invoked, task chain will do the best to execute all possible
  * Tasks  which is different from Future to split every small Future to executor pool again.
  *
  * - distinct with Monix Task, this one only eval once time. Consider this one is a Scala Future but give a lazy
  * evaluates like Monix Task run invoke.
  *
  * - promise those stage:
  *   - only run invoked to execute all chain except
  */
abstract class Task[T]{
  protected val callbacks = mutable.MutableList[T => Unit]()
  @volatile protected var formerTask: Option[Task[_]] = None
  @volatile protected var value: Option[T] = None
  protected val lock = new Object
  @volatile protected var hasEager = false
  @volatile protected[chain] var priority: Int = _

  /**
    * make lazy to eage
    */
  protected[chain] def toEager(): Unit = {
    hasEager = true
  }

  /**
    * promise all relative work
    * eval all tasks of the chain and return this one result.
    */
  def run(priority: Int): Task[T] = {
    this.priority = priority
    this.toEager()
    def runHelp(curTask: Task[_]): Unit = {
      curTask.formerTask match {
        case None => ()
        case Some(task) =>
          task.priority = priority
          task.toEager()
          runHelp(task)
      }

    }
    runHelp(this)
    this
  }

  def map[B](f: T => B): Task[B] = {
    val rst = value match {
      case None =>
        val p = Promise[B]()
        p.setFormer(this)
        onCompleted(x => p.completeWith(f(x)))
        p
      case Some(v) =>
        Eval(() => f(v))
    }

    rst

  }

  def flatmap[B](f: T => Task[B]): Task[B] = {
    val rst = value match {
      case None =>
        val p = Promise[B]()
        p.setFormer(this)
        onCompleted(x => {
          val r = f(x)
          //todo connect the former task by toEager
          //todo make to eager only run once
          r.run(priority)
          r.onCompleted(y => p.completeWith(y))
        })
        p
      case Some(v) =>
        Suspend(() => f(v))
    }

    rst
  }

  def foreach(f: T => Unit): Unit = {
    onCompleted(x => f(x))
  }


  /**
    * just execute it.
    * @param f
    */
  def onCompleted(f: T => Unit): Unit = lock.synchronized {
    value match {
      case None =>
        callbacks += f
      case Some(v) =>
        f(v)
    }

  }


}

case class Eval[T](v: () => T) extends Task[T] {
  override protected[chain] def toEager(): Unit = this.lock.synchronized{
    super.toEager()
    v()
  }
}

// never do a eager eval
//case class Eager[T](value: T) extends Task[T] { completeWith(value) }

/**
  * only promise could be completed
  */
case class Promise[T]() extends Task[T] {

  override protected[chain] def toEager(): Unit = this.lock.synchronized {
    super.toEager()
    value match {
      case Some(v) =>
        callbacks.foreach(x => x(v))
        callbacks.clear()
      case None =>
        ()
    }

  }

  /**
    * todo: wait `run` function called
    * todo: consider implement with lazy if not effect performance
    *
    * @param value
    * @return
    */
//  def completeWith(value: => T): Boolean = this.lock.synchronized {
  def completeWith(value: T): Boolean = this.lock.synchronized {
    this.value match {
      case None =>
        this.value = Some(value)
        true
      case Some(_) =>
        false
    }
  }

  protected[chain] def setFormer(task: Task[_]): Unit = {
     this.formerTask = Some(task)
  }
}


case class Suspend[T](v: () => Task[T]) extends Task[T] {
  override protected[chain] def toEager(): Unit = this.lock.synchronized{
    super.toEager()
    v().toEager()
  }
}

object Task {
  def apply[T](value: => T): Task[T] = new Eval[T](() => value)

}
