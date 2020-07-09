package moe.brianhsu.maidroidtask.gateway.repo

import java.util.UUID

import moe.brianhsu.maidroidtask.entity.TrashableEntity

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

trait ParentChildReadable[T <: TrashableEntity] {
  def hasUnTrashedChildren(uuid: UUID): Boolean
}
