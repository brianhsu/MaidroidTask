package moe.brianhsu.maidroidtask.usecase.tag

import java.util.UUID

import moe.brianhsu.maidroidtask.entity.{GroupedJournal, Change, Tag, User}
import moe.brianhsu.maidroidtask.gateway.generator.DynamicDataGenerator
import moe.brianhsu.maidroidtask.gateway.repo.TagRepo
import moe.brianhsu.maidroidtask.usecase.{UseCase, UseCaseRequest}
import moe.brianhsu.maidroidtask.usecase.Validations.ValidationRules
import moe.brianhsu.maidroidtask.usecase.validator.{EntityValidator, GenericValidator}

object AddTag {
  case class Request(loggedInUser: User,
                     uuid: UUID,
                     name: String,
                     parentTagUUID: Option[UUID] = None) extends UseCaseRequest
}

class AddTag(request: AddTag.Request)(implicit tagRepo: TagRepo, generator: DynamicDataGenerator) extends UseCase[Tag] {
  private val currentTime = generator.currentTime
  private val tag = Tag(
    request.uuid, request.loggedInUser.uuid,
    request.name, request.parentTagUUID,
    isTrashed = false,
    currentTime, currentTime
  )
  override def doAction(): Tag = {
    tagRepo.write.insert(tag)
    tag
  }

  override def groupedJournal: GroupedJournal = GroupedJournal(
    generator.randomUUID, request.loggedInUser.uuid,
    request, journals, generator.currentTime
  )

  private def journals: List[Change] = List(
    Change(generator.randomUUID, None, tag, generator.currentTime)
  )

  override def validations: List[ValidationRules] = {
    implicit val tagReadable = tagRepo.read
    import GenericValidator.option

    groupByField(
      createValidator("uuid", request.uuid, EntityValidator.noCollision[Tag]),
      createValidator("name", request.name,
        GenericValidator.notEmpty,
        EntityValidator.noSameNameForSameUser[Tag](request.loggedInUser)
      ),
      createValidator(
        "parentTagUUID", request.parentTagUUID,
        option(EntityValidator.exist[Tag]),
        option(EntityValidator.belongToUser[Tag](request.loggedInUser))
      )
    )
  }
}
