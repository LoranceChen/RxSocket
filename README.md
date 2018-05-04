# RxSocket - socket with reactive style.

## Why do this project
Reactive programming has a important concept of stream which allow application scale vertically and apply good multi-thread dispatch for concurrent event.Network programming is designed as many layer for different data types. It's nature and useful if use Reactive Style to implement network data stream.At upper of TCP, the library complete a basic function of Session Layer and Json Presentation Layer.

### SBT Usage
from `0.12.0`, only support Scala `2.12.x` and `2.11.x`
```
"com.scalachan" %% "rxsocket" % "0.12.0"
```

### Feature
- do logic with reactive style with `Observable` and `Future`
- with RPC stream can get a pipe-like socket communicate.
- Asynchronous & Non-blocking
- Json based Server mode make it easy to build TCP service.

### Example
- [simple example in test/demo directory](https://github.com/LoranceChen/RxSocket/tree/master/src/test/scala/demo)

### Not good parts
- lack of monitor for net status
- No benchmark yet

### TODO
- complete back-pressure mechanism [x]
- custom protocol parser
 	- do like Erlang bit pattern match: `<<1::size(8), length::big-32, json::binary>>`
 	- DSL implement as http4s route match DSL
- combine heartbeat timer task
:: currently, every socket has heartbeat timer task which occupy many resource for scheduler.
