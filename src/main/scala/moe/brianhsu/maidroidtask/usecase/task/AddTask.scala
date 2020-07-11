package moe.brianhsu.maidroidtask.usecase.task

import java.time.LocalDateTime
import java.util.UUID

import moe.brianhsu.maidroidtask.entity.{Change, Journal, Project, ScheduledAt, Tag, Task, User}
import moe.brianhsu.maidroidtask.gateway.repo.Readable
import moe.brianhsu.maidroidtask.usecase.Validations.ValidationRules
import moe.brianhsu.maidroidtask.usecase.base.{UseCase, UseCaseRequest, UseCaseRuntime}
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
                     isDone: Boolean = false) extends UseCaseRequest
}

class AddTask(request: Request)(implicit runtime: UseCaseRuntime) extends UseCase[Task] {
  private val task = Task(
    request.uuid, request.loggedInUser.uuid,
    request.description, request.note, request.project, request.tags,
    request.dependsOn, request.waitUntil, request.due,
    request.scheduledAt, isDone = request.isDone, isTrashed = false,
    runtime.generator.currentTime, runtime.generator.currentTime
  )

  override def doAction(): Task = {
    runtime.taskRepo.write.insert(task)
    task
  }

  override def journal: Journal = Journal(
    runtime.generator.randomUUID, request.loggedInUser.uuid,
    request, journals, runtime.generator.currentTime
  )

  private def journals: List[Change] = List(
    Change(
      runtime.generator.randomUUID, None, task,
      runtime.generator.currentTime
    )
  )

  override def validations: List[ValidationRules] = {

    implicit val readable: Readable[Task] = runtime.taskRepo.read
    implicit val tagReadable: Readable[Tag] = runtime.tagRepo.read
    implicit val projectReadable: Readable[Project] = runtime.projectRepo.read
    import GenericValidator.option

    groupByField(
      createValidator("uuid", request.uuid, EntityValidator.noCollision[Task]),
      createValidator("description", request.description, GenericValidator.notEmpty),
      createValidator("dependsOn", request.dependsOn, EntityValidator.allExist[Task]),
      createValidator("tags", request.tags,
        EntityValidator.allExist[Tag],
        EntityValidator.allBelongToUser[Tag](request.loggedInUser),
        EntityValidator.allNotTrashed[Tag]
      ),
      createValidator("project", request.project, option(EntityValidator.notTrashed[Project]))
    )
  }
}
