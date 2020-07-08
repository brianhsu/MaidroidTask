package moe.brianhsu.maidroidtask.usecase.project

import java.util.UUID

import moe.brianhsu.maidroidtask.entity.Project
import moe.brianhsu.maidroidtask.usecase.Validations.{AccessDenied, AlreadyTrashed, HasChildren, NotFound}
import moe.brianhsu.maidroidtask.utils.fixture.{BaseFixture, BaseFixtureFeature}

class TrashProjectFixture extends BaseFixture {
  def run(request: TrashProject.Request) = {
    val useCase = new TrashProject(request)
    useCase.execute()
  }
}

class TrashProjectTest extends BaseFixtureFeature[TrashProjectFixture] {
  override protected def createFixture: TrashProjectFixture = new TrashProjectFixture

  Feature("Validation before trash project") {
    Scenario("Trash a project with non-exist UUID") { fixture =>
      Given("user request to trash a project that does not exist")
      val nonExistProjectUUID = UUID.randomUUID
      val request = TrashProject.Request(fixture.loggedInUser, nonExistProjectUUID)

      When("run the use case")
      val response = fixture.run(request)

      Then("it should NOT pass the validation, and yield NotFound error")
      response should containsFailedValidation("uuid", NotFound)
    }

    Scenario("Trash a project that is already trashed") { fixture =>
      Given("user request to trash a project that is already trashed")
      val trashedProject = fixture.createProject(fixture.loggedInUser, "TrashedProject", isTrashed = true)
      val request = TrashProject.Request(fixture.loggedInUser, trashedProject.uuid)

      When("run the use case")
      val response = fixture.run(request)

      Then("it should NOT pass the validation, and yield AlreadyTrashed error")
      response should containsFailedValidation("uuid", AlreadyTrashed)
    }

    Scenario("Trash a project that has child project") { fixture =>
      Given("user request to trash a project that has child project")
      val parentProject = fixture.createProject(fixture.loggedInUser, "ParentProject")
      val childProject = fixture.createProject(fixture.loggedInUser, name = "Child Project", parentProject = Some(parentProject.uuid))
      val request = TrashProject.Request(fixture.loggedInUser, parentProject.uuid)

      When("run the use case")
      val response = fixture.run(request)

      Then("is should NOT pass the validation, and yield HasChildren error")
      response should containsFailedValidation("uuid", HasChildren)
    }

    Scenario("Trash a project that belongs to others") { fixture =>
      Given("user request to trash a project that belongs to others")
      val otherUserProject = fixture.createProject(fixture.otherUser, "OtherUserProject")
      val request = TrashProject.Request(fixture.loggedInUser, otherUserProject.uuid)

      When("run the use case")
      val response = fixture.run(request)

      Then("it should NOT pass the validation and yield AccessDenied error")
      response should containsFailedValidation("uuid", AccessDenied)
    }

    Scenario("Validation passed") { fixture =>
      Given("user request to trash a normal project")
      val userProject = fixture.createProject(fixture.loggedInUser, "MyProject")
      val request = TrashProject.Request(fixture.loggedInUser, userProject.uuid)

      When("run the use case")
      val response = fixture.run(request)

      Then("it should pass the validation")
      response.success.value.result shouldBe a[Project]
    }
  }

  Feature("Trash a project in a cascade trash way") {
    Scenario("There are only 1 tasks") { fixture =>
      Given("a project with only 1 non-trashed task")
      And("user request to trash that project")
      When("run the use case")
      Then("the task should also be trashed")
      And("maintain the project uuid in task entity")
      And("the trashed task is updated in storage")
      And("the project is updated in storage")
      And("generate correct log")
      pending
    }

    Scenario("There are multiple task in that project") { fixture =>
      Given("a project with multiple tasks, including trashed one")
      And("user request to trash that project")
      When("run the use case")
      Then("all tasks should also be trashed")
      And("maintain the project uuid in tasks")
      And("all tasks is updated in storage")
      And("the project is updated in storage")
      And("generate correct log")
      pending
    }

  }
  Feature("Trash a project in oneLevelUp way") {
    Scenario("There are only 1 tasks, no-parent project") { fixture =>
      Given("a project with only 1 non-trashed task")
      And("user request to trash that project")
      When("run the use case")
      Then("the project of that task should be None")
      And("the task is updated in storage")
      And("the project is updated in storage")
      And("generate correct log")
      pending
    }

    Scenario("There are multiple task in that project, no-parent job") { fixture =>
      Given("a project with multiple tasks, including trashed one")
      And("user request to trash that project")
      Then("all tasks should have no assigned project")
      And("all tasks is updated in storage")
      And("the project is updated in storage")
      And("generate correct log")
      pending
    }
    Scenario("There are only 1 tasks, with parent project") { fixture =>
      Given("a project with parent project, and contains only 1 tasks")
      And("user request to trash that project")
      When("run the use case")
      Then("the project of that task should be parent-project")
      And("the task is updated in storage")
      And("the project is updated in storage")
      And("generate correct log")
      pending
    }

    Scenario("There are multiple task in that project, with parent job") { fixture =>
      Given("a project with parent project, and contains multiple tasks, including trashed one")
      And("user request to trash that project")
      Then("all tasks should have project uuid assigned to parent project")
      And("all tasks is updated in storage")
      And("the project is updated in storage")
      And("generate correct log")
      pending
    }

  }


}
