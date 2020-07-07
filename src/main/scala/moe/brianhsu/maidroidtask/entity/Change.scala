package moe.brianhsu.maidroidtask.entity

import java.time.LocalDateTime
import java.util.UUID

import moe.brianhsu.maidroidtask.usecase.UseCaseRequest

case class GroupedJournal(journalUUID: UUID, userUUID: UUID, request: UseCaseRequest, changes: List[Change], timestamp: LocalDateTime)
case class Change(changeUUID: UUID, previous: Option[Entity], current: Entity, timestamp: LocalDateTime)
