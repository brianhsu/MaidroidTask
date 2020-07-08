package moe.brianhsu.maidroidtask.usecase.tag

import java.util.UUID

import moe.brianhsu.maidroidtask.entity.{GroupedJournal, Change, Tag, User}
import moe.brianhsu.maidroidtask.gateway.generator.DynamicDataGenerator
import moe.brianhsu.maidroidtask.gateway.repo.{TagRepo, TaskRepo}
import moe.brianhsu.maidroidtask.usecase.{UseCase, UseCaseExecutor, UseCaseRequest}
import moe.brianhsu.maidroidtask.usecase.Validations.{ErrorDescription, HasChildren, ValidationRules}
import moe.brianhsu.maidroidtask.usecase.task.RemoveTag
import moe.brianhsu.maidroidtask.usecase.validator.EntityValidator

object TrashTag {
  case class Request(loggedInUser: User, uuid: UUID) extends UseCaseRequest
}

class TrashTag(request: TrashTag.Request)(implicit tagRepo: TagRepo,
                                          taskRepo: TaskRepo,
                                          generator: DynamicDataGenerator,
                                          executor: UseCaseExecutor) extends UseCase[Tag] {

  private var cleanupJournals: List[Change] = Nil
  private lazy val oldTag = tagRepo.read.findByUUID(request.uuid)
  private lazy val trashedTagHolder = oldTag.map { tag =>
    tag.copy(
      isTrashed = true,
      updateTime = generator.currentTime
    )
  }

  private def cleanupTaskTags() = {
    taskRepo.read.findByTag(request.uuid).foreach { task =>
      val useCase = new RemoveTag(RemoveTag.Request(request.loggedInUser, task.uuid, request.uuid))
      val response = useCase.execute()
      cleanupJournals ++= response.map(_.journals.changes).getOrElse(Nil)
    }
  }

  override def doAction(): Tag = {
    cleanupTaskTags()
    trashedTagHolder.foreach { tag => tagRepo.write.update(request.uuid, tag) }
    trashedTagHolder.get
  }

  override def groupedJournal: GroupedJournal = GroupedJournal(
    generator.randomUUID, request.loggedInUser.uuid,
    request, journals, generator.currentTime
  )

  private def journals: List[Change] = cleanupJournals ++ trashedTagHolder.map { tag =>
    Change(generator.randomUUID, oldTag, tag, generator.currentTime)
  }.toList

  override def validations: List[ValidationRules] = {
    implicit val read = tagRepo.read

    groupByField(
      createValidator("uuid", request.uuid,
        EntityValidator.exist[Tag],
        EntityValidator.belongToUser[Tag](request.loggedInUser),
        EntityValidator.hasNoChild[Tag]
      )
    )
  }
}
