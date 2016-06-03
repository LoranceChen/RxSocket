package lorance.rxscoket.session

import java.util.Comparator
import java.util.concurrent._

import scala.util.Try
import lorance.rxscoket.rxsocketLogger

/**
  *
  */
class HeartBeatsManager {
  object DataSet {
    //it not thread safe, but its doesn't matter actually.
    //every socket can't achieve others data and they shouldn't remove and put same item at one moment
    //but every data collection should be thread safe
    //todo how does database deal with multi index problem? it must thread safe
    //this one seems better
    private val tasks = new ConcurrentSkipListMap[TaskKey, Task](new Comparator[TaskKey]() {
      override def compare(o1: TaskKey, o2: TaskKey): Int = {
        val compare = (o1.systemTime - o2.systemTime).toInt
        //return 1 will cause dead lock - if you have any idea call me please
        if (compare == 0) {
          o1.hashCode() - o2.hashCode()
        } else compare //distinct same time task
      }
    })

    //also need a map [String -> taskKey]
    private val auxiliaryMap = new ConcurrentHashMap[String, TaskKey]()

    def pollFirst() = {
      val first = Option(tasks.pollFirstEntry())
      first.map{x =>
        val key = x.getKey
        Try(auxiliaryMap.remove(key.id))
      }
      first
    }

    def put(task: Task): Unit = {
      val x = auxiliaryMap.put(task.taskId.id, task.taskId)
      rxsocketLogger.log(s"put to auxiliaryMap - ${auxiliaryMap.get(task.taskId.id)}", 300)

      tasks.put(task.taskId, task)
      rxsocketLogger.log(s"put to tasksMap - ${tasks.get(task.taskId)}", 300)
    }

    def get(taskId: String) = {
      Option(taskId).flatMap{y => //catch null
        Option(auxiliaryMap.get(y)).flatMap{x =>
          Option(tasks.get(x)) //catch null by thread
        }
      }
    }

    def remove(taskId: String) = {
      Option(taskId).flatMap{y => //catch null
        Option(auxiliaryMap.remove(y)).flatMap{ x =>
          rxsocketLogger.log(s"remove - $taskId - form auxiliaryMap - $y; auxiliary.size = ${auxiliaryMap.size()}")

          Option{
            val removed = tasks.remove(x)
            rxsocketLogger.log(s"remove - $taskId - form tasksMap - $removed; tasks.size = ${tasks.size()}")

            removed
          }
        }
      }
    }

    def size = auxiliaryMap.size()
  }

  private val dispatch = new HeartBeats()

  //notice the observer execute at Dispatch Thread if `afterExecute` not use `observeOn`
  dispatch.afterExecute.subscribe((lastTask) => {
    rxsocketLogger.log("get last task - " + lastTask)
    //calculate next item, put it to task set
    lastTask.nextTask.foreach{task => DataSet.put(task)}
    rxsocketLogger.log("")
    DataSet.pollFirst().foreach{task =>
      rxsocketLogger.log("ready task - " + task, 170, Some("manager"))
      addTask(task.getValue)
    }
  })

  def tasksCount = DataSet.size

  def findTask(id: String) = {
    DataSet.get(id)
  }

  /**
    *
    * race condition at Some(underDispatchTask) if ...` if not use `addLock`:
    * consider the condition multi thread `getCurrentTaskRef` and stuck at `match`.
    * later a thread called `ready`, the older's enter `Some(underDispatchTask)` but it has invalid.
    *
    * fixed by forbid access `getCurrentTaskRef`
    *
    * PROBLEM:
    * not thread safe - when concurrent with `cancelTask`
    */
  def addTask(task: Task): Unit = {
    val result = dispatch.ready(task)

    rxsocketLogger.log("addTask - " + task)
    if(result._1) result._2.map{task => DataSet.put(task)} // success - put replaced task to set
    else DataSet.put(task) // fail - add current task to set
    rxsocketLogger.log(s"tasks contains - ${DataSet.size}; add task - $task", -1)
  }

  /**
    *
    */
  def cancelTask(taskId: String) = {
    rxsocketLogger.log(s"ready cancel task - $taskId")
    DataSet.remove(taskId)
    dispatch.cancelCurrentTaskIf((waitingTask) => {
      waitingTask.taskId.id == taskId
    })
  }
}
