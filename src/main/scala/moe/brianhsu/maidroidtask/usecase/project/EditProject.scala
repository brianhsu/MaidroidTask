package moe.brianhsu.maidroidtask.usecase.project

import java.util.UUID

import moe.brianhsu.maidroidtask.entity.{Change, GroupedJournal, Project, User}
import moe.brianhsu.maidroidtask.usecase.{UseCase, UseCaseRequest, UseCaseRuntime}
import moe.brianhsu.maidroidtask.usecase.Validations.ValidationRules
import moe.brianhsu.maidroidtask.usecase.validator.EntityValidator
import moe.brianhsu.maidroidtask.usecase.validator.GenericValidator

object EditProject {
  case class Request(loggedInUser: User, uuid: UUID,
                     name: Option[String] = None,
                     note: Option[Option[String]] = None,
                     parentProjectUUID: Option[Option[UUID]] = None,
                     status: Option[Project.Status] = None) extends UseCaseRequest
}

class EditProject(request: EditProject.Request)(implicit runtime: UseCaseRuntime) extends UseCase[Project] {

  private lazy val oldProject = runtime.projectRepo.read.findByUUID(request.uuid)
  private lazy val updatedProject = oldProject.map { project =>
    project.copy(
      name = request.name.getOrElse(project.name),
      note = request.note.getOrElse(project.note),
      parentProjectUUID = request.parentProjectUUID.getOrElse(project.parentProjectUUID),
      status = request.status.getOrElse(project.status)
    )
  }
  override def doAction(): Project = {
    updatedProject.foreach(project => runtime.projectRepo.write.update(project.uuid, project))
    updatedProject.get
  }

  override def groupedJournal: GroupedJournal = GroupedJournal(
    runtime.generator.randomUUID, request.loggedInUser.uuid,
    request, journals, runtime.generator.currentTime
  )

  private def journals: List[Change] = updatedProject.map { project =>
    Change(runtime.generator.randomUUID, oldProject, project, runtime.generator.currentTime)
  }.toList

  override def validations: List[ValidationRules] = {

    import GenericValidator._
    implicit val projectRepo = runtime.projectRepo.read

    groupByField(
      createValidator("uuid", request.uuid,
        EntityValidator.exist[Project],
        EntityValidator.belongToUser[Project](request.loggedInUser),
        EntityValidator.notTrashed[Project]
      ),
      createValidator("name", request.name,
        option(EntityValidator.noSameNameForSameUser[Project](request.loggedInUser))
      ),
      createValidator("parentProjectUUID", request.parentProjectUUID,
        option(option(EntityValidator.exist[Project])),
        option(option(EntityValidator.belongToUser[Project](request.loggedInUser))),
        option(option(EntityValidator.notTrashed[Project]))
      )
    )
  }
}
