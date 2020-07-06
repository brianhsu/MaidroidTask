package moe.brianhsu.maidroidtask.gateway.repo.memory

import java.util.UUID

import moe.brianhsu.maidroidtask.entity.Tag
import moe.brianhsu.maidroidtask.gateway.repo.TagRepo

class InMemoryTagRepo(data: InMemoryData) extends TagRepo {

  import data._

  override def read: TagReadable = new InMemoryTagRead
  override def write: TagWritable = new InMemoryTagWrite

  class InMemoryTagRead extends TagReadable {
    override def listByUserUUID(userUUID: UUID): List[Tag] = data.uuidToTag.values.filter(_.userUUID == userUUID).toList
    override def findByUUID(uuid: UUID): Option[Tag] = uuidToTag.get(uuid)
    override def hasChildren(uuid: UUID): Boolean = data.uuidToTag.values.exists(tag => tag.parentTagUUID.contains(uuid))
  }

  class InMemoryTagWrite extends TagWritable {
    override def insert(entity: Tag): Tag = {
      uuidToTag += (entity.uuid -> entity)
      entity
    }

    override def update(uuid: UUID, entity: Tag): Tag = {
      uuidToTag = uuidToTag.updated(uuid, entity)
      entity
    }
  }
}
