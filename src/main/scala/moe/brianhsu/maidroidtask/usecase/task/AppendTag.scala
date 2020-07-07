package moe.brianhsu.maidroidtask.usecase.task

import java.util.UUID

import moe.brianhsu.maidroidtask.entity.{GroupedJournal, Change, Tag, Task, User}
import moe.brianhsu.maidroidtask.gateway.generator.DynamicDataGenerator
import moe.brianhsu.maidroidtask.gateway.repo.{TagRepo, TaskRepo}
import moe.brianhsu.maidroidtask.usecase.{UseCase, UseCaseRequest}
import moe.brianhsu.maidroidtask.usecase.Validations.ValidationRules
import moe.brianhsu.maidroidtask.usecase.validator.EntityValidator

object AppendTag {
  case class Request(loggedInUser: User, uuid: UUID, tagUUID: UUID) extends UseCaseRequest
}

class AppendTag(request: AppendTag.Request)
               (implicit taskRepo: TaskRepo, tagRepo: TagRepo,
                generator: DynamicDataGenerator) extends UseCase[Task] {

  private lazy val oldTask = taskRepo.read.findByUUID(request.uuid)
  private lazy val updatedTask = oldTask.map { task =>
    taskRepo.write.appendTag(task.uuid, request.tagUUID, generator.currentTime)
  }

  override def doAction(): Task = updatedTask.get

  override def groupedJournal: GroupedJournal = GroupedJournal(
    generator.randomUUID, request.loggedInUser.uuid,
    request, journals, generator.currentTime
  )

  private def journals: List[Change] = List(
    Change(generator.randomUUID, oldTask, updatedTask.get, generator.currentTime)
  )

  override def validations: List[ValidationRules] = {
    implicit val taskRead= taskRepo.read
    implicit val tagRead = tagRepo.read

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
