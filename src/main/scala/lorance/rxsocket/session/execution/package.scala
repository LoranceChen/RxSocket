package lorance.rxsocket.session

import java.util.concurrent.{Executor, Executors}

import scala.concurrent.ExecutionContext

/**
  *
  */
package object execution {
  class CurrentThreadExecutor extends Executor {
    def execute( r: Runnable) = {
      r.run()
    }
  }

  /**
    * todo to test does it could ensure in current thread
    * @return
    */
  def currentThread = {
    val currentExe = new CurrentThreadExecutor
    ExecutionContext.fromExecutor(currentExe)
  }

  def customExecutionContent(count: Int) = new ExecutionContext {
    val threadPool = Executors.newWorkStealingPool(count)

    def execute(runnable: Runnable) = {
      threadPool.submit(runnable)
    }

    def reportFailure(t: Throwable) = {}
  }
}
