package moe.brianhsu.maidroidtask.gateway.repo

import java.util.UUID

import moe.brianhsu.maidroidtask.entity.Task

trait ReadableRepo[T] {
  def findByUUID(uuid: UUID): Option[T]
}

trait TaskRepo {

  def read: TaskReadable
  def write: TaskWriteable

  trait TaskReadable extends ReadableRepo[Task] {
    def findByUUID(uuid: UUID): Option[Task]
  }

  trait TaskWriteable {
    def update(uuid: UUID, updatedTask: Task): Task
    def insert(task: Task): Task
  }

}
