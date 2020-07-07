package moe.brianhsu.maidroidtask.usecase.project

import moe.brianhsu.maidroidtask.utils.fixture.{BaseFixture, BaseFixtureFeature}

class EditProjectFixture extends BaseFixture

class EditProjectTest extends BaseFixtureFeature[EditProjectFixture] {
  override protected def createFixture: EditProjectFixture = new EditProjectFixture

  Feature("Validation before edit project") {
    Scenario("Edit a project UUID that does not exist") { fxiture =>
      Given("user request to edit a project with a UUID that is not exist")
      When("run the use case")
      Then("it should NOT pass the validation and yield NotFound error")
      pending
    }

    Scenario("Edit a project that belongs to other use") { fixture =>
      Given("user request to edit a project that belongs to others")
      When("run the use case")
      Then("it should NOT pass the validation and yield AccessDenied error")
      pending
    }

    Scenario("Edit a project that has duplicate name for same user") { fixture =>
      Given("user request to edit a project and new name is duplicated with user's other project")
      When("run the use case")
      Then("it should NOT pass the validation and yield Duplicate error")
      pending
    }

    Scenario("Edit a project that child-project does not exist") { fxiture =>
      Given("user request to edit a project that new child project is not exist")
      When("run the use case")
      Then("it should NOT pass the validation and yield NotFound error")
      pending
    }

    Scenario("Edit a project that child-project is belongs to other user") { fixture =>
      Given("user request to edit a project that new child project is belongs to other user")
      When("run the use case")
      Then("it should NOT pass the validation and yield AccessDenied error")
      pending
    }

    Scenario("Edit a project that name is duplicated with trashed project") { fixture =>
      Given("user request to edit a project")
      And("new project name is duplicated with trashed project")
      When("run the use case")
      Then("it should pass the validation")
      pending
    }

    Scenario("Edit a project that name is duplicate with other user's project") { fixture =>
      Given("user request to edit a project")
      And("new project name is duplicate with other user's project")
      When("it should pass the validation")
      pending
    }
  }

  Feature("Store edited project in storage") {
    Scenario("Edit a project with validate request") { fixture =>
      Given("a exist project in system")
      And("user request to edit that project with new attributes")
      When("run the request")
      Then("it should return edited project")
      And("store it in storage")
      And("generate correct journal entry")
    }
  }
}
