package moe.brianhsu.maidroidtask.usecase.project

import java.util.UUID

import moe.brianhsu.maidroidtask.entity.{Journal, Project}
import moe.brianhsu.maidroidtask.usecase.Validations.{AccessDenied, Duplicated, NotFound, Required}
import moe.brianhsu.maidroidtask.utils.fixture.{BaseFixture, BaseFixtureFeature}

import scala.util.Try

class AddProjectFixture extends BaseFixture {
  val projectInSystem = createProject(loggedInUser, "ProjectName")

  def run(request: AddProject.Request): (Try[Project], List[Journal]) = {
    val useCase = new AddProject(request)
    (useCase.execute(), useCase.journals)
  }
}

class AddProjectTest extends BaseFixtureFeature[AddProjectFixture] {
  override protected def createFixture: AddProjectFixture = new AddProjectFixture

  Feature("Validation before add project") {
    Scenario("Add a project with duplicate UUID") { fixture =>
      Given("user request to add a project with duplicated UUID")
      val request = AddProject.Request(fixture.loggedInUser, fixture.projectInSystem.uuid, "ProjectName")

      When("run the use case")
      val (response, _) = fixture.run(request)

      Then("it should NOT pass the validation, and yield Duplicate error")
      response should containsFailedValidation("uuid", Duplicated)
    }

    Scenario("Add a project without name") { fixture =>
      Given("user request to add a project with empty name")
      val request = AddProject.Request(fixture.loggedInUser, UUID.randomUUID, "   \t \n  ")

      When("run the use case")
      val (response, _) = fixture.run(request)

      Then("it should NOT pass the validation, and yield Required error")
      response should containsFailedValidation("name", Required)
    }

    Scenario("Add a project with a non-exist parent project") { fixture =>
      Given("user request to add a project with a non-exist parent UUID")
      val nonExistParentUUID = UUID.randomUUID
      val request = AddProject.Request(
        fixture.loggedInUser, UUID.randomUUID,
        "ChildParent", None,
        parentProjectUUID = Some(nonExistParentUUID)
      )

      When("run the use case")
      val (response, _) = fixture.run(request)

      Then("it should NOT pass the validation, and yield NotFound error")
      response should containsFailedValidation("parentProjectUUID", NotFound)
    }

    Scenario("Add a project with a parent project belongs to other user") { fixture =>
      Given("user request to add a project with a parent project belongs to other")
      val otherUserProject = fixture.createProject(fixture.otherUser, "OtherUserProject")
      val request = AddProject.Request(
        fixture.loggedInUser, UUID.randomUUID,
        "ChildParent", None,
        parentProjectUUID = Some(otherUserProject.uuid)
      )

      When("run the use case")
      val (response, _) = fixture.run(request)

      Then("it should NOT pass the validation, and yield AccessDenied error")
      response should containsFailedValidation("parentProjectUUID", AccessDenied)
    }

    Scenario("Add project with duplicate name") { fixture =>
      Given("user request to add a project has duplicate name with user's other project")
      val userProject = fixture.createProject(fixture.loggedInUser, "ProjectName")
      val request = AddProject.Request(fixture.loggedInUser, UUID.randomUUID, "ProjectName")

      When("run the use case")
      val (response, _) = fixture.run(request)

      Then("it should NOT pass the validation and yield Duplicate error")
      response should containsFailedValidation("name", Duplicated)
    }

    Scenario("Add project with duplicate name of other user's project") { fixture =>
      Given("user request to add a project has duplicate name with other user's project")
      When("run the use case")
      Then("it should NOt pass the validation")
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
