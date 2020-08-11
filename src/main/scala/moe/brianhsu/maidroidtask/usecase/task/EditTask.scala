package moe.brianhsu.maidroidtask.usecase.task

import java.time.LocalDateTime
import java.util.UUID

import moe.brianhsu.maidroidtask.entity.{Change, Journal, ScheduledAt, Tag, Task, User}
import moe.brianhsu.maidroidtask.gateway.repo.{Readable, TagReadable, TagRepo, TaskReadable}
import moe.brianhsu.maidroidtask.usecase.Validations.BreakingChain.MarkAsDone
import moe.brianhsu.maidroidtask.usecase.Validations.{BreakingChain, DependencyLoop, ErrorDescription, ValidationRules}
import moe.brianhsu.maidroidtask.usecase.base.{UseCase, UseCaseRequest, UseCaseRuntime}
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

class EditTask(request: EditTask.Request)(implicit runtime: UseCaseRuntime) extends UseCase[Task] {
  private lazy val oldTask = runtime.taskRepo.read.findByUUID(request.uuid)
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
      updateTime = runtime.generator.currentTime
    )
  }

  override def doAction(): Task = {
    updateTask.foreach(task => runtime.taskRepo.write.update(task.uuid, task))
    updateTask.get
  }

  override def journal: Journal = Journal(
    runtime.generator.randomUUID,
    request.loggedInUser.uuid,
    request, journals,
    runtime.generator.currentTime
  )

  private def journals: List[Change] = updateTask.map(task =>
    Change(
      runtime.generator.randomUUID, oldTask, task,
      runtime.generator.currentTime
    )
  ).toList

  override def validations: List[ValidationRules] = {

    implicit val taskReadable: TaskReadable = runtime.taskRepo.read
    implicit val tagReadable: TagReadable = runtime.tagRepo.read

    import GenericValidator.option

    def notCreateDependencyLoop(uuidList: List[UUID]): Option[ErrorDescription] = {
      val hasLoop = uuidList.exists(uuid => oldTask.exists(_.hasLoopsWith(uuid)))
      if (hasLoop) Some(DependencyLoop) else None
    }

    def notBreakingChain(isDone: Boolean): Option[ErrorDescription] = {
      val blockedBy: List[UUID] = getBlockedBy(taskReadable)
      val blocking: List[UUID] = taskReadable.findByDependsOn(request.uuid).map(_.uuid)
      val doesNotBreakDependencyChain = blockedBy == Nil && blocking == Nil

      if (!isDone || doesNotBreakDependencyChain) {
        None
      } else {
        Some(BreakingChain(MarkAsDone, blocking, blockedBy))
      }
    }

    groupByField(
      createValidator("uuid", request.uuid,
        EntityValidator.exist[Task],
        EntityValidator.belongToUser[Task](request.loggedInUser),
        EntityValidator.notTrashed[Task]
      ),
      createValidator("description", request.description, option(GenericValidator.notEmpty)),
      createValidator("dependsOn", request.dependsOn,
        option(EntityValidator.allExist[Task]),
        option(notCreateDependencyLoop)
      ),
      createValidator("tags", request.tags,
        option(EntityValidator.allExist[Tag]),
        option(EntityValidator.allBelongToUser[Tag](request.loggedInUser)),
        option(EntityValidator.allNotTrashed[Tag])
      ),
      createValidator("isDone", request.isDone,
        option(notBreakingChain)
      )
    )
  }

  private def getBlockedBy(readable: Readable[Task]): List[UUID] = {
    for {
      currentTask <- readable.findByUUID(request.uuid).toList
      parentTaskUUID <- currentTask.dependsOn
      nonCompleteTask <- readable.findByUUID(parentTaskUUID).filterNot(_.isDone)
    } yield {
      nonCompleteTask.uuid
    }
  }
}
