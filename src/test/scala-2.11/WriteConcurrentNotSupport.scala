import java.nio.ByteBuffer
import java.util.concurrent.TimeUnit

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.concurrent.ExecutionContext.Implicits.global
import lorance.rxscoket._

/**
  * socket.write is NOT support concurrent
  */
object WriteConcurrentNotSupport extends DemoClientMain{
  def main(arg: Array[String]): Unit = {
    init
    val longMsg = """{"taskId": "threadname-timestamp" , "dataBase": "helloworld", "collection": "test", "method": "find", "params":{"match":{"name": "insertTest02"}}}"""

    //these code proof send is safe on one thread.
    val waitedS = Await.result(socket,Duration.apply(5, TimeUnit.SECONDS))
    val msg = """{"taskId": "threadname-timestamp" , "dataBase": "helloworld", "collection": "test", "method": "find", "params":{"match":{"name": "insertTest02"}}}"""
    val msg2 = """{"taskId222222": "threadname-timestamp" , "dataBase": "helloworld", "collection": "test", "method": "find", "params":{"match":{"name": "insertTest02"}}}"""
    val data = ByteBuffer.wrap(enCoding(msg))
    val data2 = ByteBuffer.wrap(enCoding(msg2))
    waitedS.send(data)
    waitedS.send(data2)

    //  //multi-thread will lose some message because of write operation is on going.
    //    socket.flatMap{s =>
    //      val firstMsg = enCoding("hello server!")
    //  //    val secondMsg = enCoding("北京,你好!")
    //  //    val longMS = enCoding(longMsg)
    //
    //      log(s"send one - length - ${firstMsg.length}")
    //      val data = ByteBuffer.wrap(firstMsg)
    //  //    val data = ByteBuffer.wrap(firstMsg ++ secondMsg ++ longMS)
    //      val f= s.send(data)
    //      Thread.sleep(10000)
    //      f
    //    }
    //    socket.flatMap{ s =>
    //      log(s"send two - length - ${data.array().length}")
    //      val f = s.send(data)
    //      log(s"send three - length - ${data2.array().length}")
    //      val f2 = s.send(data2)
    //      f
    //    }
    //    socket.flatMap{ s =>
    //      log(s"send three - length - ${data2.array().length}")
    //      val f = s.send(data2)
    //      f
    //    }

    Thread.currentThread().join()
  }





}
