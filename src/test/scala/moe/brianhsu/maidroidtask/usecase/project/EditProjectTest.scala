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
  }
}
