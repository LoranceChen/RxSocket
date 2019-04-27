package tmptest

object HashSetTest extends App {

  private val ints = Set.empty[Int]
  a()

  val x = ints + 10
  val xx = x + 100
  val xxx = xx + 1000
  val xxx4 = xxx + 10000
  val xxx5 = xxx4 + 100000

  println(x)
  println(xx)
  println(xxx)

  val aa = 1

  def a (): Unit = {
    println("a")
  }
}

object ClassA {
  trait T1

  class C1 extends T1
  class C2 extends T1
}