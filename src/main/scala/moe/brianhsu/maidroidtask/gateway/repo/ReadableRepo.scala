package moe.brianhsu.maidroidtask.gateway.repo

import java.util.UUID

trait ReadableRepo[T] {
  def findByUUID(uuid: UUID): Option[T]
}
