package moe.brianhsu.maidroidtask.usecase.task

import java.util.UUID

import moe.brianhsu.maidroidtask.entity.{Change, Task}
import moe.brianhsu.maidroidtask.usecase.Validations.{AccessDenied, AlreadyTrashed, NotFound}
import moe.brianhsu.maidroidtask.usecase.base.types.ResultHolder
import moe.brianhsu.maidroidtask.utils.fixture.{BaseFixture, BaseFixtureFeature}

class AppendTagToTaskFixture extends BaseFixture {
  val userTag1 = createTag(loggedInUser, "ExistTag 1")
  val userTag2 = createTag(loggedInUser, "ExistTag 2")
  val userTag3 = createTag(loggedInUser, "ExistTag 3")
  val otherUserTag = createTag(otherUser, "OtherUserTag")

  val userTask = createTask(loggedInUser, "UserTask")
  val otherUserTask = createTask(otherUser, "OtherUserTask")

  def run(request: AppendTag.Request): ResultHolder[Task] = {
    val useCase = new AppendTag(request)
    useCase.execute()
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
      val response = fixture.run(request)

      Then("it should NOT pass the validation, and yield NotFound error")
      response should containsFailedValidation("uuid", NotFound)
    }

    Scenario("Append non-exist tag to task") { fixture =>
      Given("user request to append a non-exist tag to a task")
      val nonExistTagUUID = UUID.randomUUID
      val request = AppendTag.Request(fixture.loggedInUser, fixture.userTask.uuid, nonExistTagUUID)

      When("run the use case")
      val response = fixture.run(request)

      Then("it should NOT pass the validation, and yield NotFound error")
      response should containsFailedValidation("tagUUID", NotFound)
    }

    Scenario("Append tag to a task of other user") { fixture =>
      Given("user request to append a tag to other user's task")
      val request = AppendTag.Request(fixture.loggedInUser, fixture.otherUserTask.uuid, fixture.userTag1.uuid)

      When("run the use case")
      val response = fixture.run(request)

      Then("it should NOT pass the validation, and yield AccessDenied error")
      response should containsFailedValidation("uuid", AccessDenied)
    }

    Scenario("Append other user tag to as task") { fixture =>
      Given("user request to append other user's tag to his/her task")
      val request = AppendTag.Request(fixture.loggedInUser, fixture.userTask.uuid, fixture.otherUserTag.uuid)

      When("run the use case")
      val response = fixture.run(request)

      Then("it should NOT pass the validation, and yield AccessDenied error")
      response should containsFailedValidation("tagUUID", AccessDenied)
    }

    Scenario("Append to a trashed task") { fixture =>
      Given("user request to append a tag to a trashed task")
      val trashedTask = fixture.createTask(fixture.loggedInUser, "TrashedTask", isTrashed = true)
      val request = AppendTag.Request(fixture.loggedInUser, trashedTask.uuid, fixture.userTag1.uuid)

      When("run the use case")
      val response = fixture.run(request)

      Then("it should NOT pass the validation, and yield AlreadyTrashed error")
      response should containsFailedValidation("uuid", AlreadyTrashed)
    }

    Scenario("Append a trashed tag to a task") { fixture =>
      Given("user request to append a trashed tag a task")
      val task = fixture.createTask(fixture.loggedInUser, "SomeTask")
      val trashedTag = fixture.createTag(fixture.loggedInUser, "TrashedTag", isTrashed = true)
      val request = AppendTag.Request(fixture.loggedInUser, task.uuid, trashedTag.uuid)

      When("run the use case")
      val response = fixture.run(request)

      Then("it should NOT pass the validation, and yield AlreadyTrashed error")
      response should containsFailedValidation("tagUUID", AlreadyTrashed)
    }

    Scenario("Add tag to user's task") { fixture =>
      Given("user request to append a tag to his/her task")
      val request = AppendTag.Request(fixture.loggedInUser, fixture.userTask.uuid, fixture.userTag1.uuid)

      When("run the use case")
      val response = fixture.run(request)

      Then("it should pass the validation")
      response.success.value.result shouldBe a[Task]
    }
  }

  Feature("Add tag to a task") {
    Scenario("Append a tag to a task without any tag") { fixture =>
      Given("user request to append a tag to a task without any tag")
      val request = AppendTag.Request(fixture.loggedInUser, fixture.userTask.uuid, fixture.userTag1.uuid)

      When("run th use case")
      val response = fixture.run(request)

      Then("the returned task will has only the appended tag")
      val updatedTask = response.success.value.result
      inside(updatedTask) { case task: Task =>
        task.tags shouldBe List(fixture.userTag1.uuid)
        task.updateTime shouldBe fixture.generator.currentTime
      }

      And("store it in storage")
      val taskInStorage = fixture.taskRepo.read.findByUUID(fixture.userTask.uuid).value
      taskInStorage shouldBe updatedTask

      And("generate correct log")
      response.success.value.journals.changes shouldBe List(
        Change(fixture.generator.randomUUID, Some(fixture.userTask), updatedTask, fixture.generator.currentTime)
      )
    }

    Scenario("Append a tag to a task with some tags") { fixture =>
      Given("a task that has several tags")
      val tagsUUIDList = List(fixture.userTag1.uuid, fixture.userTag2.uuid)
      val task = fixture.createTask(fixture.loggedInUser, "SomeTask", tagsUUIDList)

      And("user request to append another tag into it")
      val request = AppendTag.Request(fixture.loggedInUser, task.uuid, fixture.userTag3.uuid)

      When("run th use case")
      val response = fixture.run(request)

      Then("the returned task will has the old tags and new one")
      val updatedTask = response.success.value.result
      inside(updatedTask) { case task: Task =>
        task.tags should contain theSameElementsAs List(fixture.userTag1.uuid, fixture.userTag2.uuid, fixture.userTag3.uuid)
        task.updateTime shouldBe fixture.generator.currentTime
      }

      And("store it in storage")
      val taskInStorage = fixture.taskRepo.read.findByUUID(task.uuid).value
      taskInStorage shouldBe updatedTask

      And("generate correct log")
      response.success.value.journals.changes shouldBe List(
        Change(fixture.generator.randomUUID, Some(task), updatedTask, fixture.generator.currentTime)
      )
    }

  }
}
