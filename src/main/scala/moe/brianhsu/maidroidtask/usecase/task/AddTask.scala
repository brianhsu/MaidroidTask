package moe.brianhsu.maidroidtask.usecase.task

import java.time.LocalDateTime
import java.util.UUID

import moe.brianhsu.maidroidtask.entity.{InsertLog, Journal, Priority, ScheduledAt, Task, User}
import moe.brianhsu.maidroidtask.gateway.generator.DynamicDataGenerator
import moe.brianhsu.maidroidtask.gateway.repo.TaskRepo
import moe.brianhsu.maidroidtask.usecase.UseCase
import moe.brianhsu.maidroidtask.usecase.Validations.{Duplicated, ErrorDescription, NotFound, Required, ValidationRules}
import moe.brianhsu.maidroidtask.usecase.task.AddTask.Request

object AddTask {
  case class Request(uuid: UUID, loggedInUser: User, description: String, note: Option[String] = None,
                     project: Option[UUID] = None,
                     tags: List[UUID] = Nil,
                     dependsOn: List[UUID] = Nil,
                     priority: Option[Priority] = None,
                     waitUntil: Option[LocalDateTime] = None,
                     due: Option[LocalDateTime] = None,
                     scheduledAt: Option[ScheduledAt] = None)
}

class AddTask(request: Request)(implicit val taskRepo: TaskRepo, generator: DynamicDataGenerator) extends UseCase[Task] {
  val task = Task(
    request.uuid, request.loggedInUser.uuid,
    request.description, request.note, request.project, request.tags,
    request.dependsOn, request.priority, request.waitUntil, request.due,
    request.scheduledAt, isDone = false, isTrashed = false,
    generator.currentTime, generator.currentTime
  )

  override def doAction(): Task = {
    taskRepo.write.insert(task)
    task
  }

  private def notEmpty(string: String): Option[ErrorDescription] = if (string.trim.isEmpty) Some(Required) else None

  private def noCollision(uuid: UUID): Option[ErrorDescription] = {
    if (taskRepo.read.findByUUID(uuid).isDefined) {
      Some(Duplicated)
    } else {
      None
    }
  }

  private def exist(uuid: UUID): Option[ErrorDescription] = {
    if (taskRepo.read.findByUUID(uuid).isEmpty) {
      Some(NotFound)
    } else {
      None
    }
  }

  override def journals: List[Journal] = List(
    InsertLog(generator.randomUUID, request.loggedInUser.uuid, request.uuid, task, generator.currentTime)
  )

  override def validations: List[ValidationRules] = groupByField(
    createValidator("uuid", request.uuid, noCollision),
    createValidator("description", request.description, notEmpty),
    createValidator("dependsOn", request.dependsOn, (d: List[UUID]) => d.flatMap(exist).headOption)
  )
}
