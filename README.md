# RxSocket - v0.7.3
socket with reactive style.

## Example

### Client
See `DemoClientMainTest.scala` in test directory

#### Output
```
Thread-9: linked to server success
Thread-9: send completed result - 37
```

### Server
See `DemoServerMain.scala` in test directory

####Output
```
main: Server is listening at - localhost/127.0.0.1:10002
Thread-9: connect - success
ForkJoinPool-1-worker-13: client connected
ForkJoinPool-1-worker-13: Hi, Mike, someone connected - 
ForkJoinPool-1-worker-13: Hi, John, someone connected - 
ForkJoinPool-1-worker-13: first subscriber get protocol - hello server!
ForkJoinPool-1-worker-13: first subscriber get protocol - 北京,你好!
ForkJoinPool-1-worker-13: second subscriber get protocol - hello server!
ForkJoinPool-1-worker-13: second subscriber get protocol - 北京,你好!
```  

####UPDATE  
1. catch disconnected exception
2. add loop send msg simulate
3. fix Negative Exception when msg length capacity over 7 byte.
4. fix socket send message loss bug under multi-thread.
5. open limit length of load.  

2016.03.25  
1. change connect and read operation to a real observable stream  
2. test use case fix cold observable caused `ReadPendingException` by multi reading same socket.

v0.7.1 - .3
* adds json presentation extractor error log
* keep temp json task observable form leak. (Does it works?)

####Roadmap
* completely multi-thread support
* json format communicate easy and scalable
* adds try-catch for breakable event
* adds useful observable on special event
* handle reconnect and relative notification.
* setting async operates timeout limit.