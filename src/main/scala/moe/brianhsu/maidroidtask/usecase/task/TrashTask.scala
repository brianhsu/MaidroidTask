package moe.brianhsu.maidroidtask.usecase.task

import java.util.UUID

import moe.brianhsu.maidroidtask.entity.{Journal, Task, User}
import moe.brianhsu.maidroidtask.gateway.repo.{ReadableRepo, TaskRepo}
import moe.brianhsu.maidroidtask.usecase.UseCase
import moe.brianhsu.maidroidtask.usecase.Validations.{Duplicated, ValidationRules}
import moe.brianhsu.maidroidtask.usecase.validator.EntityValidator

object TrashTask {
  case class Request(loggedInUser: User, uuid: UUID)
}

class TrashTask(request: TrashTask.Request)(implicit taskRepo: TaskRepo) extends UseCase[Task] {
  override def doAction(): Task = null
  override def journals: List[Journal] = Nil
  override def validations: List[ValidationRules] = {
    implicit val readable: ReadableRepo[Task] = taskRepo.read

    groupByField(
      createValidator("uuid", request.uuid, EntityValidator.exist[Task])
    )
  }
}
