package moe.brianhsu.maidroidtask.entity

import java.time.LocalDateTime
import java.util.UUID

case class Task(uuid: UUID, userUUID: UUID,
                description: String,
                note: Option[String] = None,
                project: Option[UUID] = None,
                tags: List[UUID] = Nil,
                dependsOn: List[UUID] = Nil,
                priority: Option[Priority] = None,
                waitUntil: Option[LocalDateTime] = None,
                due: Option[LocalDateTime] = None,
                scheduledAt: Option[ScheduledAt] = None,
                isDone: Boolean = false,
                isTrashed: Boolean = false,
                createTime: LocalDateTime,
                updateTime: LocalDateTime) extends EntityWithUserId
