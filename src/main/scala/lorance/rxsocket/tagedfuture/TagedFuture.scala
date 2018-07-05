package lorance.rxsocket.tagedfuture

import monix.eval.Task

class TagedFuture[+A](valueF: => A) {
  def map[B](f: A => B): TagedFuture[B] = {
    new TagedFuture(f(valueF))
  }

  def flatmap[B](f: A => TagedFuture[B]): TagedFuture[B] = {
    f(valueF)
  }

  def async() = {

  }
}

object TestTask extends App {
  import monix.execution.Scheduler.Implicits.global
  def test1 = {
    val t1 = Task {
      println("1")
      1 + 1
    }

    val t2 = t1.flatMap { x =>
      Task {
        println("2-1")
        x.toString
      }.flatMap{x =>

        Task {
          println("2-2")
          x.toString
        }
      }
    }

    t2.runAsync.foreach(x => println(s"completed - $x"))
  }

  test1

  Thread.currentThread().join()
}