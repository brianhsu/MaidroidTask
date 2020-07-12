package moe.brianhsu.maidroidtask.entity

import java.time.LocalDateTime
import java.util.UUID

import moe.brianhsu.maidroidtask.gateway.repo.{Readable, TaskReadable, TaskRepo}

case class Task(uuid: UUID, userUUID: UUID,
                description: String,
                note: Option[String] = None,
                project: Option[UUID] = None,
                tags: List[UUID] = Nil,
                dependsOn: List[UUID] = Nil,
                waitUntil: Option[LocalDateTime] = None,
                due: Option[LocalDateTime] = None,
                scheduledAt: Option[ScheduledAt] = None,
                isDone: Boolean = false,
                isTrashed: Boolean = false,
                createTime: LocalDateTime,
                updateTime: LocalDateTime) extends EntityWithUserId with TrashableEntity {

  def blocking(implicit taskRead: TaskReadable): List[Task] = {
    taskRead.findByDependsOn(this.uuid)
      .filterNot(t => t.isDone || t.isTrashed)
  }

  def hasLoopsWith(thatUUID: UUID)(implicit taskRead: Readable[Task]): Boolean = {
    val thatTask = taskRead.findByUUID(thatUUID)
    val isDependsOnEachOther = thatTask.exists(_.dependsOn contains this.uuid)

    def hasLoopInParent(task: Task, thatUUID: UUID): Boolean = {
      if (task.dependsOn.contains(thatUUID)) {
        true
      } else {
        val parentTaskList: List[Task] = task.dependsOn.flatMap(taskRead.findByUUID)
        parentTaskList.exists(t => hasLoopInParent(t, thatUUID))
      }
    }

    isDependsOnEachOther || hasLoopInParent(task = this, thatUUID)
  }

}
