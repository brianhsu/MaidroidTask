package moe.brianhsu.maidroidtask.entity

import java.time.LocalDateTime
import java.util.UUID

case class Tag(uuid: UUID, userUUID: UUID,
               name: String,
               parentTagUUID: Option[UUID],
               createTime: LocalDateTime,
               updateTime: LocalDateTime) extends EntityWithUserId
