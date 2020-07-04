package moe.brianhsu.maidroidtask.usecase.task

import java.util.UUID

import moe.brianhsu.maidroidtask.entity.{Journal, Task}
import moe.brianhsu.maidroidtask.usecase.Validations.{Duplicated, FailedValidation, NotFound, ValidationErrors}
import moe.brianhsu.maidroidtask.usecase.fixture.{BaseFixture, BaseFixtureFeature}

import scala.util.Try

class TrashTaskFixture extends BaseFixture {
  def run(request: TrashTask.Request): (Try[Task], List[Journal]) = {
    val useCase = new TrashTask(request)
    (useCase.execute(), useCase.journals)
  }

}

class TrashTaskTest extends BaseFixtureFeature[TrashTaskFixture] {

  override protected def createFixture: TrashTaskFixture = new TrashTaskFixture

  Feature("Validation before trash the task") {
    info("As a system administrator, I will like the system prevent")
    info("some trashed task that not exist or not belong to them.")

    Scenario("Delete non-exist task") { fixture =>
      Given("user request to trash a task does not exist")
      val uuidNotExist = UUID.fromString("a927508f-edfe-45f7-bdf8-ff1ef394673c")
      val request = TrashTask.Request(fixture.loggedInUser, uuidNotExist)

      When("run the use case")
      val (response, _) = fixture.run(request)

      Then("it should NOT pass the validation")
      val exception = response.failure.exception
      exception shouldBe a[ValidationErrors]
      exception.asInstanceOf[ValidationErrors].failedValidations shouldBe List(FailedValidation("uuid", NotFound))
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
