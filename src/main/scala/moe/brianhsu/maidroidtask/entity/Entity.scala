package moe.brianhsu.maidroidtask.entity

import java.util.UUID

trait Entity
trait EntityWithUserId extends Entity {
  def userUUID: UUID
}