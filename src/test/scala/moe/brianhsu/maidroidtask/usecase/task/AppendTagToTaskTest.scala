package moe.brianhsu.maidroidtask.usecase.task

import java.time.LocalDateTime
import java.util.UUID

import moe.brianhsu.maidroidtask.entity.{Journal, Tag, Task}
import moe.brianhsu.maidroidtask.usecase.Validations.{FailedValidation, NotFound, ValidationErrors}
import moe.brianhsu.maidroidtask.utils.fixture.{BaseFixture, BaseFixtureFeature}

import scala.util.Try

class AppendTagToTaskFixture extends BaseFixture {
  private val fixtureCreateTime = LocalDateTime.parse("2020-07-30T11:12:13")

  val userTag = tagRepo.write.insert(Tag(UUID.randomUUID, loggedInUser.uuid, "ExistTag", None, isTrashed = false, generator.currentTime, generator.currentTime))
  val otherUserTag = tagRepo.write.insert(Tag(UUID.randomUUID, otherUser.uuid, "OtherUserTag", None, isTrashed = false, generator.currentTime, generator.currentTime))

  def run(request: AppendTagToTask.Request): (Try[Task], List[Journal]) = {
    val useCase = new AppendTagToTask(request)
    (useCase.execute(), useCase.journals)
  }
}

class AppendTagToTaskTest extends BaseFixtureFeature[AppendTagToTaskFixture] {
  override protected def createFixture: AppendTagToTaskFixture = new AppendTagToTaskFixture

  Feature("Validation before append tag to task") {
    Scenario("Append tag to non-exist task") { fixture =>
      Given("user request to append a tag to non-exist task")
      val nonExistTaskUUID = UUID.randomUUID
      val request = AppendTagToTask.Request(fixture.loggedInUser, nonExistTaskUUID, fixture.userTag.uuid)
      When("run the use case")
      val (response, _) = fixture.run(request)

      Then("it should NOT pass the validation, and yield NotFound error")
      response should containsFailedValidation("uuid", NotFound)
    }

    Scenario("Append non-exist tag to task") { fxiture =>
      Given("user request to append a non-exist tag to a task")
      When("run the use case")
      Then("it should NOT pass the validation, and yield NotFound error")
      pending
    }

    Scenario("Append tag to a task of other user") { fixture =>
      Given("user request to append a tag to other user's task")
      When("run the use case")
      Then("it should NOT pass the validation, and yield AccessDenied error")
      pending
    }

    Scenario("Append other user tag to as task") { fixture =>
      Given("user request to append other user's tag to his/her task")
      When("run the use case")
      Then("it should NOT pass the validation, and yield AccessDenied error")
      pending
    }

    Scenario("Add tag to user's task") { fixture =>
      Given("user request to append a tag to his/her task")
      When("run the use case")
      Then("it should pass the validation")
      pending
    }
  }

  Feature("Add tag to a task") {
    Scenario("Append a tag to a task without any tag") { fixture =>
      Given("user request to append a tag to a task without any tag")
      When("run th use case")
      Then("the returned task will has only the appended tag")
      And("store it in storage")
      And("generate correct log")
      pending
    }
    Scenario("Append a tag to a task with some tags") { fixture =>
      Given("a task that has several tags")
      And("user request to append another tag into it")
      When("run th use case")
      Then("the returned task will has the old tags and new one")
      And("store it in storage")
      And("generate correct log")
      pending
    }

  }
}
