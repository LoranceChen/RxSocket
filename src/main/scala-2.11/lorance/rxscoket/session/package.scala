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
}
