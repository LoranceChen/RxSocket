import lorance.rxscoket.session.{ClientEntrance, ServerEntrance}

object OutNet extends App{
//  val server = new ServerEntrance("192.168.1.111", 12010)
  lorance.rxscoket.rxsocketLogger.logLevel = 20
  val server = new ServerEntrance("127.0.0.1", 12010)
  val ls = server.listen
  ls.subscribe(s =>
    println(s"client connected - ${s.addressPair.remote}")
  )

  val reader = ls.flatMap{s =>
    s.startReading.map(s.addressPair.remote -> _)
  }

  reader.subscribe{s =>
    lorance.rxscoket.rxsocketLogger.log(s"read - ${s._1} - ${(s._2.uuid, s._2.length, new String(s._2.loaded.array))}")
  }

  Thread.currentThread().join()
}

object OutNetClient extends App{
  val client = new ClientEntrance("127.0.0.1", 12010)
//  val client = new ClientEntrance("192.168.0.102", 12001)
  val socket = client.connect

  Thread.currentThread().join()
}
