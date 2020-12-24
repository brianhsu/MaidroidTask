package moe.brianhsu.maidroidtask.gateway.repo

import java.time.LocalDateTime
import java.util.UUID

import moe.brianhsu.maidroidtask.entity.Task

trait TaskRepo {
  val read: TaskReadable
  val write: TaskWriteable
}

trait TaskReadable extends Readable[Task] {
  def findByTag(uuid: UUID): List[Task]
  def findByProject(uuid: UUID): List[Task]
  def findByDependsOn(uuid: UUID): List[Task]
}

trait TaskWriteable extends Writable[Task] {
  def appendTag(uuid: UUID, tagUUID: UUID, updateTime: LocalDateTime): Task
  def removeTag(uuid: UUID, tagUUID: UUID, updateTime: LocalDateTime): Task
}
