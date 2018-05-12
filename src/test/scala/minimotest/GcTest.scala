package minimotest

import monix.execution.Ack.Continue
import monix.reactive.subjects.PublishSubject
import monix.execution.Scheduler.Implicits.global

object GcTest extends App {
  demo.tool.Tool.showPid

  case class MyStrcut(x: Int)
  val pub = PublishSubject[MyStrcut]()

  pub.subscribe(x => {
    println(x)
    Continue
  })

  def loop(count: Int = 10): Unit = {
    if(count != 0)
      pub.onNext(new MyStrcut(10)).map(x => loop(count - 1))(scala.concurrent.ExecutionContext.Implicits.global
    )

  }

  loop()

  demo.tool.Tool.createGcThread(1000 * 10)

  Thread.currentThread().join()
}
