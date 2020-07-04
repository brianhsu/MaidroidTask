package moe.brianhsu.maidroidtask.usecase.task

import java.time.{LocalDate, LocalDateTime, LocalTime}
import java.util.UUID

import moe.brianhsu.maidroidtask.entity.{Journal, P1, ScheduledAt, Task, UpdateLog}
import moe.brianhsu.maidroidtask.usecase.Validations.{AccessDenied, FailedValidation, NotFound, Required, ValidationErrors}
import moe.brianhsu.maidroidtask.utils.fixture.{BaseFixture, BaseFixtureFeature}

import scala.util.Try

class EditTaskFixture extends BaseFixture {

  val task1UUID = UUID.fromString("65d94f14-b00d-4060-8c8f-30a64aca8f08")
  val task2UUID = UUID.fromString("a641ec70-6402-4e8b-b467-50e73aa49bc1")
  val task3UUID = UUID.fromString("83537092-07cb-4f85-afe1-cf6188edbf00")

  val otherUserTaskUUID = UUID.fromString("d6c0bab2-dd90-462f-bb6f-682a97405e64")

  private val fixtureCreateTime = LocalDateTime.parse("2020-07-30T11:12:13")
  val task1 = taskRepo.write.insert(Task(task1UUID, loggedInUser.uuid, "SomeTask 1", createTime = fixtureCreateTime, updateTime = fixtureCreateTime))
  val task2 = taskRepo.write.insert(Task(task2UUID, loggedInUser.uuid, "SomeTask 2", createTime = fixtureCreateTime, updateTime = fixtureCreateTime))
  val task3 = taskRepo.write.insert(
    Task(
      task3UUID, loggedInUser.uuid, "SomeTask 3",
      note = Some("note"),
      project = Some(UUID.randomUUID()),
      tags = List(UUID.randomUUID(), UUID.randomUUID()),
      dependsOn = List(task2.uuid),
      priority = Some(P1),
      waitUntil = Some(LocalDateTime.parse("2020-09-30T11:11:11")),
      due = Some(LocalDateTime.parse("2030-01-12T12:33:11")),
      scheduledAt = Some(ScheduledAt(LocalDate.parse("2020-01-11"), None)),
      createTime = fixtureCreateTime,
      updateTime = fixtureCreateTime
    )
  )

  val otherUserTask = taskRepo.write.insert(
    Task(
      otherUserTaskUUID, otherUser.uuid, "OtherUserTask",
      createTime = fixtureCreateTime, updateTime = fixtureCreateTime
    )
  )

  def run(request: EditTask.Request): (Try[Task], List[Journal]) = {
    val useCase = new EditTask(request)
    (useCase.execute(), useCase.journals)
  }


}
class EditTaskTest extends BaseFixtureFeature[EditTaskFixture] {
  override protected def createFixture: EditTaskFixture = new EditTaskFixture

  Feature("Validation before edit") {
    Scenario("Edit task that does not exist") { fixture =>
      Given("user request to edit a task does not exist")
      val nonExistUUID = UUID.fromString("cbf3408d-68ec-407a-bd87-97e14c2a41e7")
      val request = EditTask.Request(fixture.loggedInUser, nonExistUUID, Some("SomeDesc"))

      When("run the use case")
      val (response, _) = fixture.run(request)

      Then("it should NOT pass the validation and yield NotFound error")
      val exception = response.failure.exception
      exception shouldBe a[ValidationErrors]
      exception.asInstanceOf[ValidationErrors].failedValidations shouldBe List(FailedValidation("uuid", NotFound))
    }

    Scenario("Edit task that belong to other user") { fixture =>
      Given("user request to edit a task belongs to other user")
      val request = EditTask.Request(fixture.loggedInUser, fixture.otherUserTaskUUID, Some("SomeDesc"))

      When("run the use case")
      val (response, _) = fixture.run(request)

      Then("it should NOT pass the validation and yield AccessDenied error")
      val exception = response.failure.exception
      exception shouldBe a[ValidationErrors]
      exception.asInstanceOf[ValidationErrors].failedValidations shouldBe List(FailedValidation("uuid", AccessDenied))
    }

    Scenario("Edit task that new task depends on non-exist task") { fixture =>
      Given("user request to edit a task and make it depends on non-exist task")
      val nonExistUUID = UUID.fromString("286b5649-049d-476a-9ef2-38de31e86c5b")
      val request = EditTask.Request(fixture.loggedInUser, fixture.task1UUID, dependsOn = Some(List(fixture.task2UUID, nonExistUUID)))

      When("run the use case")
      val (response, _) = fixture.run(request)

      Then("it should NOT pass the validation, and yield NotFound error")
      val exception = response.failure.exception
      exception shouldBe a[ValidationErrors]
      exception.asInstanceOf[ValidationErrors].failedValidations shouldBe List(FailedValidation("dependsOn", NotFound))
    }

    Scenario("Edit task that title is empty") { fixture =>
      Given("user request to edit a task, and new title only consist of empty characters")
      val request = EditTask.Request(fixture.loggedInUser, fixture.task1UUID, Some("   \t \n   "))

      When("run the use case")
      val (response, _) = fixture.run(request)

      Then("it should NOT pass the validation and yield Required error")
      val exception = response.failure.exception
      exception shouldBe a[ValidationErrors]
      exception.asInstanceOf[ValidationErrors].failedValidations shouldBe List(FailedValidation("description", Required))
    }
  }

  Feature("Update task in system") {
    Scenario("Update all fields inside a task that belongs to logged in user") { fixture =>
      Given("user request to edit a task belongs to him or her")
      val taskDependsOn = List(fixture.task2UUID)
      val request = EditTask.Request(
        fixture.loggedInUser, fixture.task1UUID,
        description = Some("EditedDescription"),
        note = Some(Some("Note")),
        dependsOn = Some(taskDependsOn),
        priority = Some(Some(P1)),
        waitUntil = Some(Some(LocalDateTime.parse("2020-07-30T10:11:12"))),
        due = Some(Some(LocalDateTime.parse("2020-08-30T10:00:00"))),
        scheduledAt = Some(
          Some(
            ScheduledAt(
              LocalDate.parse("2020-08-11"),
              Some(LocalTime.parse("23:44:45"))
            )
          )
        ),
        isDone = Some(true)
      )

      When("run the use case")
      val (response, journals) = fixture.run(request)

      Then("the task in storage should be updated with new status")
      val expectedTask = Task(
        fixture.task1UUID, fixture.loggedInUser.uuid,
        description = "EditedDescription",
        note = Some("Note"),
        dependsOn = taskDependsOn,
        priority = Some(P1),
        waitUntil = Some(LocalDateTime.parse("2020-07-30T10:11:12")),
        due = Some(LocalDateTime.parse("2020-08-30T10:00:00")),
        scheduledAt = Some(
          ScheduledAt(
            LocalDate.parse("2020-08-11"),
            Some(LocalTime.parse("23:44:45"))
          )
        ),
        isDone = true,
        createTime = fixture.task1.createTime,
        updateTime = fixture.generator.currentTime
      )

      isSameTask(response.success.value, expectedTask)

      And("it should also updated to storage")
      val taskInStorage = fixture.taskRepo.read.findByUUID(expectedTask.uuid)
      isSameTask(taskInStorage.value, expectedTask)

      And("the journals should have correct entries")
      journals shouldBe List(
        UpdateLog(
          fixture.generator.randomUUID, request.loggedInUser.uuid,
          request.uuid, expectedTask, fixture.generator.currentTime
        )
      )
    }

    Scenario("Only update some fields") { fixture =>
      Given("user request to edit a task belongs to him or her")
      val request = EditTask.Request(
        fixture.loggedInUser, fixture.task3UUID,
        description = Some("NewDescription"),
      )

      When("run the use case")
      val (response, journals) = fixture.run(request)

      Then("the task in storage should only has description updated")
      val expectedTask = Task(
        fixture.task3UUID, fixture.loggedInUser.uuid,
        description = "NewDescription",
        note = fixture.task3.note,
        project = fixture.task3.project,
        tags = fixture.task3.tags,
        dependsOn = fixture.task3.dependsOn,
        priority = fixture.task3.priority,
        waitUntil = fixture.task3.waitUntil,
        due = fixture.task3.due,
        scheduledAt = fixture.task3.scheduledAt,
        isDone = fixture.task3.isDone,
        createTime = fixture.task3.createTime,
        updateTime = fixture.generator.currentTime
      )

      isSameTask(response.success.value, expectedTask)

      And("it should also updated to storage")
      val taskInStorage = fixture.taskRepo.read.findByUUID(expectedTask.uuid)
      isSameTask(taskInStorage.value, expectedTask)

      And("the journals should have correct entries")
      journals shouldBe List(
        UpdateLog(
          fixture.generator.randomUUID, request.loggedInUser.uuid,
          request.uuid, expectedTask, fixture.generator.currentTime
        )
      )
    }

  }

}
