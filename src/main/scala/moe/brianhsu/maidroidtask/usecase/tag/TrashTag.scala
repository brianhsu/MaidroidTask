package moe.brianhsu.maidroidtask.usecase.tag

import java.util.UUID

import moe.brianhsu.maidroidtask.entity.{Journal, Tag, TrashLog, User}
import moe.brianhsu.maidroidtask.gateway.generator.DynamicDataGenerator
import moe.brianhsu.maidroidtask.gateway.repo.{TagRepo, TaskRepo}
import moe.brianhsu.maidroidtask.usecase.{UseCase, UseCaseExecutor}
import moe.brianhsu.maidroidtask.usecase.Validations.{ErrorDescription, HasChildren, ValidationRules}
import moe.brianhsu.maidroidtask.usecase.tag.TrashTag.Request
import moe.brianhsu.maidroidtask.usecase.task.RemoveTag
import moe.brianhsu.maidroidtask.usecase.validator.EntityValidator

object TrashTag {
  case class Request(loggedInUser: User, uuid: UUID)
}

class TrashTag(request: TrashTag.Request)(implicit tagRepo: TagRepo,
                                          taskRepo: TaskRepo,
                                          generator: DynamicDataGenerator,
                                          executor: UseCaseExecutor) extends UseCase[Tag] {

  private var cleanupJournals: List[Journal] = Nil
  private lazy val trashedTagHolder = tagRepo.read.findByUUID(request.uuid).map { tag =>
    tag.copy(
      isTrashed = true,
      updateTime = generator.currentTime
    )
  }

  private def cleanupTaskTags() = {
    taskRepo.read.findByTag(request.uuid).foreach { task =>
      val useCase = new RemoveTag(RemoveTag.Request(request.loggedInUser, task.uuid, request.uuid))
      val response = useCase.execute()
      cleanupJournals ++= response.journals
    }
  }

  override def doAction(): Tag = {
    cleanupTaskTags()
    trashedTagHolder.foreach { tag => tagRepo.write.update(request.uuid, tag) }
    trashedTagHolder.get
  }

  override def journals: List[Journal] = cleanupJournals ++ trashedTagHolder.map { tag =>
    TrashLog(
      generator.randomUUID,
      request.loggedInUser.uuid,
      request.uuid,
      tag,
      generator.currentTime
    )
  }.toList

  override def validations: List[ValidationRules] = {
    implicit val read = tagRepo.read

    def hasChildren(uuid: UUID): Option[ErrorDescription] = {
      if (tagRepo.read.hasChildren(uuid)) Some(HasChildren) else None
    }

    groupByField(
      createValidator("uuid", request.uuid,
        EntityValidator.exist[Tag],
        EntityValidator.belongToUser[Tag](request.loggedInUser),
        hasChildren
      )
    )
  }
}
