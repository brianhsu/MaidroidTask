package moe.brianhsu.maidroidtask.utils.fixture

import java.time.LocalDateTime
import java.util.UUID

import moe.brianhsu.maidroidtask.entity.{Project, Tag, Task, User}
import moe.brianhsu.maidroidtask.gateway.generator.DynamicDataGenerator
import moe.brianhsu.maidroidtask.gateway.repo.{ProjectRepo, TagRepo, TaskRepo}
import moe.brianhsu.maidroidtask.gateway.repo.memory.{InMemoryData, InMemoryProjectRepo, InMemoryTagRepo, InMemoryTaskRepo}
import moe.brianhsu.maidroidtask.usecase.base.{UseCaseExecutor, UseCaseRuntime}

class TestRuntime extends UseCaseRuntime {
  class FixedTestDataGenerator extends DynamicDataGenerator {
    override def randomUUID: UUID = UUID.fromString("4fe31c87-7130-4d61-9912-3fa6c0c9b7ef")
    override def currentTime: LocalDateTime = LocalDateTime.parse("2020-07-02T09:10:11")
  }

  private val inMemoryData = new InMemoryData

  override val generator: DynamicDataGenerator = new FixedTestDataGenerator
  override val taskRepo: TaskRepo = new InMemoryTaskRepo(inMemoryData)
  override val tagRepo: TagRepo = new InMemoryTagRepo(inMemoryData)
  override val projectRepo: ProjectRepo = new InMemoryProjectRepo(inMemoryData)
  override val executor: UseCaseExecutor = new UseCaseExecutor
}

class BaseFixture {

  implicit val runtime: TestRuntime = new TestRuntime

  implicit val generator: DynamicDataGenerator = runtime.generator
  implicit val useCaseExecutor: UseCaseExecutor = new UseCaseExecutor
  implicit val taskRepo: TaskRepo = runtime.taskRepo
  implicit val tagRepo: TagRepo = runtime.tagRepo
  implicit val projectRepo: ProjectRepo = runtime.projectRepo

  val loggedInUser: User = User(UUID.randomUUID(), "user@example.com", "UserName")
  val otherUser: User = User(UUID.randomUUID(), "other@example.com", "OtherUser")

  def createTag(user: User, name: String,
                parentTagUIUID: Option[UUID] = None,
                isTrashed: Boolean = false): Tag = {
    tagRepo.write.insert(
      Tag(
        UUID.randomUUID, user.uuid,
        name, parentTagUIUID, isTrashed,
        LocalDateTime.now, LocalDateTime.now
      )
    )
  }

  def createTask(user: User,
                 description: String,
                 tags: List[UUID] = Nil,
                 projectUUID: Option[UUID] = None,
                 dependsOn: List[UUID] = Nil,
                 isTrashed: Boolean = false,
                 isDone: Boolean = false): Task = {
    taskRepo.write.insert(
      Task(
        UUID.randomUUID, user.uuid, description,
        tags = tags,
        project = projectUUID,
        dependsOn = dependsOn,
        isTrashed = isTrashed,
        isDone = isDone,
        createTime = LocalDateTime.now,
        updateTime = LocalDateTime.now
      )
    )
  }

  def createProject(user: User, name: String, parentProject: Option[UUID] = None,
                    isTrashed: Boolean = false): Project = {
    projectRepo.write.insert(
      Project(
        UUID.randomUUID, user.uuid, name,
        note = None,
        parentProjectUUID = parentProject,
        isTrashed = isTrashed,
        status = Project.Active,
        createTime = LocalDateTime.now,
        updateTime = LocalDateTime.now
      )
    )
  }


}
