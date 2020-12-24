package moe.brianhsu.maidroidtask.usecase.project

import java.util.UUID

import moe.brianhsu.maidroidtask.entity.{Change, Journal, Project}
import moe.brianhsu.maidroidtask.usecase.Validations.{AccessDenied, NotFound, NotTrashed, ParentIsTrashed}
import moe.brianhsu.maidroidtask.usecase.base.types.ResultHolder
import moe.brianhsu.maidroidtask.utils.fixture.{BaseFixture, BaseFixtureFeature}

class UnTrashProjectFixture extends BaseFixture {
  def run(request: UnTrashProject.Request): ResultHolder[Project] = {
    val useCase = new UnTrashProject(request)
    useCase.execute()
  }
}
class UnTrashProjectTest extends BaseFixtureFeature[UnTrashProjectFixture]{
  override protected def createFixture: UnTrashProjectFixture = new UnTrashProjectFixture

  Feature("Validation before untrash") {
    Scenario("The project uuid is not exist") { fixture =>
      Given("user request to un trash a project is not exist")
      val nonExistProjectUUID = UUID.randomUUID
      val request = UnTrashProject.Request(fixture.loggedInUser, nonExistProjectUUID)

      When("run the use case")
      val response = fixture.run(request)

      Then("it should NOT pass validation and yield NotFound error")
      response should containsFailedValidation("uuid", NotFound)
    }

    Scenario("The project is belongs to others") { fixture =>
      Given("user request to un trash a project belongs to others")
      val project = fixture.createProject(fixture.otherUser, "OtherUserProject", isTrashed = true)
      val request = UnTrashProject.Request(fixture.loggedInUser, project.uuid)

      When("run the use case")
      val response = fixture.run(request)

      Then("it should NOT pass validation and yield AccessDeined error")
      response should containsFailedValidation("uuid", AccessDenied)
    }

    Scenario("The project is no trashed") { fixture =>
      Given("user request to un trash a non-trashed project")
      val project = fixture.createProject(fixture.loggedInUser, "Project")
      val request = UnTrashProject.Request(fixture.loggedInUser, project.uuid)

      When("run the use case")
      val response = fixture.run(request)

      Then("it should NOT pass validation and yield NotTrashed error")
      response should containsFailedValidation("uuid", NotTrashed)
    }

    Scenario("The parent project is still trashed") { fixture =>
      Given("A trashed project with trashed parent project")
      val parentProject = fixture.createProject(fixture.loggedInUser, "Parent", isTrashed = true)
      val project = fixture.createProject(fixture.loggedInUser, "Project", Some(parentProject.uuid), isTrashed = true)

      And("user request to un trash that project")
      val request = UnTrashProject.Request(fixture.loggedInUser, project.uuid)

      When("run the use case")
      val response = fixture.run(request)

      Then("it should NOT pass validation and yield ParentIsTrashed error")
      response should containsFailedValidation("uuid", ParentIsTrashed)
    }

  }

  Feature("Update the status and store it to storage") {
    Scenario("Un trash a project without parent project") { fixture =>
      Given("user request to un trash a project without parent project")
      val project = fixture.createProject(fixture.loggedInUser, "Project", isTrashed = true)
      val unTrashRequest = UnTrashProject.Request(fixture.loggedInUser, project.uuid)

      When("run the use case")
      val response = fixture.run(unTrashRequest)

      Then("it should return untrashed project")
      val unTrashdProject = response.success.value.result
      unTrashdProject.isTrashed shouldBe false
      unTrashdProject.updateTime shouldBe fixture.generator.currentTime

      And("stored in storage")
      val projectInStorage = fixture.projectRepo.read.findByUUID(unTrashdProject.uuid).value
      projectInStorage shouldBe unTrashdProject

      And("generate the correct log")
      inside(response.success.value.journals) { case Journal(journalUUID, userUUID, request, changes, timestamp) =>
        journalUUID shouldBe fixture.generator.randomUUID
        userUUID shouldBe fixture.loggedInUser.uuid
        request shouldBe unTrashRequest
        changes should contain theSameElementsAs List(
          Change(fixture.generator.randomUUID, Some(project), projectInStorage, fixture.generator.currentTime)
        )
        timestamp shouldBe fixture.generator.currentTime
      }
    }

    Scenario("Un trash a project with parent project") { fixture =>
      Given("user request to un trash a project has parent project")
      val parentProject = fixture.createProject(fixture.loggedInUser, "Parent")
      val project = fixture.createProject(fixture.loggedInUser, "Project", Some(parentProject.uuid), isTrashed = true)
      val unTrashRequest = UnTrashProject.Request(fixture.loggedInUser, project.uuid)

      When("run the use case")
      val response = fixture.run(unTrashRequest)

      Then("it should return untrashed project")
      val unTrashdProject = response.success.value.result
      unTrashdProject.isTrashed shouldBe false
      unTrashdProject.updateTime shouldBe fixture.generator.currentTime

      And("stored in storage")
      val projectInStorage = fixture.projectRepo.read.findByUUID(unTrashdProject.uuid).value
      projectInStorage shouldBe unTrashdProject

      And("generate the correct log")
      inside(response.success.value.journals) { case Journal(journalUUID, userUUID, request, changes, timestamp) =>
        journalUUID shouldBe fixture.generator.randomUUID
        userUUID shouldBe fixture.loggedInUser.uuid
        request shouldBe unTrashRequest
        changes should contain theSameElementsAs List(
          Change(fixture.generator.randomUUID, Some(project), projectInStorage, fixture.generator.currentTime)
        )
        timestamp shouldBe fixture.generator.currentTime
      }
    }
  }
}
