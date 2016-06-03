import java.util.concurrent.Executors

import rx.lang.scala.{Subject, Subscriber, Observable}
import rx.lang.scala.ImplicitFunctionConversions._
import rx.lang.scala.schedulers.{IOScheduler, TestScheduler, NewThreadScheduler}
import rx.lang.scala.subjects.PublishSubject
import rx.schedulers.Schedulers
import lorance.rxscoket._
/**
  *
  */
object MultiThreadTask extends App {
  val t = new Thread{
    rxsocketLogger.log("")
    override def run(): Unit = test

    start()
  }

  /*
    NO compose
    RxNewThreadScheduler-1: first mapped observable - 10
RxNewThreadScheduler-1: second mapped observable - 100
RxNewThreadScheduler-1: observer - 100
RxNewThreadScheduler-1: first mapped observable - 20
RxNewThreadScheduler-1: second mapped observable - 200
RxNewThreadScheduler-1: observer - 200
RxNewThreadScheduler-1: first mapped observable - 30
RxNewThreadScheduler-1: second mapped observable - 300
RxNewThreadScheduler-1: observer - 300
RxNewThreadScheduler-1: first mapped observable - 40
RxNewThreadScheduler-1: second mapped observable - 400
RxNewThreadScheduler-1: observer - 400
RxNewThreadScheduler-3: first mapped observable - 10
RxNewThreadScheduler-3: second mapped observable - 100
RxNewThreadScheduler-3: observer2 - 100
RxNewThreadScheduler-3: first mapped observable - 20
RxNewThreadScheduler-3: second mapped observable - 200
RxNewThreadScheduler-3: observer2 - 200
RxNewThreadScheduler-3: first mapped observable - 30
RxNewThreadScheduler-3: second mapped observable - 300
RxNewThreadScheduler-3: observer2 - 300
RxNewThreadScheduler-3: first mapped observable - 40
RxNewThreadScheduler-3: second mapped observable - 400
RxNewThreadScheduler-3: observer2 - 400
    HAS compose
    /Library/Java/JavaVirtualMachines/jdk1.8.0_65.jdk/Contents/Home/bin/java -agentlib:jdwp=transport=dt_socket,address=127.0.0.1:51673,suspend=y,server=n -Dfile.encoding=UTF-8 -classpath "/Library/Java/JavaVirtualMachines/jdk1.8.0_65.jdk/Contents/Home/lib/ant-javafx.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_65.jdk/Contents/Home/lib/dt.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_65.jdk/Contents/Home/lib/javafx-mx.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_65.jdk/Contents/Home/lib/jconsole.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_65.jdk/Contents/Home/lib/packager.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_65.jdk/Contents/Home/lib/sa-jdi.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_65.jdk/Contents/Home/lib/tools.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_65.jdk/Contents/Home/jre/lib/charsets.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_65.jdk/Contents/Home/jre/lib/deploy.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_65.jdk/Contents/Home/jre/lib/javaws.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_65.jdk/Contents/Home/jre/lib/jce.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_65.jdk/Contents/Home/jre/lib/jfr.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_65.jdk/Contents/Home/jre/lib/jfxswt.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_65.jdk/Contents/Home/jre/lib/jsse.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_65.jdk/Contents/Home/jre/lib/management-agent.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_65.jdk/Contents/Home/jre/lib/plugin.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_65.jdk/Contents/Home/jre/lib/resources.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_65.jdk/Contents/Home/jre/lib/rt.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_65.jdk/Contents/Home/jre/lib/ext/cldrdata.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_65.jdk/Contents/Home/jre/lib/ext/dnsns.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_65.jdk/Contents/Home/jre/lib/ext/jaccess.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_65.jdk/Contents/Home/jre/lib/ext/jfxrt.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_65.jdk/Contents/Home/jre/lib/ext/localedata.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_65.jdk/Contents/Home/jre/lib/ext/nashorn.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_65.jdk/Contents/Home/jre/lib/ext/sunec.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_65.jdk/Contents/Home/jre/lib/ext/sunjce_provider.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_65.jdk/Contents/Home/jre/lib/ext/sunpkcs11.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_65.jdk/Contents/Home/jre/lib/ext/zipfs.jar:/Users/lorancechen/version_control_project/_personal/RxSocket/target/scala-2.11/test-classes:/Users/lorancechen/version_control_project/_personal/RxSocket/target/scala-2.11/classes:/Users/lorancechen/.ivy2/cache/org.scala-lang/scala-library/jars/scala-library-2.11.7.jar:/Users/lorancechen/.ivy2/cache/com.netflix.rxjava/rxjava-core/jars/rxjava-core-0.20.7.jar:/Users/lorancechen/.ivy2/cache/com.netflix.rxjava/rxjava-scala/jars/rxjava-scala-0.20.7.jar:/Users/lorancechen/.ivy2/cache/com.thoughtworks.paranamer/paranamer/jars/paranamer-2.4.1.jar:/Users/lorancechen/.ivy2/cache/net.liftweb/lift-json_2.11/jars/lift-json_2.11-3.0-M8.jar:/Users/lorancechen/.ivy2/cache/org.scala-lang/scala-compiler/jars/scala-compiler-2.11.4.jar:/Users/lorancechen/.ivy2/cache/org.scala-lang/scala-reflect/jars/scala-reflect-2.11.4.jar:/Users/lorancechen/.ivy2/cache/org.scala-lang/scalap/jars/scalap-2.11.4.jar:/Users/lorancechen/.ivy2/cache/org.scala-lang.modules/scala-parser-combinators_2.11/bundles/scala-parser-combinators_2.11-1.0.2.jar:/Users/lorancechen/.ivy2/cache/org.scala-lang.modules/scala-xml_2.11/bundles/scala-xml_2.11-1.0.2.jar:/Applications/IntelliJ IDEA 15 CE.app/Contents/lib/idea_rt.jar" MultiThreadTask
Connected to the target VM, address: '127.0.0.1:51673', transport: 'socket'
main:
RxNewThreadScheduler-1: first mapped observable - 10
RxNewThreadScheduler-1: second mapped observable - 100
RxNewThreadScheduler-1: observer - 100
RxNewThreadScheduler-1: first mapped observable - 20
RxNewThreadScheduler-1: second mapped observable - 200
RxNewThreadScheduler-1: observer - 200
RxNewThreadScheduler-1: first mapped observable - 30
RxNewThreadScheduler-1: second mapped observable - 300
RxNewThreadScheduler-1: observer - 300
RxNewThreadScheduler-1: first mapped observable - 40
RxNewThreadScheduler-1: second mapped observable - 400
RxNewThreadScheduler-1: observer - 400
RxNewThreadScheduler-3: first mapped observable - 10
RxNewThreadScheduler-3: second mapped observable - 100
RxNewThreadScheduler-3: observer2 - 100
RxNewThreadScheduler-3: first mapped observable - 20
RxNewThreadScheduler-3: second mapped observable - 200
RxNewThreadScheduler-3: observer2 - 200
RxNewThreadScheduler-3: first mapped observable - 30
RxNewThreadScheduler-3: second mapped observable - 300
RxNewThreadScheduler-3: observer2 - 300
RxNewThreadScheduler-3: first mapped observable - 40
RxNewThreadScheduler-3: second mapped observable - 400
RxNewThreadScheduler-3: observer2 - 400


    * @return
    */
  def test = {
    /**
      * NewThreadScheduler make every event(contains Observable by subscribeOn and Observer by observerOn) execute at new thread
      */
//    val x = Executors.newSingleThreadExecutor()
    val customT = Executors.newSingleThreadExecutor()
    val sch = Schedulers.from(customT)
    val obv = Observable.just(1, 2, 3, 4).
      subscribeOn(NewThreadScheduler()).
//      observeOn(sch).
//      compose{ x =>
//        x.subscribeOn(NewThreadScheduler()).observeOn(NewThreadScheduler())}.
      map { data => rxsocketLogger.log("first mapped observable - " + (data * 10).toString); data * 10 }.
      map { data => rxsocketLogger.log("second mapped observable - " + (data * 10).toString); data * 10 }//.
//      compose{x => x.doOnNext(n =>log("compose " + n.toString))}.

    obv.subscribe(data => rxsocketLogger.log("observer - " + data.toString))
    obv.subscribe(data => rxsocketLogger.log("observer2 - " + data.toString))
  }

  /**
    * result:
    * 1. map will make parent logic rexecute within itself body also execute at its Thread
    * 2.
    *
    * @return
    *         NO compose
    *         RxNewThreadScheduler-1: first mapped observable - 10
RxNewThreadScheduler-1: observer once - 10
RxNewThreadScheduler-1: first mapped observable - 20
RxNewThreadScheduler-1: observer once - 20
RxNewThreadScheduler-1: first mapped observable - 30
RxNewThreadScheduler-1: observer once - 30
RxNewThreadScheduler-1: first mapped observable - 40
RxNewThreadScheduler-1: observer once - 40
RxNewThreadScheduler-3: first mapped observable - 10
RxNewThreadScheduler-3: second mapped observable - 100
RxNewThreadScheduler-3: observer twice - 100
RxNewThreadScheduler-3: first mapped observable - 20
RxNewThreadScheduler-3: second mapped observable - 200
RxNewThreadScheduler-3: observer twice - 200
RxNewThreadScheduler-3: first mapped observable - 30
RxNewThreadScheduler-3: second mapped observable - 300
RxNewThreadScheduler-3: observer twice - 300
RxNewThreadScheduler-3: first mapped observable - 40
RxNewThreadScheduler-3: second mapped observable - 400
RxNewThreadScheduler-3: observer twice - 400

    HAS compose
    */
  def test2 = {
    /**
      * NewThreadScheduler make every event(contains Observable by subscribeOn and Observer by observerOn) execute at new thread
      */
    val obv = Observable.just(1, 2, 3, 4)//.
//      subscribeOn(NewThreadScheduler()).
//      observeOn(NewThreadScheduler())
//      combine{ x =>
//        x.subscribeOn(NewThreadScheduler()).observeOn(NewThreadScheduler())}
    val obvMapOnce =  obv.map { data => rxsocketLogger.log("first mapped observable - " + (data * 10).toString); data * 10 }
    val obvMapTwice = obvMapOnce.map { data => rxsocketLogger.log("second mapped observable - " + (data * 10).toString); data * 10 }//.
    //      compose{x => x.doOnNext(n =>log("compose " + n.toString))}.

    obvMapOnce.subscribe(data => rxsocketLogger.log("observer once - " + data.toString))
    obvMapTwice.subscribe(data => rxsocketLogger.log("observer twice - " + data.toString))
  }

  def test3 = {
    /**
      * NewThreadScheduler make every event(contains Observable by subscribeOn and Observer by observerOn) execute at new thread
      */
    val obv = Observable.just(1, 2, 3, 4).
            subscribeOn(NewThreadScheduler()).
            observeOn(NewThreadScheduler())
//      compose{ x => //???? not have compose
//      x.subscribeOn(NewThreadScheduler()).observeOn(NewThreadScheduler())}
    val obvMapOnce =  obv.map { data => rxsocketLogger.log("first mapped observable - " + (data * 10).toString); data * 10 }
    val obvMapTwice = obvMapOnce.map { data => rxsocketLogger.log("second mapped observable - " + (data * 10).toString); data * 10 }//.
    //      compose{x => x.doOnNext(n =>log("compose " + n.toString))}.

    obvMapOnce.subscribe(data => rxsocketLogger.log("MapOnce observer once - " + data.toString))
    obvMapOnce.subscribe(data => rxsocketLogger.log("MapOnce observer once - " + data.toString))
    obvMapTwice.subscribe(data => rxsocketLogger.log("MapTwice observer twice - " + data.toString))
    obvMapTwice.subscribe(data => rxsocketLogger.log("MapTwice observer twice - " + data.toString))
  }


  Thread.currentThread().join()
}

