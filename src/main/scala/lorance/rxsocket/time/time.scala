package lorance.rxsocket

import java.util.concurrent.{Executors, TimeUnit}

import akka.actor.LightArrayRevolverScheduler
import com.typesafe.config.ConfigFactory


package object time {

  /**
    * For a server socket timeout usage, mainly for:
    * 1. socket keep alive: 5s - 1min
    * 2. a timeout after a read/write action: 3s - 30s
    * timeout manager should hold under 1min is fine.
    * 100ms * 1000 = 100s
    */
//  private val nettyTimeWheel = new HashedWheelTimer(
//                                  Executors.defaultThreadFactory,
//                                  100L,
//                                  TimeUnit.MILLISECONDS,
//                                  1024
//                                )
//  new LightArrayRevolverScheduler(ConfigFactory.load())

}
