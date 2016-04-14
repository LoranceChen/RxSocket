import lorance.rxscoket.session.{ClientEntrance, ServerEntrance}

object OutNet extends App{
//  val server = new ServerEntrance("192.168.1.111", 12010)
  lorance.rxscoket.logLevel = 20
  val server = new ServerEntrance("127.0.0.1", 12010)
  val ls = server.listen
  ls.subscribe(s =>
    println(s"client connected - ${s.socketChannel.getRemoteAddress}")
  )

  val reader = ls.flatMap{s =>
    s.startReading.map(s.socketChannel -> _)
  }

  reader.subscribe{s =>
    lorance.rxscoket.log(s"read - ${s._1.getRemoteAddress} - ${s._2.map(m => (m.uuid, m.length, new String(m.loaded.array)))}")
  }

  Thread.currentThread().join()
}

object OutNetClient extends App{
  val client = new ClientEntrance("127.0.0.1", 12010)
//  val client = new ClientEntrance("192.168.0.102", 12001)
  val socket = client.connect

  Thread.currentThread().join()
}
