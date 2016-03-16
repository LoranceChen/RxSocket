# RxSocket - v0.5f
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
ForkJoinPool-1-worker-13: read success - 10 bytes
ForkJoinPool-1-worker-13: read success - 10 bytes
ForkJoinPool-1-worker-13: get info - hello server!, uuid: 1, length: 13
ForkJoinPool-1-worker-13: read success - 10 bytes
ForkJoinPool-1-worker-13: read success - 10 bytes
ForkJoinPool-1-worker-13: get info - 北京,你好!, uuid: 1, length: 14
```  

####UPDATE  
1. catch disconnected exception
2. add loop send msg simulate
3. fix Negative Exception when msg length capacity over 7 byte.
4. fix socket send message loss bug under multi-thread.
5  open limit length of load.  
