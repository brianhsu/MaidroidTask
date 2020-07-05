package moe.brianhsu.maidroidtask.gateway.repo

import java.util.UUID

trait UserBasedReadable[T] {
  def findByUUID(uuid: UUID): Option[T]
}

trait ParentChildReadble[T] {
  def hasChildren(uuid: UUID): Boolean
}
