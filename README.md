# RxSocket - socket with reactive style.

## Why do this project
Reactive programming has a important concept of stream which allow application scale vertically and apply good multi-thread dispatch for concurrent event.Network programming is designed as many layer for different data types. It's nature and useful if use Reactive Style to implement network data stream.At upper of TCP, the library complete a basic function of Session Layer and Json Presentation Layer.

### SBT Usage
From `v0.12.0`, RxSocket only support Scala `2.12.x` and `2.11.x`
```
"com.scalachan" %% "rxsocket" % "0.13.1"
```

### Features
- do logic with reactive style with `Observable` and `Future`
- with RPC stream can get a pipe-like socket communicate.
- Asynchronous & Non-blocking
- Json based Server mode make it easy to build TCP service.
- consume data with back-pressure
- custom protocol parser which could be active mode or passive mode

### Example
- [simple example in test/demo directory](https://github.com/LoranceChen/RxSocket/tree/master/src/test/scala/demo)
- [simple benchmark in test/benchmark directory](https://github.com/LoranceChen/RxSocket/tree/master/src/test/scala/benchmark)

### TODO
- support heartbeat
    - timing wheel
- priority scheduler thread pool
    - avoid request callback re-enqueue to thread pool(forkjoinpool now) caused timeout.
    - allow handler massive request and give a slight performance affect
        - give a space to monitor network request status
- better benchmark
- concurrently handle request for every request.
    - replace Monix: Monix for async operation but concurrent.
    - need backpressure
- support backpressure at thread-pool level.
- support parser work with filter, such as support String 'aaaa##bbbb##' which '##' as protocol 
split.

- 添加backpressure
- 添加断线重连
    - 通过实现DisconnectSocket类，然后包装ConnectedSocket和DisconnectSocket统一接口
- 自定义线程池
    - 实现java SchedulerFuture，实现scala Future。注意：SchedulerFuture需要支持cancel操作，可重复的定时任务
        需要支持取消操作——已经不是不可变的future了