package moe.brianhsu.maidroidtask.usecase.project

import java.util.UUID

import moe.brianhsu.maidroidtask.entity.{Change, Journal, Project, User}
import moe.brianhsu.maidroidtask.gateway.repo.ProjectReadable
import moe.brianhsu.maidroidtask.usecase.Validations.ValidationRules
import moe.brianhsu.maidroidtask.usecase.base.{UseCase, UseCaseRequest, UseCaseRuntime}
import moe.brianhsu.maidroidtask.usecase.validator.{EntityValidator, GenericValidator}

object AddProject {

  case class Request(loggedInUser: User, uuid: UUID,
                     name: String,
                     note: Option[String] = None,
                     parentProjectUUID: Option[UUID] = None,
                     status: Project.Status = Project.Active) extends UseCaseRequest
}

class AddProject(request: AddProject.Request)(implicit runtime: UseCaseRuntime) extends UseCase[Project] {

  private lazy val project = Project(
    request.uuid, request.loggedInUser.uuid,
    request.name, request.note, request.parentProjectUUID,
    request.status, isTrashed = false,
    createTime = runtime.generator.currentTime,
    updateTime = runtime.generator.currentTime
  )

  override def doAction(): Project = {
    runtime.projectRepo.write.insert(project)
    project
  }

  override def journal: Journal = Journal(
    runtime.generator.randomUUID, request.loggedInUser.uuid,
    request, journals, runtime.generator.currentTime
  )

  private def journals: List[Change] = List(
    Change(runtime.generator.randomUUID, None, project, runtime.generator.currentTime)
  )

  override def validations: List[ValidationRules] = {

    import GenericValidator._
    implicit val projectRead: ProjectReadable = runtime.projectRepo.read

    groupByField(
      createValidator("uuid", request.uuid, EntityValidator.noCollision[Project]),
      createValidator("name", request.name,
        GenericValidator.notEmpty,
        EntityValidator.noSameNameForSameUser[Project](request.loggedInUser),
      ),
      createValidator("parentProjectUUID", request.parentProjectUUID,
        option(EntityValidator.exist[Project]),
        option(EntityValidator.notTrashed[Project]),
        option(EntityValidator.belongToUser[Project](request.loggedInUser))
      )
    )
  }
}
