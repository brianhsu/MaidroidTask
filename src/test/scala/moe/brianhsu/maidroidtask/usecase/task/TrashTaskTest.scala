package moe.brianhsu.maidroidtask.usecase.task

import moe.brianhsu.maidroidtask.usecase.fixture.{BaseFixture, BaseFixtureFeature}

class TrashTaskFixture extends BaseFixture

class TrashTaskTest extends BaseFixtureFeature[TrashTaskFixture] {

  override protected def createFixture: TrashTaskFixture = new TrashTaskFixture

  Feature("Validation before trash the task") {
    info("As a system administrator, I will like the system prevent")
    info("some trashed task that not exist or not belong to them.")

    Scenario("Delete non-exist task") { fixture =>
      Given("user request to trash a task does not exist")
      When("run the use case")
      Then("it should NOT pass the validation")
      pending
    }

    Scenario("Delete task does not belong to the logged in user") { fixture =>
      Given("user request to trash a task belongs to other")
      When("run the use case")
      Then("it should NOT pass the validation")
      pending
    }

  }

  Feature("Mark the task as trashed") {
    Scenario("Delete the task") { fixture =>
      Given("user request to trash a task belong to them")
      When("run the use case")
      Then("it should mark as trashed in storage")
      And("generate correct journal entry")
    }
  }

}
