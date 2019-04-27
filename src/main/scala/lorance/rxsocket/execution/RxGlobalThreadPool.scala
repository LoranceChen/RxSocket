package lorance.rxsocket.execution

import java.util.concurrent.Executors

import lorance.rxsocket.session.Configration
import monix.execution.UncaughtExceptionReporter.LogExceptionsToStandardErr
import monix.execution.{Scheduler, UncaughtExceptionReporter}
import monix.execution.schedulers.{AsyncScheduler, ThreadFactoryBuilder}

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor}

/**
  * fork-join threads same as CPU count
  */
class RxGlobalThreadPool {

  private val workers: ExecutionContextExecutor = {
    ExecutionContext.fromExecutor(Executors.newWorkStealingPool(Configration.WORKTHREAD_COUNT))
  }

  implicit lazy val global: Scheduler =
    AsyncScheduler(
      Executors.newSingleThreadScheduledExecutor(),
      workers,
      UncaughtExceptionReporter.LogExceptionsToStandardErr,
      monix.execution.ExecutionModel.Default
    )
}
