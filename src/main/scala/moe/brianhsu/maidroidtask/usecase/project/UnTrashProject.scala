package moe.brianhsu.maidroidtask.usecase.project

import java.util.UUID

import moe.brianhsu.maidroidtask.entity.{Change, GroupedJournal, Project, User}
import moe.brianhsu.maidroidtask.usecase.Validations.{ErrorDescription, ParentIsTrashed, ValidationRules}
import moe.brianhsu.maidroidtask.usecase.validator.EntityValidator
import moe.brianhsu.maidroidtask.usecase.{UseCase, UseCaseRequest, UseCaseRuntime}

object UnTrashProject {
  case class Request(loggedInUser: User, uuid: UUID) extends UseCaseRequest
}

class UnTrashProject(request: UnTrashProject.Request)(implicit runtime: UseCaseRuntime) extends UseCase[Project] {

  private lazy val oldProject = runtime.projectRepo.read.findByUUID(request.uuid)
  private lazy val updatedProject = oldProject.map { case project =>
    project.copy(
      isTrashed = false,
      updateTime = runtime.generator.currentTime
    )
  }

  override def doAction(): Project = {
    updatedProject.foreach(p => runtime.projectRepo.write.update(p.uuid, p))
    updatedProject.get
  }

  override def validations: List[ValidationRules] = {
    implicit val projectRepo = runtime.projectRepo.read

    def parentNotTrashed(uuid: UUID): Option[ErrorDescription] = {
      val isParentTrashed = oldProject
        .flatMap(_.parentProjectUUID)
        .flatMap(projectRepo.findByUUID)
        .exists(_.isTrashed)

      if (isParentTrashed) Some(ParentIsTrashed) else None
    }

    groupByField(
      createValidator("uuid", request.uuid,
        EntityValidator.exist[Project],
        EntityValidator.belongToUser[Project](request.loggedInUser),
        EntityValidator.isTrashed[Project],
        parentNotTrashed
      )
    )
  }

  override def groupedJournal: GroupedJournal = GroupedJournal(
    runtime.generator.randomUUID,
    request.loggedInUser.uuid,
    request,
    List(
      Change(runtime.generator.randomUUID, oldProject, updatedProject.get, runtime.generator.currentTime)
    ),
    runtime.generator.currentTime
  )
}
