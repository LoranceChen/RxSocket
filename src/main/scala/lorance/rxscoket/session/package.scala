package lorance.rxscoket

import java.nio.ByteBuffer
import lorance.rxscoket.session.implicitpkg._

package object session {
  val EmptyByteBuffer = ByteBuffer.allocate(0)

  def enCode(protoGroup: Byte, msg: String) = {
    val msgBytes = msg.getBytes
    val length = msgBytes.length.getByteArray
    val bytes = protoGroup +: length
    bytes ++ msgBytes
  }

  def deCode(bytePtoro: Array[Byte]) = {
    val uuid = bytePtoro(0)
    val length = Array(bytePtoro(1),bytePtoro(2),bytePtoro(3),bytePtoro(4)).toInt
    val load = bytePtoro.drop(5).string
    (uuid, length, load)
  }
}
