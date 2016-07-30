package lorance.rxscoket.session

import java.util.Comparator
import java.util.concurrent._

import scala.util.Try

import scala.concurrent.ExecutionContext.Implicits.global
import lorance.rxscoket.rxsocketLogger
//
///**
//  *
//  */
//class HeartBeatsManager {
//  object DataSet {
//    //it not thread safe, but its doesn't matter actually.
//    //every socket can't achieve others data and they shouldn't remove and put same item at one moment
//    //but every data collection should be thread safe
//    //todo how does database deal with multi index problem? it must thread safe
//    //this one seems better
//    private val tasks = new ConcurrentSkipListMap[TaskKey, Task](new Comparator[TaskKey]() {
//      override def compare(o1: TaskKey, o2: TaskKey): Int = {
//        val compare = (o1.systemTime - o2.systemTime).toInt
//        //return 1 will cause dead lock - if you have any idea call me please
//        if (compare == 0) {
//          o1.hashCode() - o2.hashCode()
//        } else compare //distinct same time task
//      }
//    })
//
//    //also need a map [String -> taskKey]
//    private val auxiliaryMap = new ConcurrentHashMap[String, TaskKey]()
//
//    def pollFirst() = {
//      val first = Option(tasks.pollFirstEntry())
//      first.map{x =>
//        val key = x.getKey
//        Try(auxiliaryMap.remove(key.id))
//      }
//      first
//    }
//
//    def put(task: Task): Unit = {
//      val x = auxiliaryMap.put(task.taskId.id, task.taskId)
//      rxsocketLogger.log(s"put to auxiliaryMap - ${auxiliaryMap.get(task.taskId.id)}", 300)
//
//      tasks.put(task.taskId, task)
//      rxsocketLogger.log(s"put to tasksMap - ${tasks.get(task.taskId)}", 300)
//    }
//
//    def get(taskId: String) = {
//      Option(taskId).flatMap{y => //catch null
//        Option(auxiliaryMap.get(y)).flatMap{x =>
//          Option(tasks.get(x)) //catch null by thread
//        }
//      }
//    }
//
//    def remove(taskId: String) = {
//      Option(taskId).flatMap{y => //catch null
//        Option(auxiliaryMap.remove(y)).flatMap{ x =>
//          rxsocketLogger.log(s"remove - $taskId - form auxiliaryMap - $y; auxiliary.size = ${auxiliaryMap.size()}")
//
//          Option{
//            val removed = tasks.remove(x)
//            rxsocketLogger.log(s"remove - $taskId - form tasksMap - $removed; tasks.size = ${tasks.size()}")
//
//            removed
//          }
//        }
//      }
//    }
//
//    def size = auxiliaryMap.size()
//  }
//
//  private val dispatch = new HeartBeats()
//
//  //notice the observer execute at Dispatch Thread if `afterExecute` not use `observeOn`
//  dispatch.afterExecute.subscribe((lastTask) => {
//    rxsocketLogger.log("get last task - " + lastTask)
//    //calculate next item, put it to task set
//    lastTask.nextTask.foreach{task => DataSet.put(task)}
//    rxsocketLogger.log("")
//    DataSet.pollFirst().foreach{task =>
//      rxsocketLogger.log("ready task - " + task, 170, Some("manager"))
//      addTask(task.getValue)
//    }
//  })
//
//  def tasksCount = DataSet.size
//
//  def findTask(id: String) = {
//    DataSet.get(id)
//  }
//
//  /**
//    *
//    * race condition at Some(underDispatchTask) if ...` if not use `addLock`:
//    * consider the condition multi thread `getCurrentTaskRef` and stuck at `match`.
//    * later a thread called `ready`, the older's enter `Some(underDispatchTask)` but it has invalid.
//    *
//    * fixed by forbid access `getCurrentTaskRef`
//    *
//    * PROBLEM:
//    * not thread safe - when concurrent with `cancelTask`
//    */
//  def addTask(task: Task): Unit = {
//    val result = dispatch.ready(task)
//
//    rxsocketLogger.log("addTask - " + task)
//    if(result._1) result._2.map{task => DataSet.put(task)} // success - put replaced task to set
//    else DataSet.put(task) // fail - add current task to set
//    rxsocketLogger.log(s"tasks contains - ${DataSet.size}; add task - $task", 600)
//  }
//
//  /**
//    *
//    */
//  def cancelTask(taskId: String) = {
//    rxsocketLogger.log(s"ready cancel task - $taskId")
//    DataSet.remove(taskId)
//    dispatch.cancelCurrentTaskIf((waitingTask) => {
//      waitingTask.taskId.id == taskId
//    })
//  }
//}


import java.util.Comparator
import java.util.concurrent._

import scala.concurrent.Promise

/**
  * todo remove the first task after it was ensure not nextTask
  * some principle:
  *   1. a task exist if it in DataSet
  *   2. remove a task from DataSet only it was executed or cancel by user.
  */
class HeartBeatsManager {
  import TaskCommandQueue._
  object TaskCommandQueue {
    trait Action
//    case class Update(key: TaskKey, newTask: Option[Task]) extends Action
    case class Cancel(id: String, promise: Promise[Option[Task]]) extends Action
    case class Get(id: String, promise: Promise[Option[Task]]) extends Action
//    case class GetByKey(taskKey: TaskKey, promise: Promise[Option[Task]]) extends Action
    case class AddTask(task: Task) extends Action
//    case class GetFirst(promise: Promise[Option[Task]]) extends Action
    case class GetCount(promise: Promise[Int]) extends Action
    case class NextTask(lastTask: Task) extends Action
  }

  class TaskCommandQueue extends CommandQueue[TaskCommandQueue.Action]{
    import TaskCommandQueue._
    private def addTaskSync(task: Task) = {
      DataSet.put(task)
      dispatch.ready(task)
      rxsocketLogger.log("addTask - " + task)
      //    if(result._1) result._2.foreach{tsk => DataSet.put(tsk)} // success - put replaced task to set
      rxsocketLogger.log(s"tasks contains - ${DataSet.size}; add task - $task", 600)
    }
    protected override def receive(action: Action): Unit = action match {
      //      case Remove(id: String) => ???
      //      case RemoveByKey(key: TaskKey) => ???
      //      case Update(key: TaskKey, newTask: TaskKey) => ???
      case Cancel(id: String, promise: Promise[Option[Task]]) =>
        rxsocketLogger.log(s"ready cancel task - $id")
        val tryGetTask = List(dispatch.cancelCurrentTaskIf((waitingTask) => {
          waitingTask.taskId.id == id
        })._2,
          DataSet.remove(id)).flatten

        promise.trySuccess(tryGetTask.headOption)
      case AddTask(task: Task) =>
        addTaskSync(task)
      case Get(id: String, promise: Promise[Option[Task]]) =>
        promise.trySuccess(DataSet.get(id))
//      case GetByKey(key: TaskKey, promise: Promise[Option[Task]]) =>
//        promise.trySuccess(DataSet.get(key))
//      case Update(key, newTask: Option[Task]) =>
//        DataSet.update(key, newTask)
//      case GetFirst(promise: Promise[Option[Task]]) =>
//        promise.trySuccess(DataSet.getFirst)
      case GetCount(promise: Promise[Int]) =>
        val x = DataSet.size
        rxsocketLogger.log(s"GetCount - $x")
        promise.trySuccess(x)
      case NextTask(lastTask) =>
        DataSet.get(lastTask.taskId) match {
          case None => //has removed, DON'T calculate nextTask even though the Task has next task
            rxsocketLogger.log("has removed and needn't get `nextTask`- " + lastTask)
          case Some(_) =>
            rxsocketLogger.log("get last task - " + lastTask)
            DataSet.update(lastTask.taskId, lastTask.nextTask)
          }

    //calculate next item, put it to task set
    //    lastTask.nextTask.foreach{task => DataSet.put(task)}

        DataSet.getFirst.foreach { task =>
          rxsocketLogger.log("ready task - " + task, 170, Some("manager"))
//          addTask(task) SHOULDN'T use tell
          addTaskSync(task)
        }

        rxsocketLogger.log("get task count - " + DataSet.size)
    }


    private object DataSet {
      private val tasks = new ConcurrentSkipListMap[TaskKey, Task](new Comparator[TaskKey]() {
        override def compare(o1: TaskKey, o2: TaskKey): Int = {
          val compare = (o1.systemTime - o2.systemTime).toInt
          //return 1 will cause dead lock, we should always promise compare result is great or little
          if (compare == 0) {
            val comp = o1.hashCode() - o2.hashCode()
            //          assert(comp != 0) //can't be 0
            comp
          } else compare //distinct same time task
        }
      })

      private val auxiliaryMap = new ConcurrentHashMap[String, TaskKey]()

      def pollFirst = {
        val first = Option(tasks.pollFirstEntry())
        first.foreach(y => auxiliaryMap.remove(y.getKey.id))
        first
      }

      def getFirst = {
        Option(tasks.firstEntry()).map(_.getValue)
      }

      def put(task: Task): Unit = {
        auxiliaryMap.put(task.taskId.id, task.taskId)
        tasks.put(task.taskId, task)
        rxsocketLogger.log(s"put to tasksMap - ${tasks.get(task.taskId)}", 300)
      }

      def get(taskKey: TaskKey) = {
        Option(tasks.get(taskKey))
      }

      def get(taskId: String) = {
        Option(auxiliaryMap.get(taskId)).flatMap(key =>
          Option(tasks.get(key))
        )
      }

      def remove(taskKey: TaskKey) = {
        Option{
          val removed = tasks.remove(taskKey)
          rxsocketLogger.log(s"remove - $taskKey - form tasksMap - $removed; tasks.size = ${tasks.size()}")
          removed
        }
      }

      def remove(taskId: String) = {
        Option(auxiliaryMap.get(taskId)).flatMap(taskKey =>
          Option{
            auxiliaryMap.remove(taskId)
            val removed = tasks.remove(taskKey)
            rxsocketLogger.log(s"remove - $taskKey - form tasksMap - $removed; tasks.size = ${tasks.size()}")
            removed
          }
        )
      }

      def update(older: TaskKey, newTask: Option[Task]) = {
        assert(newTask.fold(true)(_.taskId.id == older.id))
        remove(older)
        newTask.foreach{task => DataSet.put(task)}
      }

      def size = tasks.size()
    }
  }

  private val dataSetOperateQueue = new TaskCommandQueue()

  private val dispatch = new HeartBeats()

  //notice the observer execute at Dispatch Thread if `afterExecute` not use `observeOn`
  dispatch.afterExecute.subscribe { (lastTask) => {
    dataSetOperateQueue.tell(NextTask(lastTask))
//    val promise = Promise[Option[Task]]()
//    dataSetOperateQueue.tell(GetByKey(lastTask.taskId, promise))
//    promise.future.map { getRst =>
//      getRst match {
//        case None => //has removed, DON'T calculate nextTask even though the Task has next task
//          rxsocketLogger.log("has removed and needn't get `nextTask`- " + lastTask)
//        case Some(_) =>
//          rxsocketLogger.log("get last task - " + lastTask)
//          dataSetOperateQueue.tell(Update(lastTask.taskId, lastTask.nextTask))
//      }
//      val getFirstPromise = Promise[Option[Task]]()
//      dataSetOperateQueue.tell(GetFirst(getFirstPromise))
//      getFirstPromise.future.foreach { tkpt =>
//        tkpt.foreach { tk =>
//          rxsocketLogger.log("ready task - " + tk, 170, Some("manager"))
//          dataSetOperateQueue.tell(AddTask(tk))
//        }
//      }
//    }

    //    DataSet.get(lastTask.taskId) match {
    //      case None => //has removed, DON'T calculate nextTask even though the Task has next task
    //        rxsocketLogger.log("has removed and needn't get `nextTask`- " + lastTask)
    //      case Some(_) =>
    //        rxsocketLogger.log("get last task - " + lastTask)
    //        DataSet.update(lastTask.taskId, lastTask.nextTask)
    //      //        DataSet.lock.synchronized{
    //      //          DataSet.remove(lastTask.taskId) //remove the completed task SHOULDN'T use `pullFirst`
    //      //          lastTask.nextTask.foreach{task => DataSet.put(task)} //try add next task
    //      }
    //    }

    ////calculate next item, put it to task set
    ////    lastTask.nextTask.foreach{task => DataSet.put(task)}

    //    DataSet.getFirst.foreach { task =>
    //      rxsocketLogger.log("ready task - " + task, 170, Some("manager"))
    //      addTask(task.getValue)
    //    }
    }
  }

  def tasksCount = {
    val promise = Promise[Int]()
    dataSetOperateQueue.tell(GetCount(promise))
    promise.future
  } //DataSet.size

  def findTask(id: String) = {
    val promise= Promise[Option[Task]]()
    dataSetOperateQueue.tell(Get(id, promise))
    promise.future
  }

  /**
    * 1. add to DataSet
    * 2. try add to TaskHolder
    */
  def addTask(task: Task): Unit = {
    dataSetOperateQueue.tell(AddTask(task))
  }

  def cancelTask(id: String) = {
    val promise= Promise[Option[Task]]()
    dataSetOperateQueue.tell(Cancel(id, promise))
    promise.future
  }
}

import java.util.concurrent.ConcurrentLinkedQueue

trait CommandQueue[T] {
  val myQueue = new ConcurrentLinkedQueue[T]()
  val lock = new Object()
  object QueueThread extends Thread {
    setDaemon(true)

    override def run = {
      while(true) {
        if (myQueue.size() == 0) {
          lock.synchronized(lock.wait())
        } else {
          val theTask = myQueue.poll()
          rxsocketLogger.log(s"poll task cmd queue - $theTask")

          receive(theTask)
        }
      }
    }
  }

  QueueThread.start()

  def tell(cmd: T) = {
    myQueue.add(cmd)
    rxsocketLogger.log(s"tell cmd - $cmd - current count - ${myQueue.size()}")
    lock.synchronized(lock.notify())
  }

  //must sync operation
  protected def receive(t: T): Unit
}
