package moe.brianhsu.maidroidtask.usecase.task

import java.time.LocalDateTime
import java.util.UUID

import moe.brianhsu.maidroidtask.entity.{GroupedJournal, Change, ScheduledAt, Tag, Task, User}
import moe.brianhsu.maidroidtask.gateway.generator.DynamicDataGenerator
import moe.brianhsu.maidroidtask.gateway.repo.{Readable, TagRepo, TaskRepo}
import moe.brianhsu.maidroidtask.usecase.{UseCase, UseCaseRequest}
import moe.brianhsu.maidroidtask.usecase.Validations.ValidationRules
import moe.brianhsu.maidroidtask.usecase.validator.{EntityValidator, GenericValidator}

object EditTask {
  case class Request(loggedInUser: User, uuid: UUID,
                     description: Option[String] = None,
                     note: Option[Option[String]] = None,
                     project: Option[Option[UUID]] = None,
                     tags: Option[List[UUID]] = None,
                     dependsOn: Option[List[UUID]] = None,
                     waitUntil: Option[Option[LocalDateTime]] = None,
                     due: Option[Option[LocalDateTime]] = None,
                     scheduledAt: Option[Option[ScheduledAt]] = None,
                     isDone: Option[Boolean] = None) extends UseCaseRequest
}

class EditTask(request: EditTask.Request)(implicit taskRepo: TaskRepo, tagRepo: TagRepo, generator: DynamicDataGenerator) extends UseCase[Task] {
  private lazy val oldTask = taskRepo.read.findByUUID(request.uuid)
  private lazy val updateTask = oldTask.map { task =>
    task.copy(
      description = request.description.getOrElse(task.description),
      note = request.note.getOrElse(task.note),
      project = request.project.getOrElse(task.project),
      tags = request.tags.getOrElse(task.tags),
      dependsOn = request.dependsOn.getOrElse(task.dependsOn),
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

  override def groupedJournal: GroupedJournal = GroupedJournal(
    generator.randomUUID, request.loggedInUser.uuid,
    request, journals, generator.currentTime
  )

  private def journals: List[Change] = updateTask.map(task =>
    Change(generator.randomUUID, oldTask, task, generator.currentTime)
  ).toList

  override def validations: List[ValidationRules] = {

    implicit val readable: Readable[Task] = taskRepo.read
    implicit val tagReadable: Readable[Tag] = tagRepo.read

    import GenericValidator.option

    groupByField(
      createValidator("uuid", request.uuid,
        EntityValidator.exist[Task],
        EntityValidator.belongToUser[Task](request.loggedInUser),
        EntityValidator.notTrashed[Task]
      ),
      createValidator("description", request.description, option(GenericValidator.notEmpty)),
      createValidator("dependsOn", request.dependsOn, option(EntityValidator.allExist[Task])),
      createValidator("tags", request.tags,
        option(EntityValidator.allExist[Tag]),
        option(EntityValidator.allBelongToUser[Tag](request.loggedInUser)),
        option(EntityValidator.allNotTrashed[Tag])
      )
    )
  }

}
