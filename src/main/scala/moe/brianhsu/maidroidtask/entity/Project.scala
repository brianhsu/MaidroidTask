package moe.brianhsu.maidroidtask.entity

import java.time.LocalDateTime
import java.util.UUID

import moe.brianhsu.maidroidtask.gateway.repo.Readable

import scala.annotation.tailrec

object Project {
  sealed trait Status
  case object Active extends Status
  case object Done extends Status
  case object Inactive extends Status
}

case class Project(uuid: UUID, userUUID: UUID,
                   name: String,
                   note: Option[String],
                   parentProjectUUID: Option[UUID],
                   status: Project.Status,
                   isTrashed: Boolean,
                   createTime: LocalDateTime,
                   updateTime: LocalDateTime) extends EntityWithUserId with NamedEntity with TrashableEntity {

  def hasLoopsWith(uuid: UUID)(implicit projectRead: Readable[Project]): Boolean = {

    @tailrec
    def hasLoopsInParent(parentProjectHolder: Option[Project]): Boolean = {
      parentProjectHolder match {
        case None => false
        case Some(p) if p.parentProjectUUID.contains(uuid) => true
        case Some(p) => hasLoopsInParent(p.parentProjectUUID.flatMap(projectRead.findByUUID))
      }
    }

    val projectHolder = projectRead.findByUUID(uuid)
    projectHolder match {
      case None => false
      case Some(project) =>
        project.parentProjectUUID.contains(this.uuid) ||
          hasLoopsInParent(parentProjectUUID.flatMap(projectRead.findByUUID))
    }
  }

}
