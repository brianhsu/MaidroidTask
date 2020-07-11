package moe.brianhsu.maidroidtask.gateway.repo

import moe.brianhsu.maidroidtask.entity.Project

trait ProjectRepo {
  val read: ProjectReadable
  val write: ProjectWritable
}

trait ProjectReadable extends Readable[Project] with ParentChildReadable[Project] with UserBasedReadable[Project]
trait ProjectWritable extends Writable[Project]
