package moe.brianhsu.maidroidtask.gateway.repo

import java.util.UUID

trait UserBasedReadable[T] {
  def listByUserUUID(userUUID: UUID): List[T]
}

trait Writable[T] {
  def update(uuid: UUID, entity: T): T
  def insert(entity: T): T
}

trait Readable[T] {
  def findByUUID(uuid: UUID): Option[T]
}

trait ParentChildReadable[T] {
  def hasChildren(uuid: UUID): Boolean
}
