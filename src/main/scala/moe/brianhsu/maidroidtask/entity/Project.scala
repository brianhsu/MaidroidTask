package moe.brianhsu.maidroidtask.entity

import java.time.LocalDateTime
import java.util.UUID

object Project {
  sealed trait Status
  case object Active extends Status
  case object Done extends Status
  case object Inactive extends Status
}

case class Project(uuid: UUID, userUUID: UUID,
                   name: String,
                   note: Option[String],
                   parentProjectUUID: Option[UUID],
                   status: Project.Status,
                   isTrashed: Boolean,
                   createTime: LocalDateTime,
                   updateTime: LocalDateTime) extends EntityWithUserId with NamedEntity with TrashableEntity
