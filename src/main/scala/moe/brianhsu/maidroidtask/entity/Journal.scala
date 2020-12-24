package moe.brianhsu.maidroidtask.entity

import java.time.LocalDateTime
import java.util.UUID

import moe.brianhsu.maidroidtask.usecase.base.UseCaseRequest

case class Journal(journalUUID: UUID,
                   userUUID: UUID,
                   request: UseCaseRequest,
                   changes: List[Change],
                   timestamp: LocalDateTime)
