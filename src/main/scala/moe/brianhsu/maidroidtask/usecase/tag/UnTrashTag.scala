package moe.brianhsu.maidroidtask.usecase.tag

import java.util.UUID

import moe.brianhsu.maidroidtask.entity.{Change, Journal, Tag, User}
import moe.brianhsu.maidroidtask.gateway.repo.TagReadable
import moe.brianhsu.maidroidtask.usecase.Validations.{ErrorDescription, ParentIsTrashed, ValidationRules}
import moe.brianhsu.maidroidtask.usecase.validator.EntityValidator
import moe.brianhsu.maidroidtask.usecase.base.{UseCase, UseCaseRequest, UseCaseRuntime}

object UnTrashTag {
  case class Request(loggedInUser: User, uuid: UUID) extends UseCaseRequest
}

class UnTrashTag(request: UnTrashTag.Request)(implicit runtime: UseCaseRuntime) extends UseCase[Tag] {

  private lazy val oldTag = runtime.tagRepo.read.findByUUID(request.uuid)
  private lazy val updatedTag = oldTag.map { tag =>
    tag.copy(
      isTrashed = false,
      updateTime = runtime.generator.currentTime
    )
  }

  override def doAction(): Tag = {
    updatedTag.foreach(p => runtime.tagRepo.write.update(p.uuid, p))
    updatedTag.get
  }

  override def validations: List[ValidationRules] = {
    implicit val tagRepo: TagReadable = runtime.tagRepo.read

    def parentNotTrashed(uuid: UUID): Option[ErrorDescription] = {
      val isParentTrashed = oldTag
        .flatMap(_.parentTagUUID)
        .flatMap(tagRepo.findByUUID)
        .exists(_.isTrashed)

      if (isParentTrashed) Some(ParentIsTrashed) else None
    }

    groupByField(
      createValidator("uuid", request.uuid,
        EntityValidator.exist[Tag],
        EntityValidator.belongToUser[Tag](request.loggedInUser),
        EntityValidator.isTrashed[Tag],
        parentNotTrashed
      )
    )
  }

  override def journal: Journal = Journal(
    runtime.generator.randomUUID,
    request.loggedInUser.uuid,
    request,
    List(
      Change(runtime.generator.randomUUID, oldTag, updatedTag.get, runtime.generator.currentTime)
    ),
    runtime.generator.currentTime
  )
}
