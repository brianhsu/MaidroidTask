package moe.brianhsu.maidroidtask.usecase.tag

import java.util.UUID

import moe.brianhsu.maidroidtask.entity.{Journal, Tag, TrashLog, User}
import moe.brianhsu.maidroidtask.gateway.generator.DynamicDataGenerator
import moe.brianhsu.maidroidtask.gateway.repo.TagRepo
import moe.brianhsu.maidroidtask.usecase.UseCase
import moe.brianhsu.maidroidtask.usecase.Validations.{ErrorDescription, HasChildren, ValidationRules}
import moe.brianhsu.maidroidtask.usecase.validator.EntityValidator

object TrashTag {
  case class Request(loggedInUser: User, uuid: UUID)
}

class TrashTag(request: TrashTag.Request)(implicit tagRepo: TagRepo, generator: DynamicDataGenerator) extends UseCase[Tag] {
  private lazy val trashedTagHolder = tagRepo.read.findByUUID(request.uuid).map { tag =>
    tag.copy(
      isTrashed = true,
      updateTime = generator.currentTime
    )
  }
  override def doAction(): Tag = {
    trashedTagHolder.foreach { tag => tagRepo.write.update(request.uuid, tag) }
    trashedTagHolder.get
  }

  override def journals: List[Journal] = trashedTagHolder.map { tag =>
    TrashLog(
      generator.randomUUID,
      request.loggedInUser.uuid,
      request.uuid,
      tag,
      generator.currentTime
    )
  }.toList

  override def validations: List[ValidationRules] = {
    implicit val read = tagRepo.read

    def hasChildren(uuid: UUID): Option[ErrorDescription] = {
      if (tagRepo.read.hasChildren(uuid)) Some(HasChildren) else None
    }

    groupByField(
      createValidator("uuid", request.uuid,
        EntityValidator.exist[Tag],
        EntityValidator.belongToUser[Tag](request.loggedInUser),
        hasChildren
      )
    )
  }
}
