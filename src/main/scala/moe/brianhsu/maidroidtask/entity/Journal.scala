package moe.brianhsu.maidroidtask.entity

import java.time.LocalDateTime
import java.util.UUID

sealed trait Journal

case class InsertLog(userUUID: UUID, uuid: UUID, entry: Entity, timestamp: LocalDateTime) extends Journal
case class UpdateLog(userUUID: UUID, uuid: UUID, entry: Entity, timestamp: LocalDateTime) extends Journal
case class DeleteLog(userUUID: UUID, uuid: UUID, entry: Entity, timestamp: LocalDateTime) extends Journal
