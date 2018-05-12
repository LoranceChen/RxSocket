package demo.tool


import java.lang.management.ManagementFactory


object Tool {
  def createGcThread(gcTime: Long): Unit = {
    object GcThread extends Thread {
      override def run(): Unit = {
        while(true) {
          Thread.sleep(gcTime)
          println("do gc")
          System.gc()

        }
      }
    }
    GcThread.start()
  }

  def showPid: Unit = {
    val runtime = ManagementFactory.getRuntimeMXBean()
    val name = runtime.getName()
    System.out.println("当前进程的标识为："+name)
    val index = name.indexOf("@")
    if (index != -1) {
      val pid = Integer.parseInt(name.substring(0, index))
      System.out.println("当前进程的PID为："+pid)
    }
  }
}

