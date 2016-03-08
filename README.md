# RxSocket
socket with reactive style.

## Example

### Client
```
import java.nio.ByteBuffer

import lorance.rxscoket.session.ClientEntrance
import lorance.rxscoket._
import scala.concurrent.ExecutionContext.Implicits.global

object DemoClientMain extends App {
  val client = new ClientEntrance("localhost", 10001)
  val socket = client.connect

  val send = socket.flatMap{s =>
    def enCoding(msg: String) = {
      val msgBytes = msg.getBytes
      val bytes = Array[Byte](1,msgBytes.length.toByte)
      bytes ++ msgBytes
    }
    val firstMsg = enCoding("hello server!")
    val secondMsg = enCoding("北京,你好!")
    val data = ByteBuffer.wrap(firstMsg ++ secondMsg)
    s.send(data)
  }

  val reading = socket.map(_.startReading)
  reading.map{r =>
    r.subscribe{protos =>
      protos.map{ proto =>
        val context = new String(proto.loaded.array())
        log(s"get info - $context, uuid: ${proto.uuid}, length: ${proto.length}")
        context
      }
    }
  }

  Thread.currentThread().join()
}
```

#### Output
```
Connected to the target VM, address: '127.0.0.1:61276', transport: 'socket'
Thread-9: linked to server success
Thread-9: send result - 31
```

### Server
```
import lorance.rxscoket._
import lorance.rxscoket.session.ServerEntrance

object DemoServerMain extends App {
  val server = new ServerEntrance("localhost", 10001)
  val socket = server.listen
  val read = socket.flatMap(_.startReading)

  read.subscribe{ protos =>
    protos.map{ proto =>
      val context = new String(proto.loaded.array())
      log(s"get info - $context, uuid: ${proto.uuid}, length: ${proto.length}")
      context
    }
  }

  Thread.currentThread().join()
}
```

####Output
```
main: Server is listening at - localhost/127.0.0.1:10001
Thread-9: connect - success
ForkJoinPool-1-worker-13: client connected
ForkJoinPool-1-worker-13: read success - [B@1129e10
ForkJoinPool-1-worker-13: read success - [B@1129e10
ForkJoinPool-1-worker-13: get info - hello server!, uuid: 1, length: 13
ForkJoinPool-1-worker-13: read success - [B@1129e10
ForkJoinPool-1-worker-13: read success - [B@1129e10
ForkJoinPool-1-worker-13: get info - 北京,你好!, uuid: 1, length: 14

```  

####UPDATE  
1. catch disconnected exception
2. add loop send msg simulate