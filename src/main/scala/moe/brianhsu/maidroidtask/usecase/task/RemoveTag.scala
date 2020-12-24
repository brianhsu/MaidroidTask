package moe.brianhsu.maidroidtask.usecase.task

import java.util.UUID

import moe.brianhsu.maidroidtask.entity.{Change, Journal, Tag, Task, User}
import moe.brianhsu.maidroidtask.gateway.repo.{TagReadable, TaskReadable}
import moe.brianhsu.maidroidtask.usecase.Validations.ValidationRules
import moe.brianhsu.maidroidtask.usecase.base.{UseCase, UseCaseRequest, UseCaseRuntime}
import moe.brianhsu.maidroidtask.usecase.validator.EntityValidator

object RemoveTag {
  case class Request(loggedInUser: User, uuid: UUID, tagUUID: UUID) extends UseCaseRequest
}

class RemoveTag(request: RemoveTag.Request)
               (implicit runtime: UseCaseRuntime) extends UseCase[Task] {

  private lazy val oldTask = runtime.taskRepo.read.findByUUID(request.uuid)
  private lazy val shouldBeUpdated = oldTask.exists(_.tags.contains(request.tagUUID))
  private lazy val updatedTask = runtime.taskRepo.write.removeTag(request.uuid, request.tagUUID, runtime.generator.currentTime)

  override def doAction(): Task = {
    if (shouldBeUpdated) {
      updatedTask
    } else {
      oldTask.get
    }
  }

  override def journal: Journal = Journal(
    runtime.generator.randomUUID, request.loggedInUser.uuid,
    request, journals, runtime.generator.currentTime
  )

  private def journals: List[Change] = {

    if (shouldBeUpdated) {
      List(
        Change(
          runtime.generator.randomUUID, oldTask, updatedTask,
          runtime.generator.currentTime)
      )
    } else {
      Nil
    }
  }

  override def validations: List[ValidationRules] = {
    implicit val taskRead: TaskReadable = runtime.taskRepo.read
    implicit val tagRead: TagReadable = runtime.tagRepo.read

    groupByField(
      createValidator("uuid", request.uuid,
        EntityValidator.exist[Task],
        EntityValidator.belongToUser[Task](request.loggedInUser),
        EntityValidator.notTrashed[Task]
      ),
      createValidator("tagUUID", request.tagUUID,
        EntityValidator.exist[Tag],
        EntityValidator.belongToUser[Tag](request.loggedInUser)
      )
    )
  }
}
