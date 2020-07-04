package moe.brianhsu.maidroidtask.usecase.tag

import java.util.UUID

import moe.brianhsu.maidroidtask.entity.{Journal, Tag, User}
import moe.brianhsu.maidroidtask.gateway.repo.TagRepo
import moe.brianhsu.maidroidtask.usecase.UseCase
import moe.brianhsu.maidroidtask.usecase.Validations.ValidationRules
import moe.brianhsu.maidroidtask.usecase.validator.EntityValidator

object AddTag {
  case class Request(loggedInUser: User, uuid: UUID, name: String)
}

class AddTag(request: AddTag.Request)(implicit tagRepo: TagRepo) extends UseCase[Tag] {
  override def doAction(): Tag = null

  override def journals: List[Journal] = Nil

  override def validations: List[ValidationRules] = {
    implicit val tagReadable = tagRepo.read
    groupByField(
      createValidator("uuid", request.uuid, EntityValidator.noCollision[Tag])
    )
  }
}
