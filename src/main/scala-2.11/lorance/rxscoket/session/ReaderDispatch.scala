package lorance.rxscoket.session

import java.nio.ByteBuffer

import lorance.rxscoket.session
import lorance.rxscoket.session.exception.TmpBufferOverLoadException

import scala.annotation.tailrec

/**
  * one map to for every socket
  */
class ReaderDispatch(private var tmpProto: PaddingProto) {//extends Subject[]{
  val maxLength = 256

  def this() {
    this(PaddingProto(None, None, session.EmptyByteBuffer))
  }

  def receive(src: ByteBuffer) = {
    src.flip()
    val rst = receiveHelper(src, None)
    src.clear()
    rst
  }

  /**
    * read all ByteBuffer form src, put those data to Observer or cache to dst
    * handle src ByteBuffer from network
    *
    * @param src
    * @return None, if uuidOpt or lengthOpt is None
    */
  @tailrec private def receiveHelper(src: ByteBuffer, completes: Option[Vector[CompletedProto]]): Option[Vector[CompletedProto]] = {
    def tryGetByte(bf: ByteBuffer) = if(bf.remaining() >= 1) Some(bf.get()) else None

    tmpProto match {
      case PaddingProto(None, _, _) =>
        val uuidOpt = tryGetByte(src)
        val lengthOpt = uuidOpt.flatMap{uuid => tryGetByte(src)}
        val protoOpt = lengthOpt.flatMap { length =>
          if (length > maxLength) throw new TmpBufferOverLoadException()
          if(src.remaining() < length) {
            val newBf = ByteBuffer.allocate(length)
            tmpProto = PaddingProto(uuidOpt, lengthOpt, newBf.put(src))
            None
          } else {
            tmpProto = PaddingProto(None,None, session.EmptyByteBuffer)
            val newAf = new Array[Byte](length)
            src.get(newAf, 0, length)
            val completed = CompletedProto(uuidOpt.get, lengthOpt.get, ByteBuffer.wrap(newAf))
            Some(completed)
          }
        }
        protoOpt match {
          case None => completes
          case Some(completed) =>
            if (completes.isEmpty) receiveHelper(src, Some(Vector(completed)))
            else receiveHelper(src, completes.map(_ :+ completed))
        }
      case PaddingProto(Some(uuid), None, tmpBf) =>
        val lengthOpt = tryGetByte(src)
        val protoOpt = lengthOpt.flatMap { length =>
          if (length > maxLength) throw new TmpBufferOverLoadException()
          if (src.remaining() < length) {
            val newBf = ByteBuffer.allocate(length)
            tmpProto = PaddingProto(Some(uuid), lengthOpt, newBf.put(src))
            None
          } else {
            tmpProto = PaddingProto(None,None, session.EmptyByteBuffer)
            val newAf = new Array[Byte](length)
            src.get(newAf, 0, length)
            val completed = CompletedProto(uuid, lengthOpt.get, ByteBuffer.wrap(newAf))
            Some(completed)
          }
        }
        protoOpt match {
          case None => completes
          case Some(completed) =>
            if (completes.isEmpty) receiveHelper(src, Some(Vector(completed)))
            else receiveHelper(src, completes.map(_ :+ completed))
        }
      case PaddingProto(Some(uuid), Some(length), padding) =>
        val protoOpt = if (padding.position() + src.remaining() < length) {
          tmpProto = PaddingProto(Some(uuid), Some(length), padding.put(src))
          None
        } else {
          tmpProto = PaddingProto(None, None, session.EmptyByteBuffer)
          val needLength =  length - padding.position()
          val newAf = new Array[Byte](needLength)
          src.get(newAf, 0, needLength)
          val completed = CompletedProto(uuid, length, padding.put(newAf))
          Some(completed)
        }
        protoOpt match {
          case None => completes
          case Some(completed) =>
            if (completes.isEmpty) receiveHelper(src, Some(Vector(completed)))
            else receiveHelper(src, completes.map(_ :+ completed))
        }
    }
  }
}

abstract class BufferedProto
case class PaddingProto(uuidOpt: Option[Byte],lengthOpt: Option[Byte],loading: ByteBuffer)
case class CompletedProto(uuid: Byte,length: Byte, loaded: ByteBuffer) extends BufferedProto
