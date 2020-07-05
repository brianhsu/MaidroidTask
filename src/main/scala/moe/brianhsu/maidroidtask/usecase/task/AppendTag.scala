package moe.brianhsu.maidroidtask.usecase.task

import java.util.UUID

import moe.brianhsu.maidroidtask.entity.{Journal, Tag, Task, UpdateLog, User}
import moe.brianhsu.maidroidtask.gateway.generator.DynamicDataGenerator
import moe.brianhsu.maidroidtask.gateway.repo.{TagRepo, TaskRepo}
import moe.brianhsu.maidroidtask.usecase.UseCase
import moe.brianhsu.maidroidtask.usecase.Validations.ValidationRules
import moe.brianhsu.maidroidtask.usecase.validator.EntityValidator

object AppendTag {
  case class Request(loggedInUser: User, uuid: UUID, tagUUID: UUID)
}

class AppendTag(request: AppendTag.Request)
               (implicit taskRepo: TaskRepo, tagRepo: TagRepo,
                generator: DynamicDataGenerator) extends UseCase[Task] {

  private lazy val updatedTask = taskRepo.read.findByUUID(request.uuid).map { task =>
    task.copy(
      tags = request.tagUUID :: task.tags,
      updateTime = generator.currentTime
    )
  }

  override def doAction(): Task = {
    updatedTask.foreach(task => taskRepo.write.update(task.uuid, task))
    updatedTask.get
  }

  override def journals: List[Journal] = updatedTask.map { task =>
    UpdateLog(
      generator.randomUUID,
      request.loggedInUser.uuid,
      request.uuid,
      task,
      generator.currentTime
    )
  }.toList

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