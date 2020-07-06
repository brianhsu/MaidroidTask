package moe.brianhsu.maidroidtask.usecase.task

import java.time.LocalDateTime
import java.util.UUID

import moe.brianhsu.maidroidtask.entity.{InsertLog, Journal, ScheduledAt, Tag, Task, User}
import moe.brianhsu.maidroidtask.gateway.generator.DynamicDataGenerator
import moe.brianhsu.maidroidtask.gateway.repo.{TagRepo, TaskRepo, Readable}
import moe.brianhsu.maidroidtask.usecase.UseCase
import moe.brianhsu.maidroidtask.usecase.Validations.ValidationRules
import moe.brianhsu.maidroidtask.usecase.task.AddTask.Request
import moe.brianhsu.maidroidtask.usecase.validator.{EntityValidator, GenericValidator}

object AddTask {
  case class Request(loggedInUser: User,
                     uuid: UUID,
                     description: String,
                     note: Option[String] = None,
                     project: Option[UUID] = None,
                     tags: List[UUID] = Nil,
                     dependsOn: List[UUID] = Nil,
                     waitUntil: Option[LocalDateTime] = None,
                     due: Option[LocalDateTime] = None,
                     scheduledAt: Option[ScheduledAt] = None,
                     isDone: Boolean = false)
}

class AddTask(request: Request)(implicit val taskRepo: TaskRepo, tagRepo: TagRepo, generator: DynamicDataGenerator) extends UseCase[Task] {
  private val task = Task(
    request.uuid, request.loggedInUser.uuid,
    request.description, request.note, request.project, request.tags,
    request.dependsOn, request.waitUntil, request.due,
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

    implicit val readable: Readable[Task] = taskRepo.read
    implicit val tagReadable: Readable[Tag] = tagRepo.read

    groupByField(
      createValidator("uuid", request.uuid, EntityValidator.noCollision[Task]),
      createValidator("description", request.description, GenericValidator.notEmpty),
      createValidator("dependsOn", request.dependsOn, EntityValidator.allExist[Task]),
      createValidator("tags", request.tags,
        EntityValidator.allExist[Tag],
        EntityValidator.allBelongToUser[Tag](request.loggedInUser)
      )
    )
  }
}
