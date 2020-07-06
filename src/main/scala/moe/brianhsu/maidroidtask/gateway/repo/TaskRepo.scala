package moe.brianhsu.maidroidtask.gateway.repo

import java.time.{LocalDate, LocalDateTime}
import java.util.UUID

import moe.brianhsu.maidroidtask.entity.Task

trait TaskRepo {

  def read: TaskReadable
  def write: TaskWriteable

  trait TaskReadable extends Readable[Task]
  trait TaskWriteable extends Writable[Task] {
    def appendTag(uuid: UUID, tagUUID: UUID, updateTime: LocalDateTime): Task
    def removeTag(uuid: UUID, tagUUID: UUID, updateTime: LocalDateTime): Task

  }
}
