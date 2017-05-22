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
    * class TempBuffer's limit field
    */
  var TEMPBUFFER_LIMIT = 1024 * 10 //10k byte

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

  var CONNECT_TIME_LIMIT = 7 // second

  var SEND_HEART_BEAT_BREAKTIME = 30 //second
  var CHECK_HEART_BEAT_BREAKTIME = 40 // Should large then SEND_HEART_BEAT_BREAKTIME
}
