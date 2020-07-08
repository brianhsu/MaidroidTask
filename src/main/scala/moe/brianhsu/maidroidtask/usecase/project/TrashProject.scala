package moe.brianhsu.maidroidtask.usecase.project

import java.util.UUID

import moe.brianhsu.maidroidtask.entity.{Change, GroupedJournal, Project, User}
import moe.brianhsu.maidroidtask.gateway.generator.DynamicDataGenerator
import moe.brianhsu.maidroidtask.gateway.repo.{ProjectRepo, TaskRepo}
import moe.brianhsu.maidroidtask.usecase.{UseCase, UseCaseRequest}
import moe.brianhsu.maidroidtask.usecase.Validations.ValidationRules
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
                  (implicit projectRepo: ProjectRepo,
                   taskRepo: TaskRepo,
                   generator: DynamicDataGenerator) extends UseCase[Project] {

  private var cascadeChanges: List[Change] = Nil
  private lazy val oldProject = projectRepo.read.findByUUID(request.uuid)
  private lazy val updatedProject = oldProject.map { project =>
    project.copy(
      isTrashed = true,
      updateTime = generator.currentTime
    )
  }

  override def doAction(): Project = {

    println("====> Mode:" + request.cascadeMode)
    this.cascadeChanges = request.cascadeMode match {
      case TrashCascade => cascadeTrashTask()
      case MoveOneLevelUp => moveOneLevelUp()
    }

    updatedProject.foreach { project => projectRepo.write.update(project.uuid, project) }
    updatedProject.get
  }

  private def moveOneLevelUp(): List[Change] = {
    println("====> moveOneLevelUp")

    taskRepo.read.findByProject(request.uuid).map { task =>
      println("====> moveOneLevelUp.parentProjectUUID:" + oldProject.get.parentProjectUUID)

      val updatedTask = task.copy(project = oldProject.get.parentProjectUUID, updateTime = generator.currentTime)
      taskRepo.write.update(updatedTask.uuid, updatedTask)
      Change(generator.randomUUID, Some(task), updatedTask, generator.currentTime)
    }
  }

  private def cascadeTrashTask(): List[Change] = {
    taskRepo.read.findByProject(request.uuid).map { task =>
      val trashedTask = task.copy(isTrashed = true, updateTime = generator.currentTime)
      taskRepo.write.update(trashedTask.uuid, trashedTask)
      Change(generator.randomUUID, Some(task), trashedTask, generator.currentTime)
    }
  }

  override def validations: List[ValidationRules] = {

    implicit val projectReadable = projectRepo.read

    groupByField(
      createValidator("uuid", request.uuid,
        EntityValidator.exist[Project],
        EntityValidator.notTrashed[Project],
        EntityValidator.hasNoChild[Project],
        EntityValidator.belongToUser[Project](request.loggedInUser)
      ),
    )
  }

  private val projectChange = updatedProject.map(p => Change(generator.randomUUID, oldProject, p, generator.currentTime)).toList
  override def groupedJournal: GroupedJournal = GroupedJournal(
    generator.randomUUID,
    request.loggedInUser.uuid,
    request,
    projectChange ++ cascadeChanges,
    generator.currentTime
  )
}
