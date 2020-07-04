package moe.brianhsu.maidroidtask.usecase.task

import java.time.{LocalDate, LocalDateTime, LocalTime}
import java.util.UUID

import moe.brianhsu.maidroidtask.entity.{InsertLog, Journal, P1, ScheduledAt, Task, User}
import moe.brianhsu.maidroidtask.usecase.Validations.{Duplicated, FailedValidation, NotFound, Required, ValidationErrors}
import moe.brianhsu.maidroidtask.usecase.fixture.{BaseFixture, BaseFixtureFeature}

import scala.util.Try

class AddTaskFixture extends BaseFixture {
  val uuidInSystem = UUID.fromString("ba2d0314-c049-48cb-99db-b068ceeb4a41")

  taskRepo.write.insert(Task(uuidInSystem, loggedInUser.uuid, "Description"))

  def run(request: AddTask.Request): (Try[Task], List[Journal]) = {
    val useCase = new AddTask(request)
    (useCase.execute(), useCase.journals)
  }
}

class AddTaskTest extends BaseFixtureFeature[AddTaskFixture] {

  override def createFixture = new AddTaskFixture

  Feature("Validation before add task to system") {

    info("As a user, I would like the system prevent me to add task ")
    info("if some required information is not provided")

    Scenario("Validation failed because UUID collision") { fixture =>
      Given("we request to add a task with UUID that already in system")
      val request = AddTask.Request(fixture.loggedInUser, fixture.uuidInSystem, "Description")

      When("run the use case")
      val (response, _) = fixture.run(request)

      Then("it shouldn't pass the validation")
      val exception = response.failure.exception
      exception shouldBe a [ValidationErrors]
      exception.asInstanceOf[ValidationErrors].failedValidations shouldBe List(FailedValidation("uuid",Duplicated))
    }

    Scenario("Validation failed because we don't provide description") { fixture =>
      Given("we request to add a task without description")
      val request = AddTask.Request(fixture.loggedInUser, UUID.randomUUID, "")

      When("run the use case")
      val (response, _) = fixture.run(request)

      Then("it shouldn't pass the validation")
      val exception = response.failure.exception
      exception shouldBe a [ValidationErrors]
      exception.asInstanceOf[ValidationErrors].failedValidations shouldBe List(FailedValidation("description", Required))
    }

    Scenario("Validation failed because we provide description that is basically empty") { fixture =>
      Given("we request to add a task that only consist of space, tab, newline")
      val request = AddTask.Request(fixture.loggedInUser, UUID.randomUUID, "    \t   \n  ")

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
      val request = AddTask.Request(fixture.loggedInUser, UUID.randomUUID, "Description", dependsOn = taskDependsOn)

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
      val request = AddTask.Request(fixture.loggedInUser, UUID.randomUUID, "Description", note = Some("Note"), dependsOn = taskDependsOn, priority = Some(P1), waitUntil = Some(LocalDateTime.parse("2020-07-30T10:11:12")), due = Some(LocalDateTime.parse("2020-08-30T10:00:00")), scheduledAt = Some(
                ScheduledAt(
                  LocalDate.parse("2020-08-11"),
                  Some(LocalTime.parse("23:44:45"))
                )
              ))

      When("run the use case")
      val (response, _) = fixture.run(request)

      Then("it should pass the validation")
      response.success.value shouldBe a [Task]
    }
  }

  Feature("Add task to system") {
    info("As a user, I would like to add a single task to system.")
    Scenario("Add task to storage") { fixture =>
      Given("we request to add a task with correct fields")
      val taskDependsOn = List(fixture.uuidInSystem)
      val taskUUID = UUID.randomUUID
      val request = AddTask.Request(fixture.loggedInUser, taskUUID, "Description", note = Some("Note"), dependsOn = taskDependsOn, priority = Some(P1), waitUntil = Some(LocalDateTime.parse("2020-07-30T10:11:12")), due = Some(LocalDateTime.parse("2020-08-30T10:00:00")), scheduledAt = Some(
                ScheduledAt(
                  LocalDate.parse("2020-08-11"),
                  Some(LocalTime.parse("23:44:45"))
                )
              ))

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
      taskInStorage.note.value shouldBe "Note"
      taskInStorage.priority.value shouldBe P1
      taskInStorage.waitUntil.value shouldBe LocalDateTime.parse("2020-07-30T10:11:12")
      taskInStorage.due.value shouldBe LocalDateTime.parse("2020-08-30T10:00:00")
      taskInStorage.scheduledAt.value.date shouldBe LocalDate.parse("2020-08-11")
      taskInStorage.scheduledAt.value.time.value shouldBe LocalTime.parse("23:44:45")
      taskInStorage.isTrashed shouldBe false
      taskInStorage.isDone shouldBe false
      taskInStorage.createTime shouldBe fixture.generator.currentTime
      taskInStorage.updateTime shouldBe fixture.generator.currentTime

      And("generate correct journal entry")
      journal should contain theSameElementsInOrderAs List(
        InsertLog(
          fixture.generator.randomUUID,
          request.loggedInUser.uuid,
          taskUUID, taskReturned,
          fixture.generator.currentTime
        )
      )
    }
  }
}
