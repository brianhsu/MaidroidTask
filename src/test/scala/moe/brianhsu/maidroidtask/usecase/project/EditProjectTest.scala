package moe.brianhsu.maidroidtask.usecase.project

import java.time.LocalDateTime
import java.util.UUID

import moe.brianhsu.maidroidtask.entity.{Change, Project}
import moe.brianhsu.maidroidtask.usecase.Validations.{AccessDenied, AlreadyTrashed, Duplicated, NotFound}
import moe.brianhsu.maidroidtask.usecase.base.types.ResultHolder
import moe.brianhsu.maidroidtask.utils.fixture.{BaseFixture, BaseFixtureFeature}

class EditProjectFixture extends BaseFixture {
  val userProject = createProject(loggedInUser, "Some Project")
  val otherUserProject = createProject(otherUser, "OtherUserProject")

  def run(request: EditProject.Request): ResultHolder[Project] = {
    val useCase = new EditProject(request)
    useCase.execute()
  }
}

class EditProjectTest extends BaseFixtureFeature[EditProjectFixture] {
  override protected def createFixture: EditProjectFixture = new EditProjectFixture

  Feature("Validation before edit project") {
    Scenario("Edit a project UUID that does not exist") { fixture =>
      Given("user request to edit a project with a UUID that is not exist")
      val nonExistUUID = UUID.randomUUID
      val request = EditProject.Request(fixture.loggedInUser, nonExistUUID, name = Some("New Project Name"))

      When("run the use case")
      val response = fixture.run(request)

      Then("it should NOT pass the validation and yield NotFound error")
      response should containsFailedValidation("uuid", NotFound)
    }

    Scenario("Edit a project that belongs to other use") { fixture =>
      Given("user request to edit a project that belongs to others")
      val request = EditProject.Request(fixture.loggedInUser, fixture.otherUserProject.uuid, name = Some("New Project Name"))

      When("run the use case")
      val response = fixture.run(request)

      Then("it should NOT pass the validation and yield AccessDenied error")
      response should containsFailedValidation("uuid", AccessDenied)
    }

    Scenario("Edit a project that has duplicate name for same user") { fixture =>
      Given("user request to edit a project and new name is duplicated with user's other project")
      val request = EditProject.Request(fixture.loggedInUser, fixture.userProject.uuid, name = Some("Some Project"))

      When("run the use case")
      val response = fixture.run(request)

      Then("it should NOT pass the validation and yield Duplicate error")
      response should containsFailedValidation("name", Duplicated)
    }

    Scenario("Edit a project that child-project does not exist") { fixture =>
      Given("user request to edit a project that new child project is not exist")

      val nonExistChildProject = UUID.randomUUID
      val request = EditProject.Request(
        fixture.loggedInUser,
        fixture.userProject.uuid,
        name = Some("Updated Project"),
        parentProjectUUID = Some(Some(nonExistChildProject))
      )

      When("run the use case")
      val response = fixture.run(request)

      Then("it should NOT pass the validation and yield NotFound error")
      response should containsFailedValidation("parentProjectUUID", NotFound)
    }

    Scenario("Edit a project that child-project is belongs to other user") { fixture =>
      Given("user request to edit a project that new child project is belongs to other user")
      val request = EditProject.Request(
        fixture.loggedInUser,
        fixture.userProject.uuid,
        name = Some("Updated Project"),
        parentProjectUUID = Some(Some(fixture.otherUserProject.uuid))
      )

      When("run the use case")
      val response = fixture.run(request)

      Then("it should NOT pass the validation and yield AccessDenied error")
      response should containsFailedValidation("parentProjectUUID", AccessDenied)
    }

    Scenario("Edit a project that is already trashed") { fixture =>
      Given("user request to edit a project that is already trashed")
      val trashedProject = fixture.createProject(fixture.loggedInUser, "TrashedProject", isTrashed = true)
      val request = EditProject.Request(
        fixture.loggedInUser,
        trashedProject.uuid,
        name = Some("NewProjectName")
      )

      When("run the use case")
      val response = fixture.run(request)

      Then("it should NOT pass the validation and yield AlreadyTrashed")
      response should containsFailedValidation("uuid", AlreadyTrashed)
    }

    Scenario("Edit a project that parent project is already trashed") { fixture =>
      Given("user request to edit a project with trashed parent project")
      val trashedProject = fixture.createProject(fixture.loggedInUser, "TrashedProject", isTrashed = true)
      val project = fixture.createProject(fixture.loggedInUser, "Project")
      val request = EditProject.Request(
        fixture.loggedInUser,
        project.uuid,
        parentProjectUUID = Some(Some(trashedProject.uuid))
      )

      When("run the use case")
      val response = fixture.run(request)

      Then("it should NOT pass the validation and yield AlreadyTrashed")
      response should containsFailedValidation("parentProjectUUID", AlreadyTrashed)
    }

    Scenario("Edit a project that name is duplicated with trashed project") { fixture =>
      Given("user request to edit a project")
      val trashedProject = fixture.createProject(fixture.loggedInUser, "SomeTrashedProject", isTrashed = true)

      And("new project name is duplicated with trashed project")
      val request = EditProject.Request(
        fixture.loggedInUser,
        fixture.userProject.uuid,
        name = Some(trashedProject.name)
      )

      When("run the use case")
      val response = fixture.run(request)

      Then("it should pass the validation")
      response.success.value.result shouldBe a[Project]
    }

    Scenario("Edit a project that name is duplicate with other user's project") { fixture =>
      Given("user request to edit a project")
      val request = EditProject.Request(
        fixture.loggedInUser,
        fixture.userProject.uuid,
        name = Some("SomeNewName")
      )

      And("new project name is duplicate with other user's project")
      fixture.createProject(fixture.otherUser, "SomeNewName")

      When("run the use case")
      val response = fixture.run(request)

      When("it should pass the validation")
      response.success.value.result shouldBe a[Project]
    }
  }

  Feature("Store edited project in storage") {
    Scenario("Edit a project with all fields updated") { fixture =>
      Given("a exist project in system")
      val newParentProject = fixture.createProject(fixture.loggedInUser, "NewParent")

      And("user request to edit that project with new attributes")
      val request = EditProject.Request(
        fixture.loggedInUser,
        fixture.userProject.uuid,
        name = Some("SomeNewName"),
        note = Some(Some("SomeNewNote")),
        parentProjectUUID = Some(Some(newParentProject.uuid)),
        status = Some(Project.Inactive)
      )

      When("run the request")
      val response = fixture.run(request)

      Then("it should return edited project")
      val returnedProject = response.success.value.result
      inside(returnedProject) { case Project(uuid, userUUID, name, note, parentProjectUUID, status, isTrashed, createTime, updateTime) =>
        uuid shouldBe request.uuid
        userUUID shouldBe fixture.loggedInUser.uuid
        name shouldBe "SomeNewName"
        note shouldBe Some("SomeNewNote")
        parentProjectUUID shouldBe Some(newParentProject.uuid)
        status shouldBe Project.Inactive
      }
      And("store it in storage")
      val projectInStorage = fixture.projectRepo.read.findByUUID(request.uuid).value
      projectInStorage shouldBe returnedProject

      And("generate correct journal entry")
      response.success.value.journals.changes shouldBe List(
        Change(fixture.generator.randomUUID, Some(fixture.userProject), projectInStorage, fixture.generator.currentTime)
      )
    }

    Scenario("Edit a project with some fields updated") { fixture =>
      Given("a exist project in system")
      val myProject = fixture.projectRepo.write.insert(
        Project(
          UUID.randomUUID, fixture.loggedInUser.uuid,
          "Some Project",
          note = Some("Some Note"),
          parentProjectUUID = Some(fixture.userProject.uuid),
          status = Project.Done,
          isTrashed = false,
          createTime = LocalDateTime.now,
          updateTime = LocalDateTime.now
        )
      )

      And("user request to edit project that only some fields are updated")
      val request = EditProject.Request(fixture.loggedInUser, myProject.uuid, note = Some(Some("SomeNewNote")))

      When("run the request")
      val response = fixture.run(request)

      Then("it should return edited project and only the requested fields are changed")
      val returnedProject = response.success.value.result
      inside(returnedProject) { case Project(uuid, userUUID, name, note, parentProjectUUID, status, isTrashed, createTime, updateTime) =>
        uuid shouldBe request.uuid
        userUUID shouldBe fixture.loggedInUser.uuid
        name shouldBe myProject.name
        note shouldBe Some("SomeNewNote")
        parentProjectUUID shouldBe myProject.parentProjectUUID
        status shouldBe myProject.status
        isTrashed shouldBe myProject.isTrashed
      }

      And("store it in storage")
      val projectInStorage = fixture.projectRepo.read.findByUUID(request.uuid).value
      projectInStorage shouldBe returnedProject

      And("generate correct journal entry")
      response.success.value.journals.changes shouldBe List(
        Change(fixture.generator.randomUUID, Some(myProject), projectInStorage, fixture.generator.currentTime)
      )

    }

  }
}
