# RxSocket - v0.9.7
socket with reactive style.

## Why do this project
Reactive programming has important concept of stream.It's very ease to apply data similar task, furthermore, network 
programming is designed as many layer for different data types. It's nature and useful if use Reactive Style to practice
a network data stream.At upper of TCP, I will beginning to complete basic function of Session Layer and Json Presentation 
Layer, it also allow anyone to redesign Presentation Layer easily.

###Example
todo

####UPDATE  
1. catch disconnected exception
2. add loop send msg simulate
3. fix Negative Exception when msg length capacity over 7 byte.
4. fix socket send message loss bug under multi-thread.
5. open limit length of load.  

2016.03.25  
1. change connect and read operation to a real observable stream  
2. test use case fix cold observable caused `ReadPendingException` by multi reading same socket.

v0.7.1 - 0.7.3
* adds json presentation extractor error log
* keep temp json task observable form leak.(can i call it will lead a gpu leak?)

v0.8.1
* fix bug: json presentation `sendWithResult` method NOT filter specify taskId.

v0.9.0
* fix bug: `ReaderDispatch` can't works in some situation...(so sorry)
* add concurrent dispatch `CompleteProto`

v0.9.1
* fix bug: `socket.write` operation NOT wait complete

v0.9.2
* use java ConcurrentHashMap at `JProtocol.sendWithResult` avoid lock data frequency.

v0.9.3
* extract Logger to `lorance.rxsocket.rxsocketLogger` class instance
* add heart beat in session

####Roadmap
* adds useful observable on special event
* handle reconnect and relative notification
* need read Queue and write Queue - ensure same request thread i/o socket with FIFO
* log method add class path: replace Int log level by readable words. Related by package, class and importance.
* to solve head-of-line blocking with multi socket
* add heart beat to test connect on works
* encapsulate taskId because it must be unique.
* add Model and Service concept to support specify Req an Rsp.
