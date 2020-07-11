package moe.brianhsu.maidroidtask.entity

import java.time.LocalDateTime
import java.util.UUID

import moe.brianhsu.maidroidtask.gateway.repo.Readable

import scala.annotation.tailrec

case class Tag(uuid: UUID, userUUID: UUID,
               name: String,
               parentTagUUID: Option[UUID],
               isTrashed: Boolean = false,
               createTime: LocalDateTime,
               updateTime: LocalDateTime) extends EntityWithUserId
                                          with NamedEntity
                                          with TrashableEntity {

  def hasLoopsWith(thatUUID: UUID)(implicit tagReadable: Readable[Tag]): Boolean = {

    @tailrec
    def hasLoopsInParent(parentProjectHolder: Option[Tag]): Boolean = {
      parentProjectHolder match {
        case None => false
        case Some(p) if p.parentTagUUID.contains(thatUUID) => true
        case Some(p) => hasLoopsInParent(p.parentTagUUID.flatMap(tagReadable.findByUUID))
      }
    }

    val thatTag = tagReadable.findByUUID(thatUUID)
    val isDependsOnEachOther = thatTag.exists(_.parentTagUUID contains this.uuid)
    val parentTagHolder = parentTagUUID.flatMap(tagReadable.findByUUID)

    isDependsOnEachOther || hasLoopsInParent(parentTagHolder)
  }

}
