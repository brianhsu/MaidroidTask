package moe.brianhsu.maidroidtask.usecase.project

import java.util.UUID

import moe.brianhsu.maidroidtask.entity.{Change, Journal, Project, User}
import moe.brianhsu.maidroidtask.usecase.Validations.ValidationRules
import moe.brianhsu.maidroidtask.usecase.base.{UseCase, UseCaseRequest, UseCaseRuntime}
import moe.brianhsu.maidroidtask.usecase.project.TrashProject.{MoveOneLevelUp, TrashCascade}
import moe.brianhsu.maidroidtask.usecase.validator.EntityValidator

object TrashProject {
  sealed trait CascadeMode
  case object TrashCascade extends CascadeMode
  case object MoveOneLevelUp extends CascadeMode

  case class Request(loggedInUser: User,
                     uuid: UUID,
                     cascadeMode: CascadeMode = TrashCascade) extends UseCaseRequest
}

class TrashProject(request: TrashProject.Request)
                  (implicit runtime: UseCaseRuntime) extends UseCase[Project] {

  private var cascadeChanges: List[Change] = Nil
  private lazy val oldProject = runtime.projectRepo.read.findByUUID(request.uuid)
  private lazy val updatedProject = oldProject.map { project =>
    project.copy(
      isTrashed = true,
      updateTime = runtime.generator.currentTime
    )
  }

  override def doAction(): Project = {

    this.cascadeChanges = request.cascadeMode match {
      case TrashCascade => cascadeTrashTask()
      case MoveOneLevelUp => moveOneLevelUp()
    }

    updatedProject.foreach { project => runtime.projectRepo.write.update(project.uuid, project) }
    updatedProject.get
  }

  private def moveOneLevelUp(): List[Change] = {

    runtime.taskRepo.read.findByProject(request.uuid).map { task =>
      val updatedTask = task.copy(
        project = oldProject.get.parentProjectUUID,
        updateTime = runtime.generator.currentTime
      )
      runtime.taskRepo.write.update(updatedTask.uuid, updatedTask)
      Change(runtime.generator.randomUUID, Some(task), updatedTask, runtime.generator.currentTime)
    }
  }

  private def cascadeTrashTask(): List[Change] = {
    runtime.taskRepo.read.findByProject(request.uuid).map { task =>
      val trashedTask = task.copy(isTrashed = true, updateTime = runtime.generator.currentTime)
      runtime.taskRepo.write.update(trashedTask.uuid, trashedTask)
      Change(runtime.generator.randomUUID, Some(task), trashedTask, runtime.generator.currentTime)
    }
  }

  override def validations: List[ValidationRules] = {

    implicit val projectRead = runtime.projectRepo.read

    groupByField(
      createValidator("uuid", request.uuid,
        EntityValidator.exist[Project],
        EntityValidator.notTrashed[Project],
        EntityValidator.hasNoUnTrashedChildren[Project],
        EntityValidator.belongToUser[Project](request.loggedInUser)
      ),
    )
  }

  private val projectChange = updatedProject.map { p =>
    Change(runtime.generator.randomUUID, oldProject, p, runtime.generator.currentTime)
  }.toList

  override def journal: Journal = Journal(
    runtime.generator.randomUUID,
    request.loggedInUser.uuid,
    request,
    projectChange ++ cascadeChanges,
    runtime.generator.currentTime
  )
}
