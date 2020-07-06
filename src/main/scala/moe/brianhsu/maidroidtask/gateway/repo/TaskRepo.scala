package moe.brianhsu.maidroidtask.gateway.repo

import moe.brianhsu.maidroidtask.entity.Task

trait TaskRepo {

  def read: TaskReadable
  def write: TaskWriteable

  trait TaskReadable extends Readable[Task]
  trait TaskWriteable extends Writable[Task]
}
