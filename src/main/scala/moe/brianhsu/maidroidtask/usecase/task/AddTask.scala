package moe.brianhsu.maidroidtask.usecase.task

import java.time.LocalDateTime
import java.util.UUID

import moe.brianhsu.maidroidtask.entity.{InsertLog, Journal, Priority, ScheduledAt, Task, User}
import moe.brianhsu.maidroidtask.gateway.generator.DynamicDataGenerator
import moe.brianhsu.maidroidtask.gateway.repo.{ReadableRepo, TaskRepo}
import moe.brianhsu.maidroidtask.usecase.UseCase
import moe.brianhsu.maidroidtask.usecase.Validations.{Duplicated, ErrorDescription, NotFound, Required, ValidationRules}
import moe.brianhsu.maidroidtask.usecase.task.AddTask.Request
import moe.brianhsu.maidroidtask.usecase.validator.{GenericValidator, EntityValidator}

object AddTask {
  case class Request(loggedInUser: User,
                     uuid: UUID,
                     description: String,
                     note: Option[String] = None,
                     project: Option[UUID] = None,
                     tags: List[UUID] = Nil,
                     dependsOn: List[UUID] = Nil,
                     priority: Option[Priority] = None,
                     waitUntil: Option[LocalDateTime] = None,
                     due: Option[LocalDateTime] = None,
                     scheduledAt: Option[ScheduledAt] = None,
                     isDone: Boolean = false)
}

class AddTask(request: Request)(implicit val taskRepo: TaskRepo, generator: DynamicDataGenerator) extends UseCase[Task] {
  private val task = Task(
    request.uuid, request.loggedInUser.uuid,
    request.description, request.note, request.project, request.tags,
    request.dependsOn, request.priority, request.waitUntil, request.due,
    request.scheduledAt, isDone = request.isDone, isTrashed = false,
    generator.currentTime, generator.currentTime
  )

  override def doAction(): Task = {
    taskRepo.write.insert(task)
    task
  }

  override def journals: List[Journal] = List(
    InsertLog(generator.randomUUID, request.loggedInUser.uuid, request.uuid, task, generator.currentTime)
  )

  override def validations: List[ValidationRules] = {

    implicit val readable: ReadableRepo[Task] = taskRepo.read

    groupByField(
      createValidator("uuid", request.uuid, EntityValidator.noCollision[Task]),
      createValidator("description", request.description, GenericValidator.notEmpty),
      createValidator("dependsOn", request.dependsOn, EntityValidator.allExist[Task])
    )
  }
}
