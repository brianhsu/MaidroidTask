package moe.brianhsu.maidroidtask.usecase.project

import java.util.UUID

import moe.brianhsu.maidroidtask.entity.{Change, Project}
import moe.brianhsu.maidroidtask.usecase.Validations.{AccessDenied, AlreadyTrashed, Duplicated, NotFound, Required}
import moe.brianhsu.maidroidtask.usecase.base.types.ResultHolder
import moe.brianhsu.maidroidtask.utils.fixture.{BaseFixture, BaseFixtureFeature}

class AddProjectFixture extends BaseFixture {

  def run(request: AddProject.Request): ResultHolder[Project] = {
    val useCase = new AddProject(request)
    useCase.execute()
  }
}

class AddProjectTest extends BaseFixtureFeature[AddProjectFixture] {
  override protected def createFixture: AddProjectFixture = new AddProjectFixture

  Feature("Validation before add project") {
    Scenario("Add a project with duplicate UUID") { fixture =>
      Given("user request to add a project with duplicated UUID")
      val projectInSystem = fixture.createProject(fixture.loggedInUser, "ProjectName")
      val request = AddProject.Request(fixture.loggedInUser, projectInSystem.uuid, "ProjectName")

      When("run the use case")
      val response = fixture.run(request)

      Then("it should NOT pass the validation, and yield Duplicate error")
      response should containsFailedValidation("uuid", Duplicated)
    }

    Scenario("Add a project without name") { fixture =>
      Given("user request to add a project with empty name")
      val request = AddProject.Request(fixture.loggedInUser, UUID.randomUUID, "   \t \n  ")

      When("run the use case")
      val response = fixture.run(request)

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
      val response = fixture.run(request)

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
      val response = fixture.run(request)

      Then("it should NOT pass the validation, and yield AccessDenied error")
      response should containsFailedValidation("parentProjectUUID", AccessDenied)
    }

    Scenario("Add project with duplicate name") { fixture =>
      Given("user request to add a project has duplicate name with user's other project")
      val userProject = fixture.createProject(fixture.loggedInUser, "ProjectName")
      val request = AddProject.Request(fixture.loggedInUser, UUID.randomUUID, "ProjectName")

      When("run the use case")
      val response = fixture.run(request)

      Then("it should NOT pass the validation and yield Duplicate error")
      response should containsFailedValidation("name", Duplicated)
    }

    Scenario("Add a project with trashed parent project") { fixture =>
      Given("user request to add a project with a trashed parent project")
      val trashedProject = fixture.createProject(fixture.loggedInUser, "Trashed Project", isTrashed = true)
      val request = AddProject.Request(
        fixture.loggedInUser, UUID.randomUUID,
        "NewProject",
        parentProjectUUID = Some(trashedProject.uuid)
      )

      When("run the use case")
      val response = fixture.run(request)

      Then("it should NOT pass the validation and yield AlreadyTrashed")
      response should containsFailedValidation("parentProjectUUID", AlreadyTrashed)
    }

    Scenario("Add project with duplicate name of other user's project") { fixture =>
      Given("user request to add a project has duplicate name with other user's project")
      val otherUserProject = fixture.createProject(fixture.otherUser, "Some Project Name")
      val request = AddProject.Request(fixture.loggedInUser, UUID.randomUUID, "Some Project Name")

      When("run the use case")
      val response = fixture.run(request)

      Then("it should pass the validation")
      response.success.value.result shouldBe a[Project]
    }

    Scenario("Add project with duplicate name of logged user's trahsed project") { fixture =>
      Given("user request to add a project has duplicate name with other user's project")
      val trashedProject = fixture.createProject(fixture.loggedInUser, "Some Project Name", isTrashed = true)
      val request = AddProject.Request(fixture.loggedInUser, UUID.randomUUID, "Some Project Name")

      When("run the use case")
      val response = fixture.run(request)

      Then("it should pass the validation")
      response.success.value.result shouldBe a[Project]
    }

    Scenario("Validation passed") { fixture =>
      Given("user request to add a project that is valid")
      val request = AddProject.Request(fixture.loggedInUser, UUID.randomUUID, "Some Project Name")

      When("run the use case")
      val response = fixture.run(request)

      Then("it should pass the validation")
      response.success.value.result shouldBe a[Project]
    }
  }

  Feature("Add project to storage") {
    Scenario("Add a project without parent") { fixture =>
      Given("user request to add a project without parent project")
      val request = AddProject.Request(
        fixture.loggedInUser,
        UUID.randomUUID,
        "Some Project Name",
        note = Some("Note"),
        status = Project.Inactive
      )

      When("run the use case")
      val response = fixture.run(request)

      Then("it should returned a project with correct data")
      val returnedProject = response.success.value.result
      inside(returnedProject) { case Project(uuid, userUUID, name, note, parentProjectUUID, status, isTrashed, createTime, updateTime) =>
        uuid shouldBe request.uuid
        userUUID shouldBe request.loggedInUser.uuid
        name shouldBe request.name
        note shouldBe request.note
        parentProjectUUID shouldBe None
        status shouldBe request.status
        isTrashed shouldBe false
        createTime shouldBe fixture.generator.currentTime
        updateTime shouldBe fixture.generator.currentTime
      }

      And("store it to storage")
      val projectInStorage = fixture.projectRepo.read.findByUUID(request.uuid)
      projectInStorage.value shouldBe returnedProject

      And("generate correct journal entry")
      response.success.value.journals.changes shouldBe List(
        Change(fixture.generator.randomUUID, None, returnedProject, fixture.generator.currentTime)
      )
    }

    Scenario("Add a project has parent project") { fixture =>
      Given("user already has a project in system")
      val userProject = fixture.createProject(fixture.loggedInUser, "ProjectName")

      And("user request to add a project with parent project point to above project")
      val request = AddProject.Request(
        fixture.loggedInUser, UUID.randomUUID,
        "Some Project Name",
        parentProjectUUID = Some(userProject.uuid)
      )

      When("run the use case")
      val response = fixture.run(request)

      Then("it should returned a project with a parent project")
      val returnedProject = response.success.value.result
      returnedProject.parentProjectUUID shouldBe Some(userProject.uuid)

      And("store it to storage")
      val projectInStorage = fixture.projectRepo.read.findByUUID(request.uuid)
      projectInStorage.value shouldBe returnedProject

      And("generate correct journal entry")
      response.success.value.journals.changes shouldBe List(
        Change(fixture.generator.randomUUID, None, returnedProject, fixture.generator.currentTime)
      )
    }
  }

}
