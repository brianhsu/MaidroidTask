package moe.brianhsu.maidroidtask.usecase.validator

import java.util.UUID

import moe.brianhsu.maidroidtask.gateway.repo.ReadableRepo
import moe.brianhsu.maidroidtask.usecase.Validations.{Duplicated, ErrorDescription, NotFound}

object EntityValidator {

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
}
