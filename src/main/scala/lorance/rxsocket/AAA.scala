package lorance.rxsocket

import scala.annotation.tailrec

object TestAA extends App {
  implicit val x = 10
  val string = "123"
  val xx = implicitly[Int]

  def xxx(implicit x: Int) = {
    x + 1
  }

  println(xx)
  println(xxx)
  @tailrec
  def fold(data: List[String],
           latestStr: String,
           latestTuple: (Int, Int),
           result: List[(Int, Int)]): List[(Int, Int)] = {
    if (data.isEmpty) { //结束
      result :+ latestTuple
    }
    else if (latestTuple == null) { //begin
      fold(data.tail, data.head, (0, 0), result :+ (0, 0))
    } else {
      //中间过程
      data match {
        case first :: tail =>
          if (first == latestStr) { // 匹配
            fold(tail, first,
              (latestTuple._1, latestTuple._2 + 1),
              result
            )
          } else { // 不匹配
            fold(tail, first,
              (latestTuple._2 + 1, latestTuple._2 + 1),
              result :+ latestTuple
            )
          }
        case _ =>
          throw new Exception("error")
      }

    }
  }

  val result = fold(List("1", "2", "2", "3", "4", "5", "5", "5", "10"), null, (0, 0), Nil)
  val result2 = fold(List("1", "2", "2", "3", "4", "5", "5", "5", "10"), null, (0, 0), Nil)
  println("result - " + result)

}