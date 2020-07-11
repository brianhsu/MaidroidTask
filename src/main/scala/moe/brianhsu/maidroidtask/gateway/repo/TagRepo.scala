package moe.brianhsu.maidroidtask.gateway.repo

import moe.brianhsu.maidroidtask.entity.Tag

trait TagRepo {
  val read: TagReadable
  val write: TagWritable
}

trait TagReadable extends Readable[Tag] with ParentChildReadable[Tag] with UserBasedReadable[Tag]
trait TagWritable extends Writable[Tag]
