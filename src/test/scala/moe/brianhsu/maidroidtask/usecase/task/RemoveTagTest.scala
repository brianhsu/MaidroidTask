package moe.brianhsu.maidroidtask.usecase.task

import java.time.LocalDateTime
import java.util.UUID

import moe.brianhsu.maidroidtask.entity.Tag
import moe.brianhsu.maidroidtask.utils.fixture.{BaseFixture, BaseFixtureFeature}

class RemoveTagFixture extends BaseFixture {

  val userTag1 = tagRepo.write.insert(Tag(UUID.randomUUID, loggedInUser.uuid, "ExistTag 1", None, isTrashed = false, LocalDateTime.now, LocalDateTime.now))
  val userTag2 = tagRepo.write.insert(Tag(UUID.randomUUID, loggedInUser.uuid, "ExistTag 2", None, isTrashed = false, LocalDateTime.now, LocalDateTime.now))
  val userTag3 = tagRepo.write.insert(Tag(UUID.randomUUID, loggedInUser.uuid, "ExistTag 3", None, isTrashed = false, LocalDateTime.now, LocalDateTime.now))

  val otherUserTag = tagRepo.write.insert(Tag(UUID.randomUUID, otherUser.uuid, "OtherUserTag", None, isTrashed = false, LocalDateTime.now, LocalDateTime.now))

}
class RemoveTagTest extends BaseFixtureFeature[RemoveTagFixture] {
  override protected def createFixture: RemoveTagFixture = new RemoveTagFixture

  Feature("Validation before remove tag from task") {
    Scenario("Remove a tag from non-exist task UUID") { fixture =>
      Given("user request to remove a tag from non-exist task UUID")
      When("run the use case")
      Then("it should not pass the validation, and yield NotFound error")
      pending
    }

    Scenario("Remove a non-exist tag from a task") { fixture =>
      Given("user request to remove a non-exist tag UUID from a task")
      When("run the use case")
      Then("it should not pass the validation, and yield NotFound error")
      pending
    }

    Scenario("Remove a tag from other user's task") { fixture =>
      Given("user request to remove a tag from other user's task")
      When("run the use case")
      Then("it should not pass the validation, and yield AccessDenied error")
      pending
    }

    Scenario("Remove other users tag from a task") { fixture =>
      Given("user request to remove a other users tag UUID from a task")
      When("run the use case")
      Then("it should not pass the validation, and yield AccessDeined error")
      pending
    }

    Scenario("Validation passed") { fixture =>
      Given("user request to remove a tag from his/her task ")
      When("run the use case")
      Then("it should pass the validation")
      pending
    }
  }

  Feature("Remove the tag from a task and store it to storage") {
    Scenario("Remove a tag from a task not does not have any tag") { fixture =>
      Given("a task that does not have any tag")
      And("user request to remove a tag from it")
      Then("it should returned the task untouched")
      And("the task in storage should not be changed")
      And("there shouldn't have any journal")
      pending
    }

    Scenario("Remove a tag from a task not does not have the target tag") { fixture =>
      Given("a task that have tags, but not the target one")
      And("user request to remove target tag from it")
      Then("it should returned the task untouched")
      And("the task in storage should not be changed")
      And("there shouldn't have any journal")
      pending
    }

    Scenario("Remove a tag from a task has the target tag") { fixture =>
      Given("a task that has the target tag and other tags")
      And("user request to remove target tag from it")
      Then("it should returned the task without the target task")
      And("the task in storage should be updated")
      And("generate correct journal entry")
      pending
    }

  }
}
