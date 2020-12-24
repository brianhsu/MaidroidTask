package moe.brianhsu.maidroidtask.entity

import java.time.LocalDateTime
import java.util.UUID

case class Change(changeUUID: UUID,
                  previous: Option[Entity],
                  current: Entity,
                  timestamp: LocalDateTime)
