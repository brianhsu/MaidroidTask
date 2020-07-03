package moe.brianhsu.maidroidtask.gateway.repo.memory

import java.util.UUID

import moe.brianhsu.maidroidtask.entity.Task
import moe.brianhsu.maidroidtask.gateway.repo.TaskRepo

class InMemoryTaskRepo extends TaskRepo {

  private var uuidToTask: Map[UUID, Task] = Map.empty
  override def read: TaskReadable = new InMemoryTaskRepoRead
  override def write: TaskWriteable = new InMemoryTaskRepoWrite

  class InMemoryTaskRepoRead extends TaskReadable {
    override def findByUUID(uuid: UUID): Option[Task] = uuidToTask.get(uuid)
  }

  class InMemoryTaskRepoWrite extends TaskWriteable {
    override def insert(task: Task): Task = {
      uuidToTask += task.uuid -> task
      task
    }
  }

}
