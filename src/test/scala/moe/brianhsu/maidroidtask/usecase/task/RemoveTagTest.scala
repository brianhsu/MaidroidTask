package moe.brianhsu.maidroidtask.usecase.task

import java.time.LocalDateTime
import java.util.UUID

import moe.brianhsu.maidroidtask.entity.{Journal, Tag, Task}
import moe.brianhsu.maidroidtask.usecase.Validations.{AccessDenied, NotFound, ValidationErrors}
import moe.brianhsu.maidroidtask.utils.fixture.{BaseFixture, BaseFixtureFeature}

import scala.util.Try

class RemoveTagFixture extends BaseFixture {

  val userTag1 = createTag(loggedInUser, "User Tag1")
  val userTag2 = createTag(loggedInUser, "User Tag2")
  val userTag3 = createTag(loggedInUser, "User Tag3")

  val otherUserTag = createTag(otherUser, "Other User Tag")
  val userTask = createTask(loggedInUser, "Task1")
  val otherUserTask = createTask(otherUser, "Other User Task")

  def run(request: RemoveTag.Request): (Try[Task], List[Journal]) = {
    val useCase = new RemoveTag(request)
    (useCase.execute(), useCase.journals)
  }
}
class RemoveTagTest extends BaseFixtureFeature[RemoveTagFixture] {
  override protected def createFixture: RemoveTagFixture = new RemoveTagFixture

  Feature("Validation before remove tag from task") {
    Scenario("Remove a tag from non-exist task UUID") { fixture =>
      Given("user request to remove a tag from non-exist task UUID")
      val nonExistTaskUUID = UUID.randomUUID
      val request = RemoveTag.Request(fixture.loggedInUser, nonExistTaskUUID, fixture.userTag1.uuid)

      When("run the use case")
      val (response, _) = fixture.run(request)

      Then("it should not pass the validation, and yield NotFound error")
      response should containsFailedValidation("uuid", NotFound)
    }

    Scenario("Remove a non-exist tag from a task") { fixture =>
      Given("user request to remove a non-exist tag UUID from a task")
      val nonExistTagUUID = UUID.randomUUID
      val request = RemoveTag.Request(fixture.loggedInUser, fixture.userTask.uuid, nonExistTagUUID)

      When("run the use case")
      val (response, _) = fixture.run(request)

      Then("it should not pass the validation, and yield NotFound error")
      response should containsFailedValidation("tagUUID", NotFound)
    }

    Scenario("Remove a tag from other user's task") { fixture =>
      Given("user request to remove a tag from other user's task")
      val request = RemoveTag.Request(fixture.loggedInUser, fixture.otherUserTask.uuid, fixture.userTag1.uuid)

      When("run the use case")
      val (response, _) = fixture.run(request)

      Then("it should not pass the validation, and yield AccessDenied error")
      response should containsFailedValidation("uuid", AccessDenied)
    }

    Scenario("Remove other users tag from a task") { fixture =>
      Given("user request to remove a other users tag UUID from a task")
      val request = RemoveTag.Request(fixture.loggedInUser, fixture.userTask.uuid, fixture.otherUserTag.uuid)

      When("run the use case")
      val (response, _) = fixture.run(request)

      Then("it should not pass the validation, and yield AccessDeined error")
      response should containsFailedValidation("tagUUID", AccessDenied)
    }

    Scenario("Validation passed") { fixture =>
      Given("user request to remove a tag from his/her task ")
      val request = RemoveTag.Request(fixture.loggedInUser, fixture.userTask.uuid, fixture.userTag1.uuid)

      When("run the use case")
      val (response, _) = fixture.run(request)

      Then("it should pass the validation")
      response.success.value shouldBe a[Task]
    }
  }

  Feature("Remove the tag from a task and store it to storage") {
    Scenario("Remove a tag from a task not does not have any tag") { fixture =>
      Given("a task that does not have any tag")
      val task = fixture.createTask(fixture.loggedInUser, "SomeTask")

      And("user request to remove a tag from it")
      val request = RemoveTag.Request(fixture.loggedInUser, task.uuid, fixture.userTag1.uuid)

      Then("it should returned the task untouched")
      val (response, journals) = fixture.run(request)
      val returnedTask = response.success.value
      returnedTask shouldBe task

      And("the task in storage should not be changed")
      val taskInStorage = fixture.taskRepo.read.findByUUID(task.uuid).value
      taskInStorage shouldBe task

      And("there shouldn't have any journal")
      journals shouldBe Nil
    }

    Scenario("Remove a tag from a task not does not have the target tag") { fixture =>
      Given("a task that have tags, but not the target one")
      val targetTag = fixture.createTag(fixture.loggedInUser, "TargetTag")
      val task = fixture.createTask(
        fixture.loggedInUser, "SomeTask",
        tags = List(fixture.userTag1.uuid, fixture.userTag2.uuid)
      )

      And("user request to remove target tag from it")
      val request = RemoveTag.Request(fixture.loggedInUser, task.uuid, targetTag.uuid)

      Then("it should returned the task untouched")
      val (response, journals) = fixture.run(request)
      val returnedTask = response.success.value
      returnedTask shouldBe task

      And("the task in storage should not be changed")
      val taskInStorage = fixture.taskRepo.read.findByUUID(task.uuid).value
      taskInStorage shouldBe task

      And("there shouldn't have any journal")
      journals shouldBe Nil
    }

    Scenario("Remove a tag from a task has the target tag") { fixture =>
      Given("a task that has the target tag and other tags")
      val targetTag = fixture.createTag(fixture.loggedInUser, "TargetTag")
      val task = fixture.createTask(
        fixture.loggedInUser, "SomeTask",
        tags = List(fixture.userTag1.uuid, targetTag.uuid, fixture.userTag2.uuid)
      )

      And("user request to remove target tag from it")
      val request = RemoveTag.Request(fixture.loggedInUser, task.uuid, targetTag.uuid)

      Then("it should returned the task without the target task")
      val (response, journals) = fixture.run(request)
      val returnedTask = response.success.value
      returnedTask.tags should contain theSameElementsAs List(fixture.userTag1.uuid, fixture.userTag2.uuid)

      And("the task in storage should be updated")
      val taskInStorage = fixture.taskRepo.read.findByUUID(task.uuid).value
      taskInStorage shouldBe returnedTask
      taskInStorage.updateTime shouldBe fixture.generator.currentTime

      And("generate correct journal entry")
      pending
    }

  }
}
