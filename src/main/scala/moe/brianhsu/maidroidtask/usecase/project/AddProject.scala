package moe.brianhsu.maidroidtask.usecase.project

import java.util.UUID

import moe.brianhsu.maidroidtask.entity.{GroupedJournal, Change, Project, User}
import moe.brianhsu.maidroidtask.gateway.generator.DynamicDataGenerator
import moe.brianhsu.maidroidtask.gateway.repo.ProjectRepo
import moe.brianhsu.maidroidtask.usecase.{UseCase, UseCaseRequest}
import moe.brianhsu.maidroidtask.usecase.Validations.ValidationRules
import moe.brianhsu.maidroidtask.usecase.validator.{EntityValidator, GenericValidator}

object AddProject {

  case class Request(loggedInUser: User, uuid: UUID,
                     name: String,
                     note: Option[String] = None,
                     parentProjectUUID: Option[UUID] = None,
                     status: Project.Status = Project.Active) extends UseCaseRequest
}

class AddProject(request: AddProject.Request)(implicit projectRepo: ProjectRepo, generator: DynamicDataGenerator) extends UseCase[Project] {

  private lazy val project = Project(
    request.uuid, request.loggedInUser.uuid,
    request.name, request.note, request.parentProjectUUID,
    request.status, isTrashed = false,
    createTime = generator.currentTime,
    updateTime = generator.currentTime
  )

  override def doAction(): Project = {
    projectRepo.write.insert(project)
    project
  }

  override def groupedJournal: GroupedJournal = GroupedJournal(
    generator.randomUUID, request.loggedInUser.uuid,
    request, journals, generator.currentTime
  )

  private def journals: List[Change] = List(
    Change(generator.randomUUID, None, project, generator.currentTime)
  )

  override def validations: List[ValidationRules] = {

    import GenericValidator._
    implicit val projectReadable = projectRepo.read

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
