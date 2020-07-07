package moe.brianhsu.maidroidtask.usecase.tag

import java.util.UUID

import moe.brianhsu.maidroidtask.entity.{GroupedJournal, Change, Tag, User}
import moe.brianhsu.maidroidtask.gateway.generator.DynamicDataGenerator
import moe.brianhsu.maidroidtask.gateway.repo.TagRepo
import moe.brianhsu.maidroidtask.usecase.{UseCase, UseCaseRequest}
import moe.brianhsu.maidroidtask.usecase.Validations.ValidationRules
import moe.brianhsu.maidroidtask.usecase.validator.{EntityValidator, GenericValidator}

object EditTag {
  case class Request(loggedInUser: User,
                     uuid: UUID,
                     name: Option[String] = None,
                     parentTagUUID: Option[Option[UUID]] = None) extends UseCaseRequest
}

class EditTag(request: EditTag.Request)(implicit tagRepo: TagRepo, generator: DynamicDataGenerator) extends UseCase[Tag] {
  private lazy val oldTag = tagRepo.read.findByUUID(request.uuid)
  private lazy val updatedTag = oldTag.map { tag =>
    tag.copy(
      name = request.name.getOrElse(tag.name),
      parentTagUUID = request.parentTagUUID.getOrElse(tag.parentTagUUID),
      updateTime = generator.currentTime
    )
  }

  override def doAction(): Tag = {
    updatedTag.foreach(tag => tagRepo.write.update(request.uuid, tag))
    updatedTag.get
  }

  override def groupedJournal: GroupedJournal = GroupedJournal(
    generator.randomUUID, request.loggedInUser.uuid,
    request, journals, generator.currentTime
  )

  private def journals: List[Change] = updatedTag.map(tag =>
    Change(generator.randomUUID, oldTag, tag, generator.currentTime)
  ).toList
  
  override def validations: List[ValidationRules] = {

    implicit val read = tagRepo.read

    import GenericValidator.option

    groupByField(
      createValidator("uuid", request.uuid,
        EntityValidator.exist[Tag],
        EntityValidator.belongToUser[Tag]((request.loggedInUser))
      ),
      createValidator("name", request.name,
        option(GenericValidator.notEmpty),
        option(EntityValidator.noSameNameForSameUser[Tag](request.loggedInUser))
      ),
      createValidator("parentTagUUID", request.parentTagUUID,
        option(option(EntityValidator.exist[Tag])),
        option(option(EntityValidator.belongToUser[Tag](request.loggedInUser)))
      )
    )
  }
}
