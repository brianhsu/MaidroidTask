package moe.brianhsu.maidroidtask.usecase.tag

import java.util.UUID

import moe.brianhsu.maidroidtask.entity.{Change, Journal, Tag, User}
import moe.brianhsu.maidroidtask.gateway.repo.TagReadable
import moe.brianhsu.maidroidtask.usecase.Validations.ValidationRules
import moe.brianhsu.maidroidtask.usecase.base.{UseCase, UseCaseRequest, UseCaseRuntime}
import moe.brianhsu.maidroidtask.usecase.task.RemoveTag
import moe.brianhsu.maidroidtask.usecase.validator.EntityValidator

object TrashTag {
  case class Request(loggedInUser: User, uuid: UUID) extends UseCaseRequest
}

class TrashTag(request: TrashTag.Request)(implicit runtime: UseCaseRuntime) extends UseCase[Tag] {

  import runtime.executor

  private var cleanupJournals: List[Change] = Nil
  private lazy val oldTag = runtime.tagRepo.read.findByUUID(request.uuid)
  private lazy val trashedTagHolder = oldTag.map { tag =>
    tag.copy(
      isTrashed = true,
      updateTime = runtime.generator.currentTime
    )
  }

  private def cleanupTaskTags(): Unit = {
    runtime.taskRepo.read.findByTag(request.uuid).foreach { task =>
      val useCase = new RemoveTag(RemoveTag.Request(request.loggedInUser, task.uuid, request.uuid))
      val response = useCase.execute()
      cleanupJournals ++= response.map(_.journals.changes).getOrElse(Nil)
    }
  }

  override def doAction(): Tag = {
    cleanupTaskTags()
    trashedTagHolder.foreach { tag => runtime.tagRepo.write.update(request.uuid, tag) }
    trashedTagHolder.get
  }

  override def journal: Journal = Journal(
    runtime.generator.randomUUID, request.loggedInUser.uuid,
    request, journals, runtime.generator.currentTime
  )

  private def journals: List[Change] = cleanupJournals ++ trashedTagHolder.map { tag =>
    Change(runtime.generator.randomUUID, oldTag, tag, runtime.generator.currentTime)
  }.toList

  override def validations: List[ValidationRules] = {

    implicit val tagRead: TagReadable = runtime.tagRepo.read

    groupByField(
      createValidator("uuid", request.uuid,
        EntityValidator.exist[Tag],
        EntityValidator.belongToUser[Tag](request.loggedInUser),
        EntityValidator.hasNoUnTrashedChildren[Tag]
      )
    )
  }
}
