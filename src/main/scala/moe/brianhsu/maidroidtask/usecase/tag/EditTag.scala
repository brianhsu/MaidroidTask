package moe.brianhsu.maidroidtask.usecase.tag

import java.util.UUID

import moe.brianhsu.maidroidtask.entity.{Journal, Tag, UpdateLog, User}
import moe.brianhsu.maidroidtask.gateway.generator.DynamicDataGenerator
import moe.brianhsu.maidroidtask.gateway.repo.TagRepo
import moe.brianhsu.maidroidtask.usecase.UseCase
import moe.brianhsu.maidroidtask.usecase.Validations.{Duplicated, ErrorDescription, ValidationRules}
import moe.brianhsu.maidroidtask.usecase.validator.{EntityValidator, GenericValidator}

object EditTag {
  case class Request(loggedInUser: User, uuid: UUID, name: String)
}

class EditTag(request: EditTag.Request)(implicit tagRepo: TagRepo, generator: DynamicDataGenerator) extends UseCase[Tag] {
  private lazy val updatedTag = tagRepo.read
    .findByUUID(request.uuid)
    .map(_.copy(name = request.name, updateTime = generator.currentTime))

  override def doAction(): Tag = {
    updatedTag.foreach(tag => tagRepo.write.update(request.uuid, tag))
    updatedTag.get
  }

  override def journals: List[Journal] = updatedTag.map(tag =>
    UpdateLog(
      generator.randomUUID, request.loggedInUser.uuid,
      request.uuid, tag, generator.currentTime
    )
  ).toList
  
  override def validations: List[ValidationRules] = {

    implicit val read: tagRepo.TagReadable = tagRepo.read

    def noSameNameForSameUser(loggedInUser: User)(name: String): Option[ErrorDescription] = {
      val tagsOfLoggedInUSer = tagRepo.read.listByUserUUID(loggedInUser.uuid)
      val hasDuplicate = tagsOfLoggedInUSer.exists(_.name == name)
      if (hasDuplicate) Some(Duplicated) else None
    }

    groupByField(
      createValidator("uuid", request.uuid,
        EntityValidator.exist[Tag],
        EntityValidator.belongToUser[Tag]((request.loggedInUser))
      ),
      createValidator("name", request.name,
        GenericValidator.notEmpty,
        noSameNameForSameUser(request.loggedInUser)
      )
    )
  }
}