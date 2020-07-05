package moe.brianhsu.maidroidtask.usecase.task

import java.time.LocalDateTime
import java.util.UUID

import moe.brianhsu.maidroidtask.entity.{Journal, Task, UpdateLog}
import moe.brianhsu.maidroidtask.usecase.Validations.{AccessDenied, NotFound}
import moe.brianhsu.maidroidtask.utils.fixture.{BaseFixture, BaseFixtureFeature}

import scala.util.Try

class AppendTagToTaskFixture extends BaseFixture {
  val userTag1 = createTag(loggedInUser, "ExistTag 1")
  val userTag2 = createTag(loggedInUser, "ExistTag 2")
  val userTag3 = createTag(loggedInUser, "ExistTag 3")
  val otherUserTag = createTag(otherUser, "OtherUserTag")

  val userTask = createTask(loggedInUser, "UserTask")
  val otherUserTask = createTask(otherUser, "OtherUserTask")

  def run(request: AppendTag.Request): (Try[Task], List[Journal]) = {
    val useCase = new AppendTag(request)
    (useCase.execute(), useCase.journals)
  }
}

class AppendTagTest extends BaseFixtureFeature[AppendTagToTaskFixture] {
  override protected def createFixture: AppendTagToTaskFixture = new AppendTagToTaskFixture

  Feature("Validation before append tag to task") {
    Scenario("Append tag to non-exist task") { fixture =>
      Given("user request to append a tag to non-exist task")
      val nonExistTaskUUID = UUID.randomUUID
      val request = AppendTag.Request(fixture.loggedInUser, nonExistTaskUUID, fixture.userTag1.uuid)

      When("run the use case")
      val (response, _) = fixture.run(request)

      Then("it should NOT pass the validation, and yield NotFound error")
      response should containsFailedValidation("uuid", NotFound)
    }

    Scenario("Append non-exist tag to task") { fixture =>
      Given("user request to append a non-exist tag to a task")
      val nonExistTagUUID = UUID.randomUUID
      val request = AppendTag.Request(fixture.loggedInUser, fixture.userTask.uuid, nonExistTagUUID)

      When("run the use case")
      val (response, _) = fixture.run(request)

      Then("it should NOT pass the validation, and yield NotFound error")
      response should containsFailedValidation("tagUUID", NotFound)
    }

    Scenario("Append tag to a task of other user") { fixture =>
      Given("user request to append a tag to other user's task")
      val request = AppendTag.Request(fixture.loggedInUser, fixture.otherUserTask.uuid, fixture.userTag1.uuid)

      When("run the use case")
      val (response, _) = fixture.run(request)

      Then("it should NOT pass the validation, and yield AccessDenied error")
      response should containsFailedValidation("uuid", AccessDenied)
    }

    Scenario("Append other user tag to as task") { fixture =>
      Given("user request to append other user's tag to his/her task")
      val request = AppendTag.Request(fixture.loggedInUser, fixture.userTask.uuid, fixture.otherUserTag.uuid)

      When("run the use case")
      val (response, _) = fixture.run(request)

      Then("it should NOT pass the validation, and yield AccessDenied error")
      response should containsFailedValidation("tagUUID", AccessDenied)
    }

    Scenario("Add tag to user's task") { fixture =>
      Given("user request to append a tag to his/her task")
      val request = AppendTag.Request(fixture.loggedInUser, fixture.userTask.uuid, fixture.userTag1.uuid)

      When("run the use case")
      val (response, _) = fixture.run(request)

      Then("it should pass the validation")
      response.success.value shouldBe a[Task]
    }
  }

  Feature("Add tag to a task") {
    Scenario("Append a tag to a task without any tag") { fixture =>
      Given("user request to append a tag to a task without any tag")
      val request = AppendTag.Request(fixture.loggedInUser, fixture.userTask.uuid, fixture.userTag1.uuid)

      When("run th use case")
      val (response, journals) = fixture.run(request)

      Then("the returned task will has only the appended tag")
      val updatedTask = response.success.value
      inside(updatedTask) { case task: Task =>
        task.tags shouldBe List(fixture.userTag1.uuid)
        task.updateTime shouldBe fixture.generator.currentTime
      }

      And("store it in storage")
      val taskInStorage = fixture.taskRepo.read.findByUUID(fixture.userTask.uuid).value
      taskInStorage shouldBe updatedTask

      And("generate correct log")
      journals shouldBe List(
        UpdateLog(
          fixture.generator.randomUUID,
          request.loggedInUser.uuid,
          request.uuid,
          updatedTask,
          fixture.generator.currentTime
        )
      )
    }

    Scenario("Append a tag to a task with some tags") { fixture =>
      Given("a task that has several tags")
      val tagsUUIDList = List(fixture.userTag1.uuid, fixture.userTag2.uuid)
      val task = fixture.createTask(fixture.loggedInUser, "SomeTask", tagsUUIDList)

      And("user request to append another tag into it")
      val request = AppendTag.Request(fixture.loggedInUser, task.uuid, fixture.userTag3.uuid)

      When("run th use case")
      val (response, journals) = fixture.run(request)

      Then("the returned task will has the old tags and new one")
      val updatedTask = response.success.value
      inside(updatedTask) { case task: Task =>
        task.tags should contain theSameElementsAs List(fixture.userTag1.uuid, fixture.userTag2.uuid, fixture.userTag3.uuid)
        task.updateTime shouldBe fixture.generator.currentTime
      }

      And("store it in storage")
      val taskInStorage = fixture.taskRepo.read.findByUUID(task.uuid).value
      taskInStorage shouldBe updatedTask

      And("generate correct log")
      journals shouldBe List(
        UpdateLog(
          fixture.generator.randomUUID,
          request.loggedInUser.uuid,
          request.uuid,
          updatedTask,
          fixture.generator.currentTime
        )
      )
    }

  }
}
