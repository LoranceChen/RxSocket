package lorance.rxsocket.session

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

  var CONNECT_TIME_LIMIT = 10 // second

  val WORKTHREAD_COUNT = 16//Runtime.getRuntime.availableProcessors / 1

  // todo need used by CPU etc. it's better limit CPU under 80%
  //      could it be adjust at dynamic runtime?
  // monitor： 频繁的触发被压就说明需要扩展了。
  val BACKPRESSURE = WORKTHREAD_COUNT * 1000 // handle message count

  //  var SEND_HEART_BEAT_BREAKTIME = 30 //second
//  var CHECK_HEART_BEAT_BREAKTIME = 60 // Should large then SEND_HEART_BEAT_BREAKTIME
}
