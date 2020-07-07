package moe.brianhsu.maidroidtask.usecase.project

import java.util.UUID

import moe.brianhsu.maidroidtask.entity.{GroupedJournal, Change, Project, User}
import moe.brianhsu.maidroidtask.gateway.generator.DynamicDataGenerator
import moe.brianhsu.maidroidtask.gateway.repo.ProjectRepo
import moe.brianhsu.maidroidtask.usecase.{UseCase, UseCaseRequest}
import moe.brianhsu.maidroidtask.usecase.Validations.ValidationRules
import moe.brianhsu.maidroidtask.usecase.validator.EntityValidator

object EditProject {
  case class Request(loggedInUser: User, uuid: UUID,
                     name: Option[String] = None,
                     note: Option[Option[String]] = None,
                     parentProjectUUID: Option[Option[UUID]] = None,
                     status: Option[Project.Status] = None) extends UseCaseRequest
}

class EditProject(request: EditProject.Request)(implicit projectRepo: ProjectRepo, generator: DynamicDataGenerator) extends UseCase[Project] {

  private lazy val oldProject = projectRepo.read.findByUUID(request.uuid)
  private lazy val updatedProject = oldProject.map { project =>
    project.copy(
      name = request.name.getOrElse(project.name),
      note = request.note.getOrElse(project.note),
      parentProjectUUID = request.parentProjectUUID.getOrElse(project.parentProjectUUID),
      status = request.status.getOrElse(project.status)
    )
  }
  override def doAction(): Project = {
    updatedProject.foreach(project => projectRepo.write.update(project.uuid, project))
    updatedProject.get
  }

  override def groupedJournal: GroupedJournal = GroupedJournal(
    generator.randomUUID, request.loggedInUser.uuid,
    request, journals, generator.currentTime
  )

  private def journals: List[Change] = updatedProject.map { project =>
    Change(generator.randomUUID, oldProject, project, generator.currentTime)
  }.toList

  override def validations: List[ValidationRules] = {

    implicit val projectReadable = projectRepo.read

    import moe.brianhsu.maidroidtask.usecase.validator.GenericValidator._

    groupByField(
      createValidator("uuid", request.uuid,
        EntityValidator.exist[Project],
        EntityValidator.belongToUser[Project](request.loggedInUser)
      ),
      createValidator("name", request.name,
        option(EntityValidator.noSameNameForSameUser[Project](request.loggedInUser))
      ),
      createValidator("parentProjectUUID", request.parentProjectUUID,
        option(option(EntityValidator.exist[Project])),
        option(option(EntityValidator.belongToUser[Project](request.loggedInUser)))
      )
    )
  }
}
