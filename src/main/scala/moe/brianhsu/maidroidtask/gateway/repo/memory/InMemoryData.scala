package moe.brianhsu.maidroidtask.gateway.repo.memory

import java.util.UUID

import moe.brianhsu.maidroidtask.entity.{Tag, Task}

class InMemoryData {
  var uuidToTag: Map[UUID, Tag] = Map.empty
  var uuidToTask: Map[UUID, Task] = Map.empty
}
