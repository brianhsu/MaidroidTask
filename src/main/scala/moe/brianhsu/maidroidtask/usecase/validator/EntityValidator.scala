package moe.brianhsu.maidroidtask.usecase.validator

import java.util.UUID

import moe.brianhsu.maidroidtask.entity.{EntityWithUserId, User}
import moe.brianhsu.maidroidtask.gateway.repo.ReadableRepo
import moe.brianhsu.maidroidtask.usecase.Validations.{AccessDenied, Duplicated, ErrorDescription, NotFound}

object EntityValidator {

  def allExist[T](uuidList: List[UUID])(implicit readable: ReadableRepo[T]): Option[ErrorDescription] = {
    uuidList.foreach { uuid =>
      if (exist(uuid).isDefined) {
        return Some(NotFound)
      }
    }
    None
  }

  def noCollision[T](uuid: UUID)(implicit readable: ReadableRepo[T]): Option[ErrorDescription] = {
    if (readable.findByUUID(uuid).isDefined) {
      Some(Duplicated)
    } else {
      None
    }
  }

  def exist[T](uuid: UUID)(implicit readable: ReadableRepo[T]): Option[ErrorDescription] = {
    if (readable.findByUUID(uuid).isEmpty) {
      Some(NotFound)
    } else {
      None
    }
  }

  def belongToUser[T <: EntityWithUserId](loggedInUser: User)(entityUUID: UUID)(implicit readable: ReadableRepo[T]): Option[ErrorDescription] = {
    val userIdHolder = readable.findByUUID(entityUUID).map(_.userUUID)

    userIdHolder match {
      case Some(userUUID) if userUUID != loggedInUser.uuid => Some(AccessDenied)
      case _ => None
    }
  }

}