package moe.brianhsu.maidroidtask.entity

import java.time.LocalDateTime
import java.util.UUID

sealed trait Journal

case class InsertLog(historyUUID: UUID, userUUID: UUID, uuid: UUID, entry: Entity, timestamp: LocalDateTime) extends Journal
case class UpdateLog(historyUUID: UUID, userUUID: UUID, uuid: UUID, entry: Entity, timestamp: LocalDateTime) extends Journal
case class DeleteLog(historyUUID: UUID, userUUID: UUID, uuid: UUID, entry: Entity, timestamp: LocalDateTime) extends Journal
