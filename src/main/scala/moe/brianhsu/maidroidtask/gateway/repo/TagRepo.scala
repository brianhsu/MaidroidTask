package moe.brianhsu.maidroidtask.gateway.repo

import moe.brianhsu.maidroidtask.entity.{Project, Tag}



trait TagRepo {

  def read: TagReadable
  def write: TagWritable

  trait TagReadable extends Readable[Tag] with ParentChildReadable[Tag] with UserBasedReadable[Tag]
  trait TagWritable extends Writable[Tag]

}
