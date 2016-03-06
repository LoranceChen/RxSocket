package lorance.rxscoket.session

import java.nio.ByteBuffer

/**
  * most of them is decode and encode.
  * All of them use Big-Endian for unification
  */
package object implicitpkg {
  //expend class
  implicit def exByteBuffer(byteBuffer: ByteBuffer): ByteBufferEx = new ByteBufferEx(byteBuffer)
  implicit def exInt(bf: Int): IntEx = new IntEx(bf)
  implicit def exArrayBuffer(arrayByte: Array[Byte]): ArrayByteEx = new ArrayByteEx(arrayByte)
  /**
    * use Upper word at beginning make it as a dependence function
    */
  implicit def StringToByteBuffer(string: String): ByteBuffer = ByteBufferEx.stringToByteBuffer(string)
  implicit def StringToByteArray(string: String): Array[Byte] = ByteBufferEx.stringToByteArray(string)
}
