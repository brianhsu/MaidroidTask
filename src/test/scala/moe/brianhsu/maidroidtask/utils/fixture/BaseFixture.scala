package moe.brianhsu.maidroidtask.utils.fixture

import java.time.LocalDateTime
import java.util.UUID

import moe.brianhsu.maidroidtask.entity.{Project, Tag, Task, User}
import moe.brianhsu.maidroidtask.gateway.generator.DynamicDataGenerator
import moe.brianhsu.maidroidtask.gateway.repo.{ProjectRepo, TagRepo, TaskRepo}
import moe.brianhsu.maidroidtask.gateway.repo.memory.{InMemoryData, InMemoryProjectRepo, InMemoryTagRepo, InMemoryTaskRepo}
import moe.brianhsu.maidroidtask.usecase.UseCaseExecutor

class BaseFixture {
  class FixedTestDataGenerator extends DynamicDataGenerator {
    override def randomUUID: UUID = UUID.fromString("4fe31c87-7130-4d61-9912-3fa6c0c9b7ef")
    override def currentTime: LocalDateTime = LocalDateTime.parse("2020-07-02T09:10:11")
  }

  private val inMemoryData = new InMemoryData

  implicit val generator: FixedTestDataGenerator = new FixedTestDataGenerator
  implicit val useCaseExecutor: UseCaseExecutor = new UseCaseExecutor
  implicit val taskRepo: TaskRepo = new InMemoryTaskRepo(inMemoryData)
  implicit val tagRepo: TagRepo = new InMemoryTagRepo(inMemoryData)
  implicit val projectRepo: ProjectRepo = new InMemoryProjectRepo(inMemoryData)

  val loggedInUser: User = User(UUID.randomUUID(), "user@example.com", "UserName")
  val otherUser: User = User(UUID.randomUUID(), "other@example.com", "OtherUser")

  def createTag(user: User, name: String, isTrashed: Boolean = false) = tagRepo.write.insert(Tag(UUID.randomUUID, user.uuid, name, None, isTrashed, LocalDateTime.now, LocalDateTime.now))
  def createTask(user: User, description: String, tags: List[UUID] = Nil, isTrashed: Boolean = false) = {
    taskRepo.write.insert(
      Task(
        UUID.randomUUID, user.uuid, description,
        tags = tags,
        isTrashed = isTrashed,
        createTime = LocalDateTime.now,
        updateTime = LocalDateTime.now
      )
    )
  }
  def createProject(user: User, name: String, parentProject: Option[UUID] = None, isTrashed: Boolean = false) = projectRepo.write.insert(
    Project(
      UUID.randomUUID, user.uuid, name,
      note = None,
      parentProjectUUID = parentProject,
      isTrashed= isTrashed,
      status = Project.Active,
      createTime = LocalDateTime.now,
      updateTime = LocalDateTime.now
    )
  )

}
