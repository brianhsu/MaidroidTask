package moe.brianhsu.maidroidtask.usecase.validator

import java.util.UUID

import moe.brianhsu.maidroidtask.entity.{Entity, EntityWithUserId, NamedEntity, TrashableEntity, User}
import moe.brianhsu.maidroidtask.gateway.repo.{ParentChildReadable, Readable, UserBasedReadable}
import moe.brianhsu.maidroidtask.usecase.Validations.{AccessDenied, AlreadyTrashed, Duplicated, ErrorDescription, HasChildren, NotFound, NotTrashed}

object EntityValidator {

  def allExist[T](uuidList: List[UUID])(implicit readable: Readable[T]): Option[ErrorDescription] = {
    uuidList.foreach { uuid =>
      if (exist(uuid).isDefined) {
        return Some(NotFound)
      }
    }
    None
  }

  def noCollision[T](uuid: UUID)(implicit readable: Readable[T]): Option[ErrorDescription] = {
    if (readable.findByUUID(uuid).isDefined) {
      Some(Duplicated)
    } else {
      None
    }
  }

  def exist[T](uuid: UUID)(implicit readable: Readable[T]): Option[ErrorDescription] = {
    if (readable.findByUUID(uuid).isEmpty) {
      Some(NotFound)
    } else {
      None
    }
  }

  def allBelongToUser[T <: EntityWithUserId](loggedInUser: User)(uuidList: List[UUID])(implicit readable: Readable[T]): Option[ErrorDescription] = {
    uuidList.foreach { uuid =>
      if (belongToUser(loggedInUser)(uuid).isDefined) {
        return Some(AccessDenied)
      }
    }
    None
  }

  def belongToUser[T <: EntityWithUserId](loggedInUser: User)(entityUUID: UUID)(implicit readable: Readable[T]): Option[ErrorDescription] = {
    val userIdHolder = readable.findByUUID(entityUUID).map(_.userUUID)

    userIdHolder match {
      case Some(userUUID) if userUUID != loggedInUser.uuid => Some(AccessDenied)
      case _ => None
    }
  }

  def noSameNameForSameUser[T <: NamedEntity](loggedInUser: User)(name: String)(implicit readable: UserBasedReadable[T]): Option[ErrorDescription] = {
    val entityOfUser = readable.listByUserUUID(loggedInUser.uuid)
    val hasDuplicate = entityOfUser.exists(e => e.name == name && !e.isTrashed)
    if (hasDuplicate) Some(Duplicated) else None
  }

  def allNotTrashed[T <: TrashableEntity](uuidList: List[UUID])(implicit readable: Readable[T]): Option[ErrorDescription] = {
    uuidList.foreach { uuid =>
      if (notTrashed(uuid).isDefined) {
        return Some(AlreadyTrashed)
      }
    }
    None
  }

  def notTrashed[T <: TrashableEntity](uuid: UUID)(implicit readable: Readable[T]): Option[ErrorDescription] = {
    val isTrashed = readable.findByUUID(uuid).exists(_.isTrashed)
    if (isTrashed) Some(AlreadyTrashed) else None
  }

  def isTrashed[T <: TrashableEntity](uuid: UUID)(implicit readable: Readable[T]): Option[ErrorDescription] = {
    val isTrashed = readable.findByUUID(uuid).exists(_.isTrashed)
    if (!isTrashed) Some(NotTrashed) else None
  }

  def hasNoUnTrashedChildren[T <: TrashableEntity](uuid: UUID)(implicit readable: ParentChildReadable[T]): Option[ErrorDescription] = {
    if (readable.hasUnTrashedChildren(uuid)) Some(HasChildren) else None
  }

}
