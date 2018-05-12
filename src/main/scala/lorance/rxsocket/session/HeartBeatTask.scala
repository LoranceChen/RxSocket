//package lorance.rxsocket.session
//
//import java.nio.ByteBuffer
//
//import org.slf4j.LoggerFactory
//import lorance.rxsocket._
//import lorance.rxsocket.dispatch.{Task, TaskKey}
//
//class HeartBeatSendTask ( val taskId: TaskKey,
//                          loopAndBreakTimes: Option[(Int, Long)] = None, // None : no next, Some(int < 0)
//                          connectedSocket: ConnectedSocket) extends Task {
//  private val logger = LoggerFactory.getLogger(getClass)
//
//  // pre calculate next execute time to avoid deviation after execute
//  private val nextTime = loopAndBreakTimes match {
//    case Some((times, breakTime)) if times != 0 => //can calculate
//      Some(taskId.systemTime + breakTime)
//    case _ => None
//  }
//
//  //connect http server and do the action cmd
//  //when executed, tell Waiter Thread not return current thread
//  override def execute(): Unit = {
//    logger.info("execute send heart beat task")
//
//    connectedSocket.send(ByteBuffer.wrap(session.enCode(0.toByte, "heart beat")))
//  }
//
//  /**
//    * 1. use nextTime as new Task real execute time
//    * 2. ensure loopTime not decrease if it is < 0
//    */
//  override def nextTask: Option[Task] = {
//    nextTime.map(x => new HeartBeatSendTask(
//      TaskKey(taskId.id, x),
//      loopAndBreakTimes.map { case (loopTime, breakTime) =>
//        if(loopTime > 0) (loopTime - 1, breakTime)
//        else (loopTime, breakTime)
//      },
//      connectedSocket
//    ))
//  }
//}
//
//class HeartBeatCheckTask ( val taskId: TaskKey,
//                           loopAndBreakTimes: Option[(Int, Long)] = None, // None : no next, Some(int < 0)
//                           connectedSocket: ConnectedSocket) extends Task {
//  private val logger = LoggerFactory.getLogger(getClass)
//
//  // pre calculate next execute time to avoid deviation after execute
//  private val nextTime = loopAndBreakTimes match {
//    case Some((times, breakTime)) if times != 0 => //able calculate
//      Some(taskId.systemTime + breakTime)
//    case _ => None
//  }
//
//  override def execute(): Unit = {
//    logger.info("execute check heart beat task")
//
//    if(!connectedSocket.heart) {
//      logger.info(s"disconnected because of no heart beat response - ${connectedSocket.addressPair.remote}")
//      connectedSocket.disconnect
//    } else {
//      connectedSocket.heart = false
//    }
//  }
//
//  /**
//    * 1. use nextTime as new Task real execute time
//    * 2. ensure loopTime not decrease if it is < 0
//    */
//  override def nextTask: Option[Task] = {
//    nextTime.map(x => new HeartBeatCheckTask(
//      TaskKey(taskId.id, x),
//      loopAndBreakTimes.map { case (loopTime, breakTime) =>
//        if (loopTime > 0) (loopTime - 1, breakTime)
//        else (loopTime, breakTime)
//      },
//      connectedSocket
//    ))
//  }
//}
