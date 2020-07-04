package moe.brianhsu.maidroidtask.entity

import java.time.LocalDateTime
import java.util.UUID

case class User(uuid: UUID, email: String, name: String,
                createTime: LocalDateTime = LocalDateTime.now(),
                updateTime: LocalDateTime = LocalDateTime.now()) extends Entity
