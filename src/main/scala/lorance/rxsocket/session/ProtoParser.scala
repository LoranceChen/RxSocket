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

abstract class PassiveParser[Proto](protected val initLength: Int) extends ProtoParser[Proto] {
  protected val logger = LoggerFactory.getLogger(getClass)

  private var nextLength = initLength
  private var tmpNextLength = -1//needn't tmp length

  //the parser want byte length for next loop, subclass should give it a init value
  assert(nextLength > 0)
  var tmpBf: ByteBuffer = ByteBuffer.allocate(initLength)

  protected def passiveReceive(length: Int, data: Array[Byte]): (Int, Option[Proto])

  //invoke the Fn by socket reader
  protected override def parser(src: ByteBuffer): Vector[Proto] = {
    loop(nextLength, src, Vector.empty[Proto])
  }

  /**
    * implement a passive mode loop
    * @param length
    * @param src
    * @param completes
    * @return
    */
  @tailrec private def loop(length: Int, src: ByteBuffer, completes: Vector[Proto]): Vector[Proto] = {
    assert(length <= session.Configration.TEMPBUFFER_LIMIT)
    val remaining = src.remaining()

    if(tmpNextLength != -1) {// deal with uncompleted tmp length
      if(remaining < tmpNextLength) {//tmp length依然不足
        val newBf = new Array[Byte](remaining)
        src.get(newBf, 0, remaining)
        tmpBf.put(newBf)
        tmpNextLength = tmpNextLength - remaining
        completes
      } else {//足够填充tmp length
        val newBf = new Array[Byte](tmpNextLength)
        src.get(newBf, 0, tmpNextLength)
        tmpBf.put(newBf)
        tmpNextLength = -1 //complete tmp next length
        val (curNextLength, protoRst) = passiveReceive(length, tmpBf.array())
        tmpBf = ByteBuffer.allocate(curNextLength)
        nextLength = curNextLength
        loop(nextLength, src, protoRst.foldLeft(completes)((olds, `new`) => olds :+ `new`))
      }

    } else { //不需要tmpLength
      if(remaining < length) {//收取的数据个数少于需要的数据个数：保存到临时消息中
        val newBf = new Array[Byte](remaining)
        src.get(newBf, 0, remaining)
        tmpBf.put(newBf)
        tmpNextLength = length - remaining //set tmp next length
        completes
      } else {//收取的数据足够
        val newBf = new Array[Byte](length)
        src.get(newBf, 0, length)
        tmpBf.put(newBf)
        val (curNextLength, protoRst) = passiveReceive(length, tmpBf.array())
        tmpBf = ByteBuffer.allocate(curNextLength)
        nextLength = curNextLength
        loop(nextLength, src, protoRst.foldLeft(completes)((olds, `new`) => olds :+ `new`))
      }
    }


  }


}