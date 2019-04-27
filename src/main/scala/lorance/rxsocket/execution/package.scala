package lorance.rxsocket

import monix.execution.Scheduler

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor}

/**
  *
  */
package object execution {

  /**
    * run action in current thread
    */
  val currentThread: ExecutionContextExecutor = {
    val currentExe = new CurrentThreadExecutor
    ExecutionContext.fromExecutor(currentExe)
  }

  implicit val global: Scheduler = new RxGlobalThreadPool().global
//
//  def customExecutionContent(count: Int) = new ExecutionContext {
//    val threadPool = Executors.newWorkStealingPool(count)
//
//    def execute(runnable: Runnable) = {
//      threadPool.submit(runnable)
//    }
//
//    def reportFailure(t: Throwable) = {}
//  }

//  private[session] lazy val sendExecutor: ExecutionContextExecutor = {
//    val cpus = Runtime.getRuntime.availableProcessors
//    ExecutionContext.fromExecutor(Executors.newScheduledThreadPool(cpus * 2))
//  }

//  private[session] lazy implicit val readExecutor: ExecutionContextExecutor = {
//    val cpus = Runtime.getRuntime.availableProcessors
//    ExecutionContext.fromExecutor(Executors.newWorkStealingPool(cpus * 2))
//  }

//  private[session] lazy val waitExecutor: ExecutionContextExecutor = {
//    val cpus = Runtime.getRuntime.availableProcessors
//    ExecutionContext.fromExecutor(Executors.newWorkStealingPool(cpus * 2))
//  }
}
