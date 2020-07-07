package moe.brianhsu.maidroidtask.usecase.task

import java.util.UUID

import moe.brianhsu.maidroidtask.entity.{Journal, Tag, Task, User}
import moe.brianhsu.maidroidtask.gateway.generator.DynamicDataGenerator
import moe.brianhsu.maidroidtask.gateway.repo.{TagRepo, TaskRepo}
import moe.brianhsu.maidroidtask.usecase.{UseCase, UseCaseRequest}
import moe.brianhsu.maidroidtask.usecase.Validations.ValidationRules
import moe.brianhsu.maidroidtask.usecase.validator.EntityValidator

object RemoveTag {
  case class Request(loggedInUser: User, uuid: UUID, tagUUID: UUID) extends UseCaseRequest
}

class RemoveTag(request: RemoveTag.Request)
               (implicit taskRepo: TaskRepo,
                tagRepo: TagRepo, generator: DynamicDataGenerator) extends UseCase[Task] {

  private lazy val oldTask = taskRepo.read.findByUUID(request.uuid)
  private lazy val shouldBeUpdated = oldTask.exists(_.tags.contains(request.tagUUID))
  private lazy val updatedTask = taskRepo.write.removeTag(request.uuid, request.tagUUID, generator.currentTime)

  override def doAction(): Task = {
    if (shouldBeUpdated) {
      updatedTask
    } else {
      oldTask.get
    }
  }

  override def journals: List[Journal] = {

    if (shouldBeUpdated) {
      List(
        Journal(generator.randomUUID, request.loggedInUser.uuid, request, oldTask, updatedTask, generator.currentTime)
      )
    } else {
      Nil
    }
  }

  override def validations: List[ValidationRules] = {
    implicit val taskReadable = taskRepo.read
    implicit val tagReadable = tagRepo.read

    groupByField(
      createValidator("uuid", request.uuid,
        EntityValidator.exist[Task],
        EntityValidator.belongToUser[Task](request.loggedInUser)
      ),
      createValidator("tagUUID", request.tagUUID,
        EntityValidator.exist[Tag],
        EntityValidator.belongToUser[Tag](request.loggedInUser)
      )
    )
  }
}
