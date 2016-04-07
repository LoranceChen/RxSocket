import lorance.rxscoket.session.{ClientEntrance, ServerEntrance}

object OutNet extends App{
  val server = new ServerEntrance("192.168.1.111", 12010)
  server.listen.subscribe(s =>
    println(s"client connected - ${s.socketChannel.getRemoteAddress}")
  )

  Thread.currentThread().join()
}

object OutNetClient extends App{
  val client = new ClientEntrance("1.1.1.1", 12010)
//  val client = new ClientEntrance("192.168.0.102", 12001)
  val socket = client.connect

  Thread.currentThread().join()
}
