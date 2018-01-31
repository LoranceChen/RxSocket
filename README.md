# RxSocket - socket with reactive style.

## Why do this project
Reactive programming has a important concept of stream which allow application scale vertically and apply good multi-thread dispatch for concurrent event.Network programming is designed as many layer for different data types. It's nature and useful if use Reactive Style to implement network data stream.At upper of TCP, the library complete a basic function of Session Layer and Json Presentation Layer.

### SBT Usage
```
"com.scalachan" %% "rxsocket" % "0.11.0"
```

### Feature
- do logic with reactive style with `Observable` and `Future`
- with RPC stream can get a pipe-like socket communicate.
- Asynchronous & Non-blocking
- Json based Server mode make it easy to build TCP service.

### Example
- [simple example in test/demo directory](https://github.com/LoranceChen/RxSocket/tree/master/src/test/scala/demo)
- [benchmark](https://github.com/LoranceChen/RxSocket/tree/master/src/test/scala/benchmark)

### Not good parts
- lack of monitor for net status
- difficult to custom protocol
