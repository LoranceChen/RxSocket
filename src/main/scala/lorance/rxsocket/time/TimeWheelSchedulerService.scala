//package lorance.rxsocket.time
//
//import java.util
//import java.util.concurrent._
//
//import lorance.rxsocket.execution.global
//import io.netty.util.HashedWheelTimer
//import monix.eval.Task
//
//import scala.concurrent.duration.Duration
//import scala.concurrent.{CanAwait, ExecutionContext, Promise, Future => SFuture}
//import scala.util.Try
//
//trait TAbstractExecutorService extends AbstractExecutorService
//
//class TimeWheelSchedulerService(hashedWheelTimer: HashedWheelTimer) extends ScheduledExecutorService with TAbstractExecutorService {
//  @volatile
//  private var isStoped = false
//  override def schedule(command: Runnable, delay: Long, unit: TimeUnit): ScheduledFuture[_] = {
//    hashedWheelTimer.newTimeout(_ => {
//      execute(command)
//    }, delay, unit)
//  }
//
//  override def shutdown(): Unit = {
//    hashedWheelTimer.stop()
//    isStoped = true
//  }
//
//  override def shutdownNow(): util.List[Runnable] = {
//    hashedWheelTimer.stop()
//    isStoped = true
//
//    return null
//  }
//
//  override def isShutdown: Boolean = isStoped
//
//  override def awaitTermination(timeout: Long, unit: TimeUnit): Boolean = null
//
//  override def execute(command: Runnable): Unit = {
//    global.execute(command)
//  }
//
//  override def schedule[V](callable: Callable[V], delay: Long, unit: TimeUnit): ScheduledFuture[V] = {
//    val p = new SimpleScheduledFuture[V]
//    hashedWheelTimer.newTimeout(_ => {
//      execute(() => {
//        val rst: Try[V] = Try{
//          callable.call()
//        }
//        p.tryComplete(rst)
//      })
//    }, delay, unit)
//    p
//  }
//
//  override def scheduleAtFixedRate(command: Runnable, initialDelay: Long, period: Long, unit: TimeUnit): ScheduledFuture[_] = {
//
//    val p: SimpleScheduledFuture[Unit] = new SimpleScheduledFuture[Unit]
//    val executeTimeMill = System.currentTimeMillis() + unit.toMillis(initialDelay)
//
//    val nextExecuteTimeMill = executeTimeMill + unit.toMillis(period)
//
//    // add next time command
//    p.map(_ => {
//      val nextDelayMill = nextExecuteTimeMill - System.currentTimeMillis()
//      val nextDelay =
//        if(nextDelayMill <= 0L) {
//          0L
//        } else {
//          unit.convert(nextDelayMill, TimeUnit.MILLISECONDS)
//        }
//
//      scheduleAtFixedRate(command, nextDelay, period, unit)
//
//    })
//
//    hashedWheelTimer.newTimeout(_ => {
//      execute(() => {
//        val rst: Try[Unit] = Try{
//          command.run()
//        }
//        p.tryComplete(rst)
//      })
//    }, initialDelay, unit)
//    p
//
//  }
//
//  override def scheduleWithFixedDelay(command: Runnable, initialDelay: Long, delay: Long, unit: TimeUnit): ScheduledFuture[_] = {
//
//    val p: SimpleScheduledFuture[Unit] = new SimpleScheduledFuture[Unit]
////    val executeTimeMill = System.currentTimeMillis() + unit.toMillis(initialDelay)
////
////    val nextExecuteTimeMill = executeTimeMill + unit.toMillis(delay)
////
////    // add next time command
////    p.map(_ => {
////      val nextDelayMill = nextExecuteTimeMill - System.currentTimeMillis()
////      val nextDelay =
////        if(nextDelayMill <= 0L) {
////          0L
////        } else {
////          unit.convert(nextDelayMill, TimeUnit.MILLISECONDS)
////        }
////
////      scheduleAtFixedRate(command, nextDelay, delay, unit)
////
////    })
//
//    hashedWheelTimer.newTimeout(_ => {
//      execute(() => {
//        val rst: Try[Unit] = Try{
//          command.run()
//        }
//
//        //gen next task
//        scheduleWithFixedDelay(
//          command,
//          0L,
//          delay,
//          unit
//        )
//
//        p.tryComplete(rst)
//      })
//    }, initialDelay, unit)
//    p
//
//  }
//
//  override def isTerminated: Boolean = isStoped
//
//  private final class SimpleScheduledFuture[V]() extends ScheduledFuture[V] with SFuture[V] with Promise[V] {
//    override def getDelay(unit: TimeUnit): Long = ???
//
//    override def cancel(mayInterruptIfRunning: Boolean): Boolean = ???
//
//    override def isCancelled: Boolean = ???
//
//    override def isDone: Boolean = ???
//
//    override def get(timeout: Long, unit: TimeUnit): V = ???
//
//    override def compareTo(o: Delayed): Int = ???
//
//    override def get(): V = ???
//
//    override def future: SFuture[V] = ???
//
//    override def isCompleted: Boolean = ???
//
//    override def tryComplete(result: Try[V]): Boolean = ???
//
//    override def onComplete[U](f: Try[V] => U)(implicit executor: ExecutionContext): Unit = ???
//
//    override def value: Option[Try[V]] = ???
//
//    override def transform[S](f: Try[V] => Try[S])(implicit executor: ExecutionContext): SFuture[S] = ???
//
//    override def transformWith[S](f: Try[V] => SFuture[S])(implicit executor: ExecutionContext): SFuture[S] = ???
//
//    override def ready(atMost: Duration)(implicit permit: CanAwait): SimpleScheduledFuture.this.type = ???
//
//    override def result(atMost: Duration)(implicit permit: CanAwait): V = ???
//  }
//}
