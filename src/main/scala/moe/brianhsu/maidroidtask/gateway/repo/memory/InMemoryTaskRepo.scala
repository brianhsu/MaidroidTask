package moe.brianhsu.maidroidtask.gateway.repo.memory

import java.time.LocalDateTime
import java.util.UUID

import moe.brianhsu.maidroidtask.entity.Task
import moe.brianhsu.maidroidtask.gateway.repo.TaskRepo

class InMemoryTaskRepo(data: InMemoryData) extends TaskRepo {

  import data._

  override def read: TaskReadable = new InMemoryTaskRepoRead
  override def write: TaskWriteable = new InMemoryTaskRepoWrite

  class InMemoryTaskRepoRead extends TaskReadable {
    override def findByUUID(uuid: UUID): Option[Task] = uuidToTask.get(uuid)
  }

  class InMemoryTaskRepoWrite extends TaskWriteable {
    override def insert(entity: Task): Task = {
      uuidToTask += entity.uuid -> entity
      entity
    }

    override def update(uuid: UUID, entity: Task): Task = {
      uuidToTask += uuid -> entity
      entity
    }

    override def appendTag(uuid: UUID, tagUUID: UUID, updateTime: LocalDateTime): Task = {
      val updatedTask = uuidToTask.get(uuid).map { task =>
        task.copy(
          tags = (tagUUID :: task.tags).distinct,
          updateTime = updateTime
        )
      }.get
      update(uuid, updatedTask)
    }

    override def removeTag(uuid: UUID, tagUUID: UUID, updateTime: LocalDateTime): Task = {
      val updatedTask = uuidToTask.get(uuid).map { task =>
        task.copy(
          tags = task.tags.filterNot(_ == tagUUID),
          updateTime = updateTime
        )
      }.get
      update(uuid, updatedTask)
    }
  }

}
