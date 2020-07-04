package moe.brianhsu.maidroidtask.entity

import java.time.{LocalDate, LocalTime}

case class ScheduledAt(date: LocalDate, time: Option[LocalTime])
