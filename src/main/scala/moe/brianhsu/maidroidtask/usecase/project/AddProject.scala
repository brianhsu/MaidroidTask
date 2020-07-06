package moe.brianhsu.maidroidtask.usecase.project

import java.util.UUID

import moe.brianhsu.maidroidtask.entity.{Journal, Project, User}
import moe.brianhsu.maidroidtask.gateway.repo.ProjectRepo
import moe.brianhsu.maidroidtask.usecase.{UseCase, validator}
import moe.brianhsu.maidroidtask.usecase.Validations.ValidationRules
import moe.brianhsu.maidroidtask.usecase.validator.{EntityValidator, GenericValidator}

object AddProject {

  case class Request(loggedInUser: User, uuid: UUID,
                     name: String,
                     note: Option[String] = None,
                     parentProjectUUID: Option[UUID] = None,
                     status: Project.Status = Project.Active)
}

class AddProject(request: AddProject.Request)(implicit projectRepo: ProjectRepo) extends UseCase[Project] {
  override def doAction(): Project = null

  override def journals: List[Journal] = Nil

  override def validations: List[ValidationRules] = {

    import GenericValidator._
    implicit val projectReadable = projectRepo.read

    groupByField(
      createValidator("uuid", request.uuid, EntityValidator.noCollision[Project]),
      createValidator("name", request.name,
        GenericValidator.notEmpty,
        EntityValidator.noSameNameForSameUser[Project](request.loggedInUser)
      ),
      createValidator("parentProjectUUID", request.parentProjectUUID,
        option(EntityValidator.exist[Project]),
        option(EntityValidator.belongToUser[Project](request.loggedInUser))
      )
    )
  }
}
