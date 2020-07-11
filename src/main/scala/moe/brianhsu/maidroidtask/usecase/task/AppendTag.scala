package moe.brianhsu.maidroidtask.usecase.task

import java.util.UUID

import moe.brianhsu.maidroidtask.entity.{Change, Journal, Tag, Task, User}
import moe.brianhsu.maidroidtask.usecase.Validations.ValidationRules
import moe.brianhsu.maidroidtask.usecase.base.{UseCase, UseCaseRequest, UseCaseRuntime}
import moe.brianhsu.maidroidtask.usecase.validator.EntityValidator

object AppendTag {
  case class Request(loggedInUser: User, uuid: UUID, tagUUID: UUID) extends UseCaseRequest
}

class AppendTag(request: AppendTag.Request)
               (implicit runtime: UseCaseRuntime) extends UseCase[Task] {

  private lazy val oldTask = runtime.taskRepo.read.findByUUID(request.uuid)
  private lazy val updatedTask = oldTask.map { task =>
    runtime.taskRepo.write.appendTag(
      task.uuid, request.tagUUID,
      runtime.generator.currentTime
    )
  }

  override def doAction(): Task = updatedTask.get

  override def journal: Journal = Journal(
    runtime.generator.randomUUID,
    request.loggedInUser.uuid,
    request, journals,
    runtime.generator.currentTime
  )

  private def journals: List[Change] = List(
    Change(
      runtime.generator.randomUUID, oldTask,
      updatedTask.get,
      runtime.generator.currentTime
    )
  )

  override def validations: List[ValidationRules] = {
    implicit val taskRead= runtime.taskRepo.read
    implicit val tagRead = runtime.tagRepo.read

    groupByField(
      createValidator("uuid", request.uuid,
        EntityValidator.exist[Task],
        EntityValidator.belongToUser[Task](request.loggedInUser),
        EntityValidator.notTrashed[Task]
      ),
      createValidator("tagUUID", request.tagUUID,
        EntityValidator.exist[Tag],
        EntityValidator.belongToUser[Tag](request.loggedInUser),
        EntityValidator.notTrashed[Tag]
      )
    )
  }
}
