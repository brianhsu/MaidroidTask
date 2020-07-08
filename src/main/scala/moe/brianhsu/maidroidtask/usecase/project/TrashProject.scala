package moe.brianhsu.maidroidtask.usecase.project

import java.util.UUID

import moe.brianhsu.maidroidtask.entity.{GroupedJournal, Project, User}
import moe.brianhsu.maidroidtask.gateway.generator.DynamicDataGenerator
import moe.brianhsu.maidroidtask.gateway.repo.ProjectRepo
import moe.brianhsu.maidroidtask.usecase.{UseCase, UseCaseRequest}
import moe.brianhsu.maidroidtask.usecase.Validations.ValidationRules
import moe.brianhsu.maidroidtask.usecase.validator.EntityValidator

object TrashProject {
  sealed trait CascadeMode
  case object TrashCascade extends CascadeMode
  case object MoveOneLevelUp extends CascadeMode

  case class Request(loggedInUser: User,
                     uuid: UUID,
                     cascadeMode: CascadeMode = TrashCascade) extends UseCaseRequest
}

class TrashProject(request: TrashProject.Request)(implicit projectRepo: ProjectRepo, generator: DynamicDataGenerator) extends UseCase[Project] {

  private lazy val oldProject = projectRepo.read.findByUUID(request.uuid)
  private lazy val updatedProject = oldProject.map { project =>
    project.copy(
      isTrashed = true,
      updateTime = generator.currentTime
    )
  }

  override def doAction(): Project = {
    updatedProject.get
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

  override def groupedJournal: GroupedJournal = GroupedJournal(
    generator.randomUUID,
    request.loggedInUser.uuid,
    request,
    Nil,
    generator.currentTime
  )
}
