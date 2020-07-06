package moe.brianhsu.maidroidtask.usecase.task

import java.time.LocalDateTime
import java.util.UUID

import moe.brianhsu.maidroidtask.entity.{Journal, Task, TrashLog}
import moe.brianhsu.maidroidtask.usecase.UseCaseExecutorResult
import moe.brianhsu.maidroidtask.usecase.Validations.{AccessDenied, FailedValidation, NotFound, ValidationErrors}
import moe.brianhsu.maidroidtask.utils.fixture.{BaseFixture, BaseFixtureFeature}

import scala.util.Try

class TrashTaskFixture extends BaseFixture {
  val otherUserTaskUUID = UUID.fromString("e334b225-d4b7-406a-a20d-3b0050a14b12")
  val taskUUID = UUID.fromString("8c2f648b-bbf3-4c00-978d-d910a5c7249e")
  private val fixtureCreateTime = LocalDateTime.parse("2020-07-30T11:12:13")

  taskRepo.write.insert(Task(otherUserTaskUUID, otherUser.uuid, "Task of Other User", createTime = fixtureCreateTime, updateTime = fixtureCreateTime))
  taskRepo.write.insert(Task(taskUUID, loggedInUser.uuid, "Task of LoggedIn User", createTime = fixtureCreateTime, updateTime = fixtureCreateTime))

  def run(request: TrashTask.Request): UseCaseExecutorResult[Task] = {
    val useCase = new TrashTask(request)
    useCase.execute()
  }

}

class TrashTaskTest extends BaseFixtureFeature[TrashTaskFixture] {

  override protected def createFixture: TrashTaskFixture = new TrashTaskFixture

  Feature("Validation before trash the task") {
    info("As a system administrator, I will like the system prevent")
    info("some trashed task that not exist or not belong to them.")

    Scenario("Delete non-exist task") { fixture =>
      Given("user request to trash a task does not exist")
      val uuidNotExist = UUID.fromString("a927508f-edfe-45f7-bdf8-ff1ef394673c")
      val request = TrashTask.Request(fixture.loggedInUser, uuidNotExist)

      When("run the use case")
      val response = fixture.run(request)

      Then("it should NOT pass the validation")
      response.result should containsFailedValidation("uuid", NotFound)
    }

    Scenario("Delete task does not belong to the logged in user") { fixture =>
      Given("user request to trash a task belongs to other")
      val request = TrashTask.Request(fixture.loggedInUser, fixture.otherUserTaskUUID)

      When("run the use case")
      val response = fixture.run(request)

      Then("it should NOT pass the validation")
      response.result should containsFailedValidation("uuid", AccessDenied)
    }

  }

  Feature("Mark the task as trashed") {
    Scenario("Delete the task") { fixture =>
      Given("user request to trash a task belong to them")
      val request = TrashTask.Request(fixture.loggedInUser, fixture.taskUUID)

      When("run the use case")
      val response = fixture.run(request)

      Then("it should mark as trashed in storage")
      val task = response.result.success.value
      val taskInStorage = fixture.taskRepo.read.findByUUID(task.uuid).value
      taskInStorage shouldBe task
      task.isTrashed shouldBe true
      task.updateTime shouldBe fixture.generator.currentTime

      And("generate correct journal entry")
      response.journals should contain theSameElementsInOrderAs List(
        TrashLog(
          fixture.generator.randomUUID, fixture.loggedInUser.uuid, task.uuid,
          taskInStorage, fixture.generator.currentTime
        )
      )
    }
  }

}
