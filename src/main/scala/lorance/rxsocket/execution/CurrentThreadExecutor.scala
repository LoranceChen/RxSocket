package lorance.rxsocket.execution

import java.util.concurrent.Executor

class CurrentThreadExecutor extends Executor {
  def execute( r: Runnable): Unit = {
    r.run()
  }
}