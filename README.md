# RxSocket - v0.10.1
socket with reactive style.

## Why do this project
Reactive programming has a important concept of stream which allow application scale vertically and apply good multi-thread dispatch for concurrent event.Network programming is designed as many layer for different data types. It's nature and useful if use Reactive Style to implement network data stream.At upper of TCP, the library complete a basic function of Session Layer and Json Presentation Layer, it also allow anyone to redesign Presentation Layer easily.

### SBT Usage
- Scala 1.12.x
```
"com.scalachan" %% "rxsocket" % "0.10.1"
//or full name
"com.scalachan" % "rxsocket_2.12" % "0.10.1"
```
- Scala 1.11.x
```
"com.scalachan" %% "rxsocket" % "0.9.8"
//or full name
"com.scalachan" % "rxsocket_2.11" % "0.9.8"
```

### Maven Usage
- Scala 1.12.x
```
<dependency>
  <groupId>com.scalachan</groupId>
  <artifactId>rxsocket_2.12</artifactId>
  <version>0.10.1</version>
</dependency>
```
- Scala 1.11.x
```
<dependency>
  <groupId>com.scalachan</groupId>
  <artifactId>rxsocket_2.11</artifactId>
  <version>0.9.8</version>
</dependency>
```

### Dependency
* [RxScala](https://github.com/ReactiveX/RxScala)
* [lift-json](https://github.com/lift/lift/tree/master/framework/lift-base/lift-json)
* Java7 nio

### Feature
* do logic with reactive style with `Observable` and `Future`
* with RPC stream can get a publish-subscribe socket communicate.
* Asynchronous & Non-blocking

### Example
- [simple example in test/demo directory](https://github.com/LoranceChen/RxSocket/tree/master/src/test/scala/demo)
- [benchmark](https://github.com/LoranceChen/RxSocket/tree/master/src/test/scala/benchmark)

#### Roadmap
* encapsulate taskId because it must be unique. [x]
* add Model and Service concept to support specify Req an Rsp.
* split every proto msg(or inner proto data type) with specify notation.
