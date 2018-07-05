package lorance.rxsocket.session

import java.nio.ByteBuffer

import lorance.rxsocket.session
import org.slf4j.LoggerFactory

import scala.annotation.tailrec

/**
  * how to handle ByteBuffer from receive
  * split the class to two class: 1. passive mode 2. active mode
  */
abstract class ProtoParser[Proto] {

  /**
    * handle src from socket reading call back
    * @param src
    * @return
    */
  def receive(src: ByteBuffer): Vector[Proto] = {
    src.flip()
    val rst = parser(src)
    src.clear()
    rst
  }

  protected def parser(src: ByteBuffer): Vector[Proto]

}

//abstract class ActiveProtoParser[Proto] extends ProtoParser[Proto]

abstract class PassiveParser[Proto](protected val initLength: Int, protected val initSymbol: Symbol) extends ProtoParser[Proto] {
  protected val logger = LoggerFactory.getLogger(getClass)

  private var nextLength = initLength
  private var nextSymbol = initSymbol

  //the parser want byte length for next loop, subclass should give it a init value
  assert(nextLength > 0)
  private var tmpBf: ByteBuffer = ByteBuffer.allocate(initLength)
  private var tmpLength: Int = initLength

  protected def passiveReceive(symbol: Symbol, length: Int, data: Array[Byte]): (Symbol, Int, Option[Proto])

  //invoke the Fn by socket reader
  protected override def parser(src: ByteBuffer): Vector[Proto] = {
    loop(src, Vector.empty[Proto])
  }

  @tailrec private def loop(src: ByteBuffer,
                            completes: Vector[Proto]): Vector[Proto] = {
    assert(nextLength <= session.Configration.TEMPBUFFER_LIMIT)
    val remaining = src.remaining()
//    logger.info(s"loop_data - $nextSymbol, $nextLength, ${tmpBf.position()}, ${tmpBf.limit()}, ${src.position()}, ${src.limit()}")

    if(remaining < nextLength) {//不足
      //全部加入到tmpBf中
      val myArr = new Array[Byte](remaining)
//      logger.info(s"part_data - $nextSymbol, $nextLength, ${tmpBf.position()}, ${tmpBf.limit()}, ${src.position()}, ${src.limit()}")
      src.get(myArr, 0, remaining)
      tmpBf.put(myArr)

      //更新状态
      nextLength = nextLength - remaining

      //返回结果
      completes
    } else { //足够
      //填充bf
      val myArr = new Array[Byte](nextLength)
//      logger.info(s"engouthdata - $nextSymbol, $nextLength, ${tmpBf.position()}, ${tmpBf.limit()}, ${src.position()}, ${src.limit()}")
      src.get(myArr, 0, nextLength)
      tmpBf.put(myArr)
      val (newSymbol, newLength, stageProto) = passiveReceive(nextSymbol, tmpLength, tmpBf.array())

      //更新状态
      val updatedProtos = stageProto match {
        case None => completes
        case Some(completedProto) => completes :+ completedProto
      }

      nextSymbol = newSymbol
      nextLength = newLength
      tmpBf = ByteBuffer.allocate(newLength)
      tmpLength = newLength

      //再次填充剩下的src
      loop(src, updatedProtos)
    }

  }

}