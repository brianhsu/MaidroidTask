package moe.brianhsu.maidroidtask.gateway.repo.memory

import java.util.UUID

import moe.brianhsu.maidroidtask.entity.Project
import moe.brianhsu.maidroidtask.gateway.repo.ProjectRepo

class InMemoryProjectRepo(data: InMemoryData) extends ProjectRepo {

  import data._

  override val read: ProjectReadable = new InMemoryProjectReadable
  override val write: ProjectWritable = new InMemoryProjectWrite

  class InMemoryProjectReadable extends ProjectReadable {
    override def findByUUID(uuid: UUID): Option[Project] = uuidToProject.get(uuid)
    override def listByUserUUID(userUUID: UUID): List[Project] = data.uuidToProject.values.filter(_.userUUID == userUUID).toList
    override def hasUnTrashedChildren(uuid: UUID): Boolean = data.uuidToProject.values.exists { project =>
      project.parentProjectUUID.contains(uuid) && !project.isTrashed
    }

  }

  class InMemoryProjectWrite extends ProjectWritable {
    override def insert(entity: Project): Project = {
      uuidToProject += (entity.uuid -> entity)
      entity
    }

    override def update(uuid: UUID, entity: Project): Project = {
      uuidToProject = uuidToProject.updated(uuid, entity)
      entity
    }
  }
}
