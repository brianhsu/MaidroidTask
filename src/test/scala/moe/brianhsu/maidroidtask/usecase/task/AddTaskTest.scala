package moe.brianhsu.maidroidtask.usecase.task

import java.time.{LocalDate, LocalDateTime, LocalTime}
import java.util.UUID

import moe.brianhsu.maidroidtask.entity.{Change, ScheduledAt, Tag, Task}
import moe.brianhsu.maidroidtask.usecase.UseCaseExecutorResult
import moe.brianhsu.maidroidtask.usecase.Validations.{AccessDenied, Duplicated, NotFound, Required}
import moe.brianhsu.maidroidtask.usecase.types.ResultHolder
import moe.brianhsu.maidroidtask.utils.fixture.{BaseFixture, BaseFixtureFeature}

class AddTaskFixture extends BaseFixture {

  private val fixtureCreateTime = LocalDateTime.parse("2020-07-30T11:12:13")

  val userTag = tagRepo.write.insert(Tag(UUID.randomUUID, loggedInUser.uuid, "ExistTag", None, isTrashed = false, generator.currentTime, generator.currentTime))
  val otherUserTag = tagRepo.write.insert(Tag(UUID.randomUUID, otherUser.uuid, "OtherUserTag", None, isTrashed = false, generator.currentTime, generator.currentTime))

  val userTask = taskRepo.write.insert(
    Task(
      UUID.randomUUID, loggedInUser.uuid, "Description",
      createTime = fixtureCreateTime,
      updateTime = fixtureCreateTime
    )
  )

  def run(request: AddTask.Request): ResultHolder[Task] = {
    val useCase = new AddTask(request)
    useCase.execute()
  }
}

class AddTaskTest extends BaseFixtureFeature[AddTaskFixture] {

  override def createFixture = new AddTaskFixture

  Feature("Validation before add task to system") {

    info("As a user, I would like the system prevent me to add task ")
    info("if some required information is not provided")

    Scenario("Validation failed because UUID collision") { fixture =>
      Given("we request to add a task with UUID that already in system")
      val request = AddTask.Request(fixture.loggedInUser, fixture.userTask.uuid, "Description")

      When("run the use case")
      val response = fixture.run(request)

      Then("it shouldn't pass the validation")
      response should containsFailedValidation("uuid",Duplicated)
    }

    Scenario("Validation failed because we don't provide description") { fixture =>
      Given("we request to add a task without description")
      val request = AddTask.Request(fixture.loggedInUser, UUID.randomUUID, "")

      When("run the use case")
      val response = fixture.run(request)

      Then("it shouldn't pass the validation")
      response should containsFailedValidation("description", Required)
    }

    Scenario("Some tags UUID no exist") { fixture =>
      Given("user request to add a task with a non-exist tag UUID")
      val nonExistTagUUID = UUID.randomUUID()
      val tagsList = List(fixture.userTag.uuid, nonExistTagUUID)
      val request = AddTask.Request(fixture.loggedInUser, UUID.randomUUID, "Task", tags = tagsList)

      When("run the use case")
      val response = fixture.run(request)

      Then("it shouldn't pass the  and yield NotFound error")
      response should containsFailedValidation("tags", NotFound)
    }

    Scenario("Some tags UUID belongs to others") { fixture =>
      Given("user request to add a task with a tag UUID belongs to others")
      val tagsList = List(fixture.userTag.uuid, fixture.otherUserTag.uuid)
      val request = AddTask.Request(fixture.loggedInUser, UUID.randomUUID, "Task", tags = tagsList)

      When("run the use case")
      val response = fixture.run(request)

      Then("it shouldn't pass the  and yield NotFound error")
      response should containsFailedValidation("tags", AccessDenied)
    }

    Scenario("Validation failed because we provide description that is basically empty") { fixture =>
      Given("we request to add a task that only consist of space, tab, newline")
      val request = AddTask.Request(fixture.loggedInUser, UUID.randomUUID, "    \t   \n  ")

      When("run the use case")
      val response = fixture.run(request)

      Then("it shouldn't pass the validation")
      response should containsFailedValidation("description", Required)
    }

    Scenario("Validation failed because the depended task not exist") { fixture =>
      Given("we request to add a task that depends on a non-exist task UUID")
      val nonExistUUID = UUID.fromString("13482407-9977-40d6-a3e8-7fb73de682c4")
      val taskDependsOn = List(nonExistUUID, fixture.userTask.uuid)
      val request = AddTask.Request(fixture.loggedInUser, UUID.randomUUID, "Description", dependsOn = taskDependsOn)

      When("run the use case")
      val response = fixture.run(request)

      Then("it shouldn't pass the validation")
      response should containsFailedValidation("dependsOn", NotFound)
    }

    Scenario("Validation passed") { fixture =>
      Given("we request to add a task with all fields except project, tags")
      val taskDependsOn = List(fixture.userTask.uuid)
      val tagsList = List(fixture.userTag.uuid)
      val request = AddTask.Request(
        fixture.loggedInUser,
        UUID.randomUUID,
        "Description",
        note = Some("Note"),
        dependsOn = taskDependsOn,
        waitUntil = Some(LocalDateTime.parse("2020-07-30T10:11:12")),
        due = Some(LocalDateTime.parse("2020-08-30T10:00:00")),
        tags = tagsList,
        scheduledAt = Some(
          ScheduledAt(
            LocalDate.parse("2020-08-11"),
            Some(LocalTime.parse("23:44:45"))
          )
        )
      )

      When("run the use case")
      val response = fixture.run(request)

      Then("it should pass the validation")
      response.success.value.result shouldBe a [Task]
    }
  }

  Feature("Add task to system") {
    info("As a user, I would like to add a single task to system.")
    Scenario("Add task to storage") { fixture =>
      Given("we request to add a task with correct fields")
      val taskDependsOn = List(fixture.userTask.uuid)
      val taskUUID = UUID.randomUUID
      val tagsList = List(fixture.userTag.uuid)
      val request = AddTask.Request(
        fixture.loggedInUser, taskUUID, "Description",
        note = Some("Note"),
        dependsOn = taskDependsOn,
        tags = tagsList,
        waitUntil = Some(LocalDateTime.parse("2020-07-30T10:11:12")),
        due = Some(LocalDateTime.parse("2020-08-30T10:00:00")),
        scheduledAt = Some(
          ScheduledAt(
            LocalDate.parse("2020-08-11"),
            Some(LocalTime.parse("23:44:45"))
          )
        ),
        isDone = true
      )

      When("run the use case")
      val response = fixture.run(request)

      Then("the returned task should be stored in our system")
      val taskReturned = response.success.value.result
      val taskInStorage = fixture.taskRepo.read.findByUUID(taskUUID).value
      taskInStorage shouldBe taskReturned

      And("the returned task should contains correct data")
      val expectedTask = Task(
        taskUUID, request.loggedInUser.uuid,
        description = request.description,
        note = request.note,
        project = request.project,
        tags = request.tags,
        dependsOn = request.dependsOn,
        waitUntil = request.waitUntil,
        due = request.due,
        scheduledAt = request.scheduledAt,
        isDone = true,
        isTrashed = false,
        createTime = fixture.generator.currentTime,
        updateTime = fixture.generator.currentTime
      )

      isSameTask(taskReturned, expectedTask)
      isSameTask(taskInStorage, expectedTask)

      And("generate correct journal entry")
      response.success.value.journals.changes should contain theSameElementsInOrderAs List(
        Change(fixture.generator.randomUUID, None, taskReturned, fixture.generator.currentTime)
      )
    }
  }
}
