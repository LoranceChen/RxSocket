package chain

object Test extends App {
  val p = Promise[Int]()

  val p1 = p.map(x => {
    println("p.map to string")
    x.toString
  })

  val p2 = p1.flatmap(x => {
    println("p1.flatmap to -----")

    Eval(() => x + "-----")
  })

  p.completeWith(1000)

  println("run after 5s")
  Thread.sleep(5 * 1000)
  p2.run(10)
//  p2.run(10)

  println("run game over")

  Thread.currentThread().join()
}
