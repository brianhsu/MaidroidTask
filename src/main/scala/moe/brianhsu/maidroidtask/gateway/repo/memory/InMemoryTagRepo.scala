package moe.brianhsu.maidroidtask.gateway.repo.memory

import java.util.UUID

import moe.brianhsu.maidroidtask.entity.Tag
import moe.brianhsu.maidroidtask.gateway.repo.TagRepo

class InMemoryTagRepo(data: InMemoryData) extends TagRepo {

  import data._

  override def read: TagReadable = new InMemoryTagRead
  override def write: TagWritable = new InMemoryTagWrite

  class InMemoryTagRead extends TagReadable {
    override def findByUUID(uuid: UUID): Option[Tag] = uuidToTag.get(uuid)
  }

  class InMemoryTagWrite extends TagWritable {
    override def insert(tag: Tag): Tag = {
      uuidToTag += (tag.uuid -> tag)
      tag
    }
  }
}
