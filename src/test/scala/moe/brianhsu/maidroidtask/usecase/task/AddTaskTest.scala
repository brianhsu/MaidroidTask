package moe.brianhsu.maidroidtask.usecase.task

import java.time.LocalDateTime
import java.util.UUID

import moe.brianhsu.maidroidtask.entity.{InsertLog, Journal, P1, Task, User}
import moe.brianhsu.maidroidtask.gateway.generator.DynamicDataGenerator
import moe.brianhsu.maidroidtask.gateway.repo.memory.InMemoryTaskRepo
import moe.brianhsu.maidroidtask.usecase.Validations.{Duplicated, FailedValidation, NotFound, Required, ValidationErrors}
import moe.brianhsu.maidroidtask.usecase.{DoNothingUseCase, UseCaseExecutor}
import org.scalactic.Validation
import org.scalatest.{GivenWhenThen, OptionValues, Outcome, TryValues}
import org.scalatest.featurespec.FixtureAnyFeatureSpec
import org.scalatest.matchers.should.Matchers

import scala.util.{Success, Try}

class AddTaskTest extends FixtureAnyFeatureSpec with GivenWhenThen with TryValues with Matchers with OptionValues {

  Feature("Validation before add task to system") {

    info("As a user, I would like the system prevent me to add task ")
    info("if some required information is not provided")

    Scenario("Validation failed because UUID collision") { fixture =>
      Given("we request to add a task with UUID that already in system")
      val request = AddTask.Request(fixture.uuidInSystem, fixture.loggedInUser, "Description")

      When("run the use case")
      val (response, _) = fixture.run(request)

      Then("it shouldn't pass the validation")
      val exception = response.failure.exception
      exception shouldBe a [ValidationErrors]
      exception.asInstanceOf[ValidationErrors].failedValidations shouldBe List(FailedValidation("uuid",Duplicated))
    }

    Scenario("Validation failed because we don't provide description") { fixture =>
      Given("we request to add a task without description")
      val request = AddTask.Request(UUID.randomUUID, fixture.loggedInUser, "")

      When("run the use case")
      val (response, _) = fixture.run(request)

      Then("it shouldn't pass the validation")
      val exception = response.failure.exception
      exception shouldBe a [ValidationErrors]
      exception.asInstanceOf[ValidationErrors].failedValidations shouldBe List(FailedValidation("description", Required))
    }

    Scenario("Validation failed because we provide description that is basically empty") { fixture =>
      Given("we request to add a task that only consist of space, tab, newline")
      val request = AddTask.Request(UUID.randomUUID, fixture.loggedInUser, "    \t   \n  ")

      When("run the use case")
      val (response, _) = fixture.run(request)

      Then("it shouldn't pass the validation")
      val exception = response.failure.exception
      exception shouldBe a [ValidationErrors]
      exception.asInstanceOf[ValidationErrors].failedValidations shouldBe List(FailedValidation("description", Required))
    }

    Scenario("Validation failed because the depended task not exist") { fixture =>
      Given("we request to add a task that depends on a non-exist task UUID")
      val nonExistUUID = UUID.fromString("13482407-9977-40d6-a3e8-7fb73de682c4")
      val taskDependsOn = List(nonExistUUID, fixture.uuidInSystem)
      val request = AddTask.Request(UUID.randomUUID, fixture.loggedInUser, "Description", dependsOn = taskDependsOn)

      When("run the use case")
      val (response, _) = fixture.run(request)

      Then("it shouldn't pass the validation")
      val exception = response.failure.exception
      exception shouldBe a [ValidationErrors]
      exception.asInstanceOf[ValidationErrors].failedValidations shouldBe List(FailedValidation("dependsOn", NotFound))
    }

    Scenario("Validation passed") { fixture =>
      Given("we request to add a task with all fields except project, tags")
      val taskDependsOn = List(fixture.uuidInSystem)
      val request = AddTask.Request(
        UUID.randomUUID, fixture.loggedInUser,
        "Description",
        dependsOn = taskDependsOn,
        note = Some("Note"),
        priority = Some(P1),
        waitUntil = Some(LocalDateTime.parse("2020-07-30T10:11:12")),
        due = Some(LocalDateTime.parse("2020-08-30T10:00:00")),
        scheduled = Some(LocalDateTime.parse("2020-08-11T23:44:45"))
      )

      When("run the use case")
      val (response, _) = fixture.run(request)

      Then("it should pass the validation")
      response.success.value shouldBe a [Task]
    }
  }

  Feature("Add task to system") {
    Scenario("Add task to storage") { fixture =>
      Given("we request to add a task with correct fields")
      val taskDependsOn = List(fixture.uuidInSystem)
      val taskUUID = UUID.randomUUID
      val request = AddTask.Request(
        taskUUID, fixture.loggedInUser,
        "Description",
        dependsOn = taskDependsOn,
        note = Some("Note"),
        priority = Some(P1),
        waitUntil = Some(LocalDateTime.parse("2020-07-30T10:11:12")),
        due = Some(LocalDateTime.parse("2020-08-30T10:00:00")),
        scheduled = Some(LocalDateTime.parse("2020-08-11T23:44:45"))
      )

      When("run the use case")
      val (response, journal) = fixture.run(request)

      Then("the returned task should be stored in our system")
      val taskReturned = response.success.value
      val taskInStorage = fixture.taskRepo.read.findByUUID(taskUUID).value
      taskInStorage shouldBe taskReturned

      And("the returned task should contains correct data")
      taskInStorage.uuid shouldBe taskUUID
      taskInStorage.userUUID shouldBe request.loggedInUser.uuid
      taskInStorage.description shouldBe request.description
      taskInStorage.project shouldBe None
      taskInStorage.tags shouldBe Nil
      taskInStorage.dependsOn shouldBe taskDependsOn
      taskInStorage.note shouldBe Some("Note")
      taskInStorage.priority shouldBe Some(P1)
      taskInStorage.waitUntil shouldBe Some(LocalDateTime.parse("2020-07-30T10:11:12"))
      taskInStorage.due shouldBe Some(LocalDateTime.parse("2020-08-30T10:00:00"))
      taskInStorage.scheduled shouldBe Some(LocalDateTime.parse("2020-08-11T23:44:45"))
      taskInStorage.isDeleted shouldBe false
      taskInStorage.isDone shouldBe false
      taskInStorage.createTime shouldBe fixture.generator.currentTime
      taskInStorage.updateTime shouldBe fixture.generator.currentTime

      And("generate correct journal entry")
      journal should contain theSameElementsInOrderAs List(
        InsertLog(
          request.loggedInUser.uuid,
          taskUUID, taskReturned,
          fixture.generator.currentTime
        )
      )
    }
  }

  override protected def withFixture(test: OneArgTest): Outcome = test(new TestFixture)
  override type FixtureParam = TestFixture

  class FixedTestDataGenerator extends DynamicDataGenerator {
    override def randomUUID: UUID = UUID.fromString("4fe31c87-7130-4d61-9912-3fa6c0c9b7ef")
    override def currentTime: LocalDateTime = LocalDateTime.parse("2020-07-02T09:10:11")
  }

  class TestFixture {

    implicit val generator = new FixedTestDataGenerator
    implicit val useCaseExecutor = new UseCaseExecutor
    implicit val taskRepo = new InMemoryTaskRepo

    val loggedInUser = User(UUID.randomUUID(), "user@example.com", "UserName")
    val uuidInSystem = UUID.fromString("ba2d0314-c049-48cb-99db-b068ceeb4a41")

    taskRepo.write.insert(Task(uuidInSystem, loggedInUser.uuid, "Description"))

    def run(request: AddTask.Request): (Try[Task], List[Journal]) = {
      val useCase = new AddTask(request)
      (useCase.execute(), useCase.journals)
    }
  }
}
