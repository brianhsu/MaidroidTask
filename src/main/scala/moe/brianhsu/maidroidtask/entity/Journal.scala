package moe.brianhsu.maidroidtask.entity

import java.time.LocalDateTime
import java.util.UUID

import moe.brianhsu.maidroidtask.usecase.UseCaseRequest

case class Journal(historyUUID: UUID, userUUID: UUID, request: UseCaseRequest, previous: Option[Entity], current: Entity, timestamp: LocalDateTime)
