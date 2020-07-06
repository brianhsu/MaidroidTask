package moe.brianhsu.maidroidtask.usecase.project

import moe.brianhsu.maidroidtask.utils.fixture.{BaseFixture, BaseFixtureFeature}


class AddProjectFixture extends BaseFixture {
  val projectInSystem = createProject(loggedInUser, "ProjectName")
}

class AddProjectTest extends BaseFixtureFeature[AddProjectFixture] {
  override protected def createFixture: AddProjectFixture = new AddProjectFixture

  Feature("Validation before add project") {
    Scenario("Add a project with duplicate UUID") { fixture =>
      Given("user request to add a project with duplicated UUID")
      When("run the use case")
      Then("it should NOT pass the validation, and yield Duplicate error")
      pending
    }

    Scenario("Add a project without name") { fixture =>
      Given("user request to add a project with empty name")
      When("run the use case")
      Then("it should NOT pass the validation, and yield Required error")
      pending
    }

    Scenario("Add a project with a non-exist parent project") { fixture =>
      Given("user request to add a project with a non-exist parent UUID")
      When("run the use case")
      Then("it should NOT pass the validation, and yield NotFound error")
      pending
    }

    Scenario("Add a project with a parent project belongs to other user") { fixture =>
      Given("user request to add a project with a parent project belongs to other")
      When("run the use case")
      Then("it should NOT pass the validation, and yield AccessDenied error")
      pending
    }

    Scenario("Validation passed") { fixture =>
      Given("user request to add a project that is valid")
      When("run the use case")
      Then("it should pass the validation")
      pending
    }
  }

  Feature("Add project to storage") {
    Scenario("Add a project without parent") { fixture =>
      Given("user request to add a project without parent project")
      When("run the use case")
      Then("it should returned a project with correct data")
      And("store it to storage")
      And("generate correct journal entry")
      pending
    }

    Scenario("Add a project has parent project") { fixture =>
      Given("user already has a project in system")
      And("user request to add a project with parent project point to above project")
      When("run the use case")
      Then("it should returned a project with a parent project")
      And("store it to storage")
      And("generate correct journal entry")
      pending
    }
  }

}
