package lorance.rxsocket.session

import java.nio.ByteBuffer

import lorance.rxsocket.session
import lorance.rxsocket.session.implicitpkg._

class CommPassiveParser(private var tmpProto: PaddingProto) extends PassiveParser[CompletedProto](1) {

  def this() = {
    this(PaddingProto(None, None, session.EmptyByteBuffer))
  }

  override protected def passiveReceive(length: Int, data: Array[Byte]): (Int, Option[CompletedProto]) = {
    length match {
      case 1 => //proto type
        tmpProto = PaddingProto(Some(data(0)), None, session.EmptyByteBuffer)
        (4, None)
      case 4 => //load length
        val curLoadLength = data.toInt
        tmpProto = PaddingProto(tmpProto.uuidOpt, Some(CompletedLength(curLoadLength)), session.EmptyByteBuffer)
        (curLoadLength, None)
      case _ if length == tmpProto.lengthOpt.get.value => //load data
        (1, Some(CompletedProto(
          tmpProto.uuidOpt.get,
          tmpProto.lengthOpt.get.asInstanceOf[CompletedLength].length,
          ByteBuffer.wrap(data)
        )))
      case _ =>
        throw ProtoParseError(s"should NOT arrive - $length")

    }
  }
}
