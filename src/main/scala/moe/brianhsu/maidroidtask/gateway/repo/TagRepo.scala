package moe.brianhsu.maidroidtask.gateway.repo

import java.util.UUID

import moe.brianhsu.maidroidtask.entity.Tag
import moe.brianhsu.maidroidtask.usecase.Validations

trait TagRepo {

  def read: TagReadable
  def write: TagWritable

  trait TagReadable extends ReadableRepo[Tag] {
    def listByUserUUID(userUUID: UUID): List[Tag]
    override def findByUUID(uuid: UUID): Option[Tag]
  }

  trait TagWritable {
    def update(uuid: UUID, updatedTag: Tag): Tag
    def insert(tag: Tag): Tag
  }

}
