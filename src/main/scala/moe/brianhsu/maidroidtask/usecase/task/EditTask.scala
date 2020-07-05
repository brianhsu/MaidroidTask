package moe.brianhsu.maidroidtask.usecase.task

import java.time.LocalDateTime
import java.util.UUID

import moe.brianhsu.maidroidtask.entity.{Journal, Priority, ScheduledAt, Task, UpdateLog, User}
import moe.brianhsu.maidroidtask.gateway.generator.DynamicDataGenerator
import moe.brianhsu.maidroidtask.gateway.repo.{UserBasedReadable, TaskRepo}
import moe.brianhsu.maidroidtask.usecase.UseCase
import moe.brianhsu.maidroidtask.usecase.Validations.ValidationRules
import moe.brianhsu.maidroidtask.usecase.validator.{EntityValidator, GenericValidator}

object EditTask {
  case class Request(loggedInUser: User, uuid: UUID,
                     description: Option[String] = None,
                     note: Option[Option[String]] = None,
                     project: Option[Option[UUID]] = None,
                     tags: Option[List[UUID]] = None,
                     dependsOn: Option[List[UUID]] = None,
                     priority: Option[Option[Priority]] = None,
                     waitUntil: Option[Option[LocalDateTime]] = None,
                     due: Option[Option[LocalDateTime]] = None,
                     scheduledAt: Option[Option[ScheduledAt]] = None,
                     isDone: Option[Boolean] = None)
}

class EditTask(request: EditTask.Request)(implicit taskRepo: TaskRepo, generator: DynamicDataGenerator) extends UseCase[Task] {
  private lazy val updateTask = taskRepo.read.findByUUID(request.uuid).map { task =>
    task.copy(
      description = request.description.getOrElse(task.description),
      note = request.note.getOrElse(task.note),
      project = request.project.getOrElse(task.project),
      tags = request.tags.getOrElse(task.tags),
      dependsOn = request.dependsOn.getOrElse(task.dependsOn),
      priority = request.priority.getOrElse(task.priority),
      waitUntil = request.waitUntil.getOrElse(task.waitUntil),
      due = request.due.getOrElse(task.due),
      scheduledAt = request.scheduledAt.getOrElse(task.scheduledAt),
      isDone = request.isDone.getOrElse(task.isDone),
      updateTime = generator.currentTime
    )
  }

  override def doAction(): Task = {
    updateTask.foreach(task => taskRepo.write.update(task.uuid, task))
    updateTask.get
  }

  override def journals: List[Journal] = updateTask.map(task =>
    UpdateLog(generator.randomUUID, request.loggedInUser.uuid, task.uuid, task, generator.currentTime)
  ).toList

  override def validations: List[ValidationRules] = {
    implicit val readable: UserBasedReadable[Task] = taskRepo.read
    import GenericValidator.option

    groupByField(
      createValidator("uuid", request.uuid,
        EntityValidator.exist[Task],
        EntityValidator.belongToUser[Task](request.loggedInUser)
      ),
      createValidator("description", request.description, option(GenericValidator.notEmpty)),
      createValidator("dependsOn", request.dependsOn, option(EntityValidator.allExist[Task]))

    )
  }

}
