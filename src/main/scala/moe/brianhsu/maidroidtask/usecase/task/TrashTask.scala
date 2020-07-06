package moe.brianhsu.maidroidtask.usecase.task

import java.util.UUID

import moe.brianhsu.maidroidtask.entity.{Journal, Task, TrashLog, User}
import moe.brianhsu.maidroidtask.gateway.generator.DynamicDataGenerator
import moe.brianhsu.maidroidtask.gateway.repo.{Readable, TaskRepo}
import moe.brianhsu.maidroidtask.usecase.UseCase
import moe.brianhsu.maidroidtask.usecase.Validations.{Duplicated, ValidationRules}
import moe.brianhsu.maidroidtask.usecase.validator.EntityValidator

object TrashTask {
  case class Request(loggedInUser: User, uuid: UUID)
}

class TrashTask(request: TrashTask.Request)(implicit taskRepo: TaskRepo, generator: DynamicDataGenerator) extends UseCase[Task] {

  private lazy val updatedTaskHolder: Option[Task] = taskRepo
    .read
    .findByUUID(request.uuid)
    .map(_.copy(
      isTrashed = true,
      updateTime = generator.currentTime
    ))

  override def doAction(): Task = {
    val updatedTask = updatedTaskHolder.get
    taskRepo.write.update(updatedTask.uuid, updatedTask)
    updatedTask
  }

  override def journals: List[Journal] = updatedTaskHolder.map(task =>
    TrashLog(
      generator.randomUUID, request.loggedInUser.uuid, request.uuid,
      task, generator.currentTime
    )
  ).toList

  override def validations: List[ValidationRules] = {
    implicit val readable: Readable[Task] = taskRepo.read

    groupByField(
      createValidator("uuid", request.uuid,
        EntityValidator.exist[Task],
        EntityValidator.belongToUser[Task](request.loggedInUser)
      )
    )
  }
}
