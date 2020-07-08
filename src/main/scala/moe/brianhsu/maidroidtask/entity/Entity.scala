package moe.brianhsu.maidroidtask.entity

import java.util.UUID

trait TrashableEntity {
  def isTrashed: Boolean
}
trait Entity {
  def uuid: UUID
}
trait EntityWithUserId extends Entity {
  def userUUID: UUID
}
trait NamedEntity extends Entity {
  def name: String
  def isTrashed: Boolean
}
