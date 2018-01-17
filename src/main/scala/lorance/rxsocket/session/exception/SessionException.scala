package lorance.rxsocket.session.exception

/**
  * UUID use 0x1000000L ~ 0x1000,00ffL
  */
class SessionException(msg: String) extends Throwable(msg)

class ReadByteBufferException(msg: String) extends SessionException(msg)

//class UUIDNotEnoughException(msg: String = "ByteBuffer NOT contains UUID Length(4Byte)") extends ReadByteBufferException(msg, 0x1001)
class ReadResultNegativeException(msg: String = "CompletedHandler return -1") extends ReadByteBufferException(msg)

class TmpBufferOverLoadException(msg: String = "protocol length too long") extends SessionException(msg)