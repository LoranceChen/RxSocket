package lorance.rxscoket.session

/**
  *
  */
object Configration {

  /**
    * class TempBuffer's limit field
    */
  var READBUFFER_LIMIT = 256

  /**
    * todo
    * class TempBuffer's limit field
    */
  var TEMPBUFFER_LIMIT = 1024 * 8 //byte

  /**
    * todo
    * proto number's bytes length
    * 1: Byte, 2: Short, 4: Int, 8: Long
    */
  var PROTO_NUMBER_LENGTH = 8

  /**
    * todo
    * proto package context Length (byte)
    */
  var PACKAGE_CONTEXT_LENGTH = Int.MaxValue
}
