package moe.brianhsu.maidroidtask.usecase.tag

import moe.brianhsu.maidroidtask.utils.fixture.{BaseFixture, BaseFixtureFeature}

class EditTagFixture extends BaseFixture

class EditTagTest extends BaseFixtureFeature[EditTagFixture] {

  override protected def createFixture: EditTagFixture = new EditTagFixture

  Feature("Validation before edit") {
    Scenario("Edit a tag that does not exist") { fixture =>
      Given("user request to edit a tag that does not exist")
      When("run the use case")
      Then("it should NOT pass the validation, and yield NotFound error")
      pending
    }

    Scenario("Edit a tag that belongs to others") { fixture =>
      Given("user request to edit a tag that belongs to other user")
      When("run the use case")
      Then("it should NOT pass the validation, and yield AccessDenied error")
      pending
    }

    Scenario("Edit a tag with new name contains only spaces, tabs and newlines") { fixture =>
      Given("user request edit a tag that name is empty")
      When("run the use case")
      Then("it should NOT pass the validation, and yield Required error")
      pending
    }

    Scenario("Edit a tag, and name has duplication in system") { fixture =>
      Given("user request to edit a tag with name is already in system")
      When("run the use case")
      Then("it should NOT pass the validation, and yield Duplicate error")
      pending
    }

    Scenario("Validation passed") { fixture =>
      Given("user request to edit a tag belongs to him/her with non-empty name")
      When("run the use case")
      Then("it should pass the validation")
    }
  }

  Feature("Edit tag in system") {
    Scenario("Update tag name") { fixture =>
      Given("user request to edit a tag to a new tag name")
      When("run the use case")
      Then("it should return edited tag contains updated information")
      And("it should be stored in storage")
      And("generate correct journal entry")
      pending
    }
  }
}
