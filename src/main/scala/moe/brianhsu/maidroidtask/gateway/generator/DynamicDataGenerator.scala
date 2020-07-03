package moe.brianhsu.maidroidtask.gateway.generator

import java.time.LocalDateTime
import java.util.UUID

trait DynamicDataGenerator {
  def randomUUID: UUID
  def currentTime: LocalDateTime
}
