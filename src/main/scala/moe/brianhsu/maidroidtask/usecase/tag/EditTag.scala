package moe.brianhsu.maidroidtask.usecase.tag

import java.util.UUID

import moe.brianhsu.maidroidtask.entity.{Change, GroupedJournal, Tag, User}
import moe.brianhsu.maidroidtask.gateway.generator.DynamicDataGenerator
import moe.brianhsu.maidroidtask.gateway.repo.TagRepo
import moe.brianhsu.maidroidtask.usecase.{UseCase, UseCaseRequest, UseCaseRuntime}
import moe.brianhsu.maidroidtask.usecase.Validations.ValidationRules
import moe.brianhsu.maidroidtask.usecase.validator.{EntityValidator, GenericValidator}

object EditTag {
  case class Request(loggedInUser: User,
                     uuid: UUID,
                     name: Option[String] = None,
                     parentTagUUID: Option[Option[UUID]] = None) extends UseCaseRequest
}

class EditTag(request: EditTag.Request)(implicit runtime: UseCaseRuntime) extends UseCase[Tag] {
  private lazy val oldTag = runtime.tagRepo.read.findByUUID(request.uuid)
  private lazy val updatedTag = oldTag.map { tag =>
    tag.copy(
      name = request.name.getOrElse(tag.name),
      parentTagUUID = request.parentTagUUID.getOrElse(tag.parentTagUUID),
      updateTime = runtime.generator.currentTime
    )
  }

  override def doAction(): Tag = {
    updatedTag.foreach(tag => runtime.tagRepo.write.update(request.uuid, tag))
    updatedTag.get
  }

  override def groupedJournal: GroupedJournal = GroupedJournal(
    runtime.generator.randomUUID, request.loggedInUser.uuid,
    request, journals, runtime.generator.currentTime
  )

  private def journals: List[Change] = updatedTag.map(tag =>
    Change(runtime.generator.randomUUID, oldTag, tag, runtime.generator.currentTime)
  ).toList
  
  override def validations: List[ValidationRules] = {

    import GenericValidator.option
    implicit val tagRead = runtime.tagRepo.read

    groupByField(
      createValidator("uuid", request.uuid,
        EntityValidator.exist[Tag],
        EntityValidator.belongToUser[Tag](request.loggedInUser),
        EntityValidator.notTrashed[Tag]
      ),
      createValidator("name", request.name,
        option(GenericValidator.notEmpty),
        option(EntityValidator.noSameNameForSameUser[Tag](request.loggedInUser))
      ),
      createValidator("parentTagUUID", request.parentTagUUID,
        option(option(EntityValidator.exist[Tag])),
        option(option(EntityValidator.belongToUser[Tag](request.loggedInUser))),
        option(option(EntityValidator.notTrashed[Tag])),
      )
    )
  }
}
