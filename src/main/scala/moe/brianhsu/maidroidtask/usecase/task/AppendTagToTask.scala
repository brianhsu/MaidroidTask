package moe.brianhsu.maidroidtask.usecase.task

import java.util.UUID

import moe.brianhsu.maidroidtask.entity.{Journal, Task, User}
import moe.brianhsu.maidroidtask.gateway.repo.TaskRepo
import moe.brianhsu.maidroidtask.usecase.UseCase
import moe.brianhsu.maidroidtask.usecase.Validations.ValidationRules
import moe.brianhsu.maidroidtask.usecase.validator.EntityValidator

object AppendTagToTask {
  case class Request(loggedInUser: User, uuid: UUID, tagUUID: UUID)
}

class AppendTagToTask(request: AppendTagToTask.Request)(implicit taskRepo: TaskRepo) extends UseCase[Task] {
  override def doAction(): Task = null
  override def journals: List[Journal] = Nil
  override def validations: List[ValidationRules] = {
    implicit val taskRead= taskRepo.read
    groupByField(
      createValidator("uuid", request.uuid, EntityValidator.exist[Task])
    )
  }
}