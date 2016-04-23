package lorance.rxscoket.session

import java.nio.ByteBuffer

import lorance.rxscoket.session
import lorance.rxscoket.session.exception.TmpBufferOverLoadException
import lorance.rxscoket.session.implicitpkg._
import lorance.rxscoket.log
import scala.annotation.tailrec
/**
  * one map to for every socket
  */
class ReaderDispatch(private var tmpProto: PaddingProto, maxLength: Int = Configration.TEMPBUFFER_LIMIT) {//extends Subject[]{
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

    def tryGetLength(bf: ByteBuffer, lengthOpt: Option[PendingLength]): Option[BufferedLength] = {
      val remaining = bf.remaining()
      lengthOpt match {
        case None =>
          if (remaining < 1) None
          else if (1 <= remaining && remaining < 4) {
            val lengthByte = new Array[Byte](4)
            bf.get(lengthByte, 0, remaining)
            Some(PendingLength(lengthByte, remaining))
          }
          else {
            val length = bf.getInt()
            log(s"${this.getClass.toString} : get length - $length", 15)
            Some(CompletedLength(length))
          }
        case pendingOpt @ Some(pendingLength) =>
          val need = 4 - pendingLength.arrivedNumber
          if (remaining >= need) {
            bf.get(pendingLength.arrived, pendingLength.arrivedNumber, need)
            Some(CompletedLength(pendingLength.arrived.toInt))
          } else {
            bf.get(pendingLength.arrived, pendingLength.arrivedNumber, remaining)
            pendingLength.arrivedNumber += remaining
            pendingOpt
          }
      }
    }

    /**
      * @param src
      * @param paddingProto has completed length
      * @return
      */
    def readLoad(src: ByteBuffer, paddingProto: PaddingProto) = {
      require(paddingProto.lengthOpt.isDefined)
      require(paddingProto.lengthOpt.get.isInstanceOf[CompletedLength])

      val length = paddingProto.lengthOpt.get.value//todo refactor
      if (length > maxLength) throw new TmpBufferOverLoadException()
      if (src.remaining() < length) {
        val newBf = ByteBuffer.allocate(length)
        tmpProto = PaddingProto(paddingProto.uuidOpt, paddingProto.lengthOpt, newBf.put(src))
        None
      } else {
        tmpProto = PaddingProto(None, None, session.EmptyByteBuffer)
        val newAf = new Array[Byte](length)
        src.get(newAf, 0, length)
        val completed = CompletedProto(paddingProto.uuidOpt.get, length, ByteBuffer.wrap(newAf))
        log(s"${this.getClass.toString} : get protocol - ${(completed.uuid, completed.length, new String(completed.loaded.array))}", -10)
        Some(completed)
      }
    }
    tmpProto match {
      case PaddingProto(None, _, _) =>
        val uuidOpt = tryGetByte(src)
        tmpProto = PaddingProto(uuidOpt, None, null)
        val lengthOpt = uuidOpt.flatMap{uuid =>
          log(s"${this.getClass.toString} : get uuid - $uuid", 15)
          tryGetLength(src, None)
        }
        val protoOpt = lengthOpt.flatMap {
          case CompletedLength(length) =>
            if (length > maxLength) throw new TmpBufferOverLoadException(s"length - ${length}")
            if(src.remaining() < length) {
              val newBf = ByteBuffer.allocate(length)
              tmpProto = PaddingProto(uuidOpt, lengthOpt, newBf.put(src))
              None
            } else {
              tmpProto = PaddingProto(None,None, session.EmptyByteBuffer)
              val newAf = new Array[Byte](length)
              src.get(newAf, 0, length)
              val completed = CompletedProto(uuidOpt.get, length, ByteBuffer.wrap(newAf))
              log(s"${this.getClass.toString} : get protocol - ${(completed.uuid, completed.length, new String(completed.loaded.array))}", -10)
              Some(completed)
            }
          case PendingLength(arrived, number) =>
            tmpProto = PaddingProto(uuidOpt, lengthOpt, session.EmptyByteBuffer)
            None
        }
        protoOpt match {
          case None => completes
          case Some(completed) =>
            if (completes.isEmpty) receiveHelper(src, Some(Vector(completed)))
            else receiveHelper(src, completes.map(_ :+ completed))
        }
      case padding @ PaddingProto(Some(uuid), None, _) =>
        val lengthOpt = tryGetLength(src, None)
        val protoOpt = lengthOpt.flatMap {
          case CompletedLength(length) =>
            tmpProto = PaddingProto(Some(uuid), lengthOpt, session.EmptyByteBuffer)
            readLoad(src, tmpProto)
          case PendingLength(_, _) =>
            tmpProto = PaddingProto(Some(uuid), lengthOpt, session.EmptyByteBuffer)
            None
        }
        protoOpt match {
          case None => completes
          case Some(completed) =>
            if (completes.isEmpty) receiveHelper(src, Some(Vector(completed)))
            else receiveHelper(src, completes.map(_ :+ completed))
        }
      case padding @ PaddingProto(Some(uuid), Some(pending @ PendingLength(arrived, number)), _) =>
        val lengthOpt = tryGetLength(src, Some(pending))
        val protoOpt = lengthOpt match {
          case Some(length @ CompletedLength(_)) =>
            val lengthedProto = PaddingProto(Some(uuid), lengthOpt, session.EmptyByteBuffer)
            readLoad(src, lengthedProto)
          case Some(PendingLength(arrived, number)) =>
            tmpProto = PaddingProto(Some(uuid), lengthOpt, session.EmptyByteBuffer)
            None
          case _ => ??? // NEVER arrived
        }
        protoOpt match {
          case None => completes
          case Some(completed) =>
            if (completes.isEmpty) receiveHelper(src, Some(Vector(completed)))
            else receiveHelper(src, completes.map(_ :+ completed))
        }
      case PaddingProto(Some(uuid), lengthOpt @ Some(CompletedLength(length)), padding) =>
        val protoOpt = if (padding.position() + src.remaining() < length) {
          tmpProto = PaddingProto(Some(uuid), lengthOpt, padding.put(src))
          None
        } else {
          tmpProto = PaddingProto(None, None, session.EmptyByteBuffer)
          val needLength =  length - padding.position()
          val newAf = new Array[Byte](needLength)
          src.get(newAf, 0, needLength)

          val completed = CompletedProto(uuid, length, padding.put(newAf))
          log(s"${this.getClass.toString} : get protocol - ${(completed.uuid, completed.length, new String(completed.loaded.array))}", -10)
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

/**
  * form now on, socket communicate length/lengthOpt with Int
  */
abstract class BufferedProto
case class PaddingProto(uuidOpt: Option[Byte],lengthOpt: Option[BufferedLength],loading: ByteBuffer)
case class CompletedProto(uuid: Byte,length: Int, loaded: ByteBuffer) extends BufferedProto

/**
  * length of the proto is represent by Int. It maybe under pending after once read form socket
  */
abstract class BufferedLength{def value: Int}
case class PendingLength(arrived: Array[Byte], var arrivedNumber: Int) extends BufferedLength{def value = throw new Exception("length not completed")}
case class CompletedLength(length: Int) extends BufferedLength{def value = length}