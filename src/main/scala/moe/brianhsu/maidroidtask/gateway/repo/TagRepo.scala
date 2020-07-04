package moe.brianhsu.maidroidtask.gateway.repo

import java.util.UUID

import moe.brianhsu.maidroidtask.entity.Tag

trait TagRepo {

  def read: TagReadable
  def write: TagWritable

  trait TagReadable extends ReadableRepo[Tag] {
    override def findByUUID(uuid: UUID): Option[Tag]
  }

  trait TagWritable {
    def insert(tag: Tag): Tag
  }

}
