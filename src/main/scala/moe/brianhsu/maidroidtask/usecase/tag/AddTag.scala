package moe.brianhsu.maidroidtask.usecase.tag

import java.util.UUID

import moe.brianhsu.maidroidtask.entity.{Change, GroupedJournal, Tag, User}
import moe.brianhsu.maidroidtask.gateway.generator.DynamicDataGenerator
import moe.brianhsu.maidroidtask.gateway.repo.TagRepo
import moe.brianhsu.maidroidtask.usecase.{UseCase, UseCaseRequest, UseCaseRuntime}
import moe.brianhsu.maidroidtask.usecase.Validations.ValidationRules
import moe.brianhsu.maidroidtask.usecase.validator.{EntityValidator, GenericValidator}

object AddTag {
  case class Request(loggedInUser: User,
                     uuid: UUID,
                     name: String,
                     parentTagUUID: Option[UUID] = None) extends UseCaseRequest
}

class AddTag(request: AddTag.Request)(implicit runtime: UseCaseRuntime) extends UseCase[Tag] {
  private val currentTime = runtime.generator.currentTime
  private val tag = Tag(
    request.uuid, request.loggedInUser.uuid,
    request.name, request.parentTagUUID,
    isTrashed = false,
    currentTime, currentTime
  )
  override def doAction(): Tag = {
    runtime.tagRepo.write.insert(tag)
    tag
  }

  override def groupedJournal: GroupedJournal = GroupedJournal(
    runtime.generator.randomUUID, request.loggedInUser.uuid,
    request, journals, runtime.generator.currentTime
  )

  private def journals: List[Change] = List(
    Change(runtime.generator.randomUUID, None, tag, runtime.generator.currentTime)
  )

  override def validations: List[ValidationRules] = {

    import GenericValidator.option
    implicit val tagRead = runtime.tagRepo.read

    groupByField(
      createValidator("uuid", request.uuid, EntityValidator.noCollision[Tag]),
      createValidator("name", request.name,
        GenericValidator.notEmpty,
        EntityValidator.noSameNameForSameUser[Tag](request.loggedInUser)
      ),
      createValidator(
        "parentTagUUID", request.parentTagUUID,
        option(EntityValidator.exist[Tag]),
        option(EntityValidator.notTrashed[Tag]),
        option(EntityValidator.belongToUser[Tag](request.loggedInUser))
      )
    )
  }
}
