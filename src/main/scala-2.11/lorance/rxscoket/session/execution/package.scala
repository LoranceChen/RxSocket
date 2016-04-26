package lorance.rxscoket.session

import java.util.concurrent.{Executor, Executors}

import scala.concurrent.ExecutionContext

/**
  *
  */
package object execution {
  class CurrentThreadExecutor extends Executor {
    def execute( r: Runnable) {
      r.run()
    }
  }
  implicit val currentThread = {
    val currentExe = new CurrentThreadExecutor
    val e = ExecutionContext.fromExecutor(currentExe)
    e
  }

  def singleExecutionContent = new ExecutionContext {
    val threadPool = Executors.newFixedThreadPool(1)

    def execute(runnable: Runnable) {
      threadPool.submit(runnable)
    }

    def reportFailure(t: Throwable) {}
  }
}
