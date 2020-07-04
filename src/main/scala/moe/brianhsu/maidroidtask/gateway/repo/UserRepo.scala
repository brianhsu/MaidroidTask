package moe.brianhsu.maidroidtask.gateway.repo

trait UserReadable
trait UserWritable

case class UserRepo(read: UserReadable, write: UserWritable)
