//package tagedfuture
//
//class Future[+T](f: => T)(implicit tag: String) {
//
//  def map[B](f2: T => B): Future[B] = {
//    Future(f2(f))
//  }
//
//  def flatMap[B](f2: T => Future[B]): Future[B] = {
//    f2(f)
//  }
//
//  def getTag: String = tag
//}
//
//object Future {
//  def apply[T](f: => T)(implicit tag: String): Future[T] = new Future(f)
//
//}
//
//object Test extends App {
//  val fur = Future{
//    10
//  }("future-chain-1")
//
//  val fur2 = fur.map(x => x * 2)
//
//  val fur3 = fur2.flatMap(x => {
//    anotherFuture(x)(fur2.getTag)
//  })
//
//  def anotherFuture(int: Int)(implicit tag: String): Future[String] = {
//    Future(int.toString)
//  }
//
//  assert(fur.getTag == fur2.getTag && fur2.getTag == fur3.getTag)
//
//}