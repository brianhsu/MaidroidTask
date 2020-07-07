package moe.brianhsu.maidroidtask.usecase.task

import java.util.UUID

import moe.brianhsu.maidroidtask.entity.{GroupedJournal, Change, Task, User}
import moe.brianhsu.maidroidtask.gateway.generator.DynamicDataGenerator
import moe.brianhsu.maidroidtask.gateway.repo.{Readable, TaskRepo}
import moe.brianhsu.maidroidtask.usecase.{UseCase, UseCaseRequest}
import moe.brianhsu.maidroidtask.usecase.Validations.ValidationRules
import moe.brianhsu.maidroidtask.usecase.validator.EntityValidator

object TrashTask {
  case class Request(loggedInUser: User, uuid: UUID) extends UseCaseRequest
}

class TrashTask(request: TrashTask.Request)(implicit taskRepo: TaskRepo, generator: DynamicDataGenerator) extends UseCase[Task] {

  private lazy val oldTask = taskRepo.read.findByUUID(request.uuid)
  private lazy val updatedTaskHolder: Option[Task] = oldTask.map { task =>
    task.copy(
      isTrashed = true,
      updateTime = generator.currentTime
    )
  }

  override def doAction(): Task = {
    val updatedTask = updatedTaskHolder.get
    taskRepo.write.update(updatedTask.uuid, updatedTask)
    updatedTask
  }

  override def groupedJournal: GroupedJournal = GroupedJournal(
    generator.randomUUID, request.loggedInUser.uuid,
    request, journals, generator.currentTime
  )

  private def journals: List[Change] = updatedTaskHolder.map(task =>
    Change(generator.randomUUID, oldTask, task, generator.currentTime)
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
