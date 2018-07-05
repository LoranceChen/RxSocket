package example.benchmark

import lorance.rxsocket.dispatch.ObjectId

object ObjeIdTest extends App {
  @volatile var x0 = (1 to 10000000).map( _ => new ObjectId())

  val a = System.nanoTime()

  //9957
  val x1 = new ObjectId()
  val x2 = new ObjectId()
  val x3 = new ObjectId()
  val x4 = new ObjectId()
  val x5 = new ObjectId()
  val x6 = new ObjectId()
  val x7 = new ObjectId()
  val x8 = new ObjectId()
  val x9 = new ObjectId()

  //34177
//  val xx1 = System.nanoTime() + Thread.currentThread().getId
//  val xx2 = System.nanoTime()+ Thread.currentThread().getId
//  val xx3 = System.nanoTime()+ Thread.currentThread().getId
//  val xx4 =System.nanoTime()+ Thread.currentThread().getId
//  val xx5 = System.nanoTime()+ Thread.currentThread().getId
//  val xx6 = System.nanoTime()+ Thread.currentThread().getId
//  val xx7 =System.nanoTime()+ Thread.currentThread().getId
//  val xx8 = System.nanoTime()+ Thread.currentThread().getId
//  val xx9 = System.nanoTime()+ Thread.currentThread().getId

  /*
  pure nano time - 10450
5b39fca477c8473cadc18bbf
5b39fca477c8473cadc18bc0
5b39fca477c8473cadc18bc1
5b39fca477c8473cadc18bc2
5b39fca477c8473cadc18bc3
5b39fca477c8473cadc18bc4
5b39fca477c8473cadc18bc5
5b39fca477c8473cadc18bc6
5b39fca477c8473cadc18bc7
   */
  val b = System.nanoTime()

  val time = b - a
  println("nano time - " + time)

  println(x1)
  println(x2)
  println(x3)
  println(x4)
  println(x5)
  println(x6)
  println(x7)
  println(x8)
  println(x9)

//  println(xx1)
//  println(xx2)
//  println(xx3)
//  println(xx1)
//  println(xx5)
//  println(xx6)
//  println(xx7)
//  println(xx8)
//  println(xx9)
}
