package moe.brianhsu.maidroidtask.usecase.task

import java.util.UUID

import moe.brianhsu.maidroidtask.entity.{Change, GroupedJournal, Task, User}
import moe.brianhsu.maidroidtask.gateway.repo.Readable
import moe.brianhsu.maidroidtask.usecase.{UseCase, UseCaseRequest, UseCaseRuntime}
import moe.brianhsu.maidroidtask.usecase.Validations.ValidationRules
import moe.brianhsu.maidroidtask.usecase.validator.EntityValidator

object TrashTask {
  case class Request(loggedInUser: User, uuid: UUID) extends UseCaseRequest
}

class TrashTask(request: TrashTask.Request)(implicit runtime: UseCaseRuntime) extends UseCase[Task] {

  private lazy val oldTask = runtime.taskRepo.read.findByUUID(request.uuid)
  private lazy val updatedTaskHolder: Option[Task] = oldTask.map { task =>
    task.copy(
      isTrashed = true,
      updateTime = runtime.generator.currentTime
    )
  }

  override def doAction(): Task = {
    val updatedTask = updatedTaskHolder.get
    runtime.taskRepo.write.update(updatedTask.uuid, updatedTask)
    updatedTask
  }

  override def groupedJournal: GroupedJournal = GroupedJournal(
    runtime.generator.randomUUID,
    request.loggedInUser.uuid,
    request, journals,
    runtime.generator.currentTime
  )

  private def journals: List[Change] = updatedTaskHolder.map(task =>
    Change(
      runtime.generator.randomUUID, oldTask, task,
      runtime.generator.currentTime
    )
  ).toList

  override def validations: List[ValidationRules] = {
    implicit val readable: Readable[Task] = runtime.taskRepo.read

    groupByField(
      createValidator("uuid", request.uuid,
        EntityValidator.exist[Task],
        EntityValidator.belongToUser[Task](request.loggedInUser)
      )
    )
  }
}
