package moe.brianhsu.maidroidtask.usecase.task

import java.time.{LocalDate, LocalDateTime, LocalTime}
import java.util.UUID

import moe.brianhsu.maidroidtask.entity.{Change, ScheduledAt, Tag, Task}
import moe.brianhsu.maidroidtask.usecase.Validations.BreakingChain.{MarkAsDone, MarkAsPending}
import moe.brianhsu.maidroidtask.usecase.Validations.{AccessDenied, AlreadyTrashed, BreakingChain, DependencyLoop, NotFound, Required}
import moe.brianhsu.maidroidtask.usecase.base.types.ResultHolder
import moe.brianhsu.maidroidtask.utils.fixture.{BaseFixture, BaseFixtureFeature}

class EditTaskFixture extends BaseFixture {

  private val fixtureCreateTime = LocalDateTime.parse("2020-07-30T11:12:13")

  val userTag: Tag = tagRepo.write.insert(Tag(UUID.randomUUID, loggedInUser.uuid, "ExistTag", None, isTrashed = false, generator.currentTime, generator.currentTime))
  val otherUserTag: Tag = tagRepo.write.insert(Tag(UUID.randomUUID, otherUser.uuid, "OtherUserTag", None, isTrashed = false, generator.currentTime, generator.currentTime))

  val task1: Task = taskRepo.write.insert(Task(UUID.randomUUID, loggedInUser.uuid, "SomeTask 1", createTime = fixtureCreateTime, updateTime = fixtureCreateTime))
  val task2: Task = taskRepo.write.insert(Task(UUID.randomUUID, loggedInUser.uuid, "SomeTask 2", createTime = fixtureCreateTime, updateTime = fixtureCreateTime))
  val task3: Task = taskRepo.write.insert(
    Task(
      UUID.randomUUID, loggedInUser.uuid, "SomeTask 3",
      note = Some("note"),
      project = Some(UUID.randomUUID()),
      tags = List(UUID.randomUUID(), UUID.randomUUID()),
      dependsOn = List(task2.uuid),
      waitUntil = Some(LocalDateTime.parse("2020-09-30T11:11:11")),
      due = Some(LocalDateTime.parse("2030-01-12T12:33:11")),
      scheduledAt = Some(ScheduledAt(LocalDate.parse("2020-01-11"), None)),
      createTime = fixtureCreateTime,
      updateTime = fixtureCreateTime
    )
  )

  val otherUserTask: Task = taskRepo.write.insert(
    Task(
      UUID.randomUUID, otherUser.uuid, "OtherUserTask",
      createTime = fixtureCreateTime, updateTime = fixtureCreateTime
    )
  )

  def run(request: EditTask.Request): ResultHolder[Task] = {
    val useCase = new EditTask(request)
    useCase.execute()
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
      val response = fixture.run(request)

      Then("it should NOT pass the validation and yield NotFound error")
      response should containsFailedValidation("uuid", NotFound)
    }

    Scenario("Edit task that belong to other user") { fixture =>
      Given("user request to edit a task belongs to other user")
      val request = EditTask.Request(fixture.loggedInUser, fixture.otherUserTask.uuid, Some("SomeDesc"))

      When("run the use case")
      val response = fixture.run(request)

      Then("it should NOT pass the validation and yield AccessDenied error")
      response should containsFailedValidation("uuid", AccessDenied)
    }

    Scenario("Edit task that new task depends on non-exist task") { fixture =>
      Given("user request to edit a task and make it depends on non-exist task")
      val nonExistUUID = UUID.fromString("286b5649-049d-476a-9ef2-38de31e86c5b")
      val request = EditTask.Request(fixture.loggedInUser, fixture.task1.uuid, dependsOn = Some(List(fixture.task2.uuid, nonExistUUID)))

      When("run the use case")
      val response = fixture.run(request)

      Then("it should NOT pass the validation, and yield NotFound error")
      response should containsFailedValidation("dependsOn", NotFound)
    }

    Scenario("Edit task that title is empty") { fixture =>
      Given("user request to edit a task, and new title only consist of empty characters")
      val request = EditTask.Request(fixture.loggedInUser, fixture.task1.uuid, Some("   \t \n   "))

      When("run the use case")
      val response = fixture.run(request)

      Then("it should NOT pass the validation and yield Required error")
      response should containsFailedValidation("description", Required)
    }

    Scenario("Edit task that is already trashed") { fixture =>
      Given("user request to edit a task that is already trashed")
      val task = fixture.createTask(fixture.loggedInUser, "TrashedTask", isTrashed = true)
      val request = EditTask.Request(fixture.loggedInUser, task.uuid, description = Some("NewTitle"))

      When("run the use case")
      val response = fixture.run(request)

      Then("it should NOT pass the validation and yield AlreadyTrashed error")
      response should containsFailedValidation("uuid", AlreadyTrashed)
    }

    Scenario("Edit task that new tags has non-exist tag UUID") { fixture =>
      Given("user request to edit a task, and new tags has non-exist tag UUID")
      val nonExistUUID = UUID.randomUUID
      val newTagList = List(fixture.userTag.uuid, nonExistUUID)
      val request = EditTask.Request(
        fixture.loggedInUser,
        fixture.task1.uuid,
        tags = Some(newTagList)
      )

      When("run the use case")
      val response = fixture.run(request)

      Then("is should NOT pass the validation and yield NotFound error")
      response should containsFailedValidation("tags", NotFound)
    }

    Scenario("Edit task that new tags has trashed tag UUID") { fixture =>
      Given("user request to edit a task, and new tags has trashed tag UUID")
      val trashedTag = fixture.createTag(fixture.loggedInUser, "TrashedTag", isTrashed = true)
      val newTagList = List(fixture.userTag.uuid, trashedTag.uuid)
      val request = EditTask.Request(
        fixture.loggedInUser,
        fixture.task1.uuid,
        tags = Some(newTagList)
      )

      When("run the use case")
      val response = fixture.run(request)

      Then("is should NOT pass the validation and yield NotFound error")
      response should containsFailedValidation("tags", AlreadyTrashed)
    }

    Scenario("Edit task that new tags belongs to other user") { fixture =>
      Given("user request to edit a task, and new tags belongs to other user")
      val newTagList = List(fixture.userTag.uuid, fixture.otherUserTag.uuid)
      val request = EditTask.Request(
        fixture.loggedInUser,
        fixture.task1.uuid,
        tags = Some(newTagList)
      )

      When("run the use case")
      val response = fixture.run(request)

      Then("is should NOT pass the validation and yield NotFound error")
      response should containsFailedValidation("tags", AccessDenied)
    }
  }

  Feature("Blocking create a dependency loop when assign parent task") {
    Scenario("Task depends on each other") { fixture =>

      Given("taskB has a parent task named taskA")
      val taskA = fixture.createTask(fixture.loggedInUser, "TaskA")
      val taskB = fixture.createTask(fixture.loggedInUser, "TaskB", dependsOn = List(taskA.uuid))

      And("user request to assign taskB as parent of taskA")
      val request = EditTask.Request(fixture.loggedInUser, taskA.uuid, dependsOn = Some(List(taskB.uuid)))

      When("run the use case")
      val response = fixture.run(request)

      Then("it should NOT pass the validation and yield DependencyLoop error")
      response should containsFailedValidation("dependsOn", DependencyLoop)
    }

    Scenario("Edit task will create loop") { fixture =>
      Given("A task dependency looks like the following")
      info("    taskA <-- taskB <-- taskC <-- taskD")
      val taskA = fixture.createTask(fixture.loggedInUser, "TaskA")
      val taskB = fixture.createTask(fixture.loggedInUser, "TaskB", dependsOn = List(taskA.uuid))
      val taskC = fixture.createTask(fixture.loggedInUser, "TaskC", dependsOn = List(taskB.uuid))
      val taskD = fixture.createTask(fixture.loggedInUser, "TaskD", dependsOn = List(taskC.uuid))

      And("user request to assign taskD as parent of taskB")
      info("    taskA <-- taskB <-- taskC <-- taskD")
      info("                 |                   ^ ")
      info("                 +-------------------+ ")
      val request = EditTask.Request(fixture.loggedInUser, taskD.uuid, dependsOn = Some(List(taskB.uuid)))

      When("run the use case")
      val response = fixture.run(request)

      Then("it should NOT pass the validation and yield DependencyLoop error")
      response should containsFailedValidation("dependsOn", DependencyLoop)
    }

    Scenario("Edit task will create loop, for multiple depednsOn") { fixture =>
      Given("A task dependency looks like the following")
      info("    taskA <-- taskB <-- taskC <-- taskD")
      info("              taskE <-+                ")

      val taskA = fixture.createTask(fixture.loggedInUser, "TaskA")
      val taskB = fixture.createTask(fixture.loggedInUser, "TaskB", dependsOn = List(taskA.uuid))
      val taskE = fixture.createTask(fixture.loggedInUser, "TaskA")
      val taskC = fixture.createTask(fixture.loggedInUser, "TaskC", dependsOn = List(taskB.uuid, taskE.uuid))
      val taskD = fixture.createTask(fixture.loggedInUser, "TaskD", dependsOn = List(taskC.uuid))

      And("user request to assign taskD as parent of taskB")
      info("    taskA <-- taskB <-- taskC <-- taskD")
      info("              taskE <-+                ")
      info("                 |                   ^ ")
      info("                 +-------------------+ ")
      val request = EditTask.Request(fixture.loggedInUser, taskD.uuid, dependsOn = Some(List(taskE.uuid)))

      When("run the use case")
      val response = fixture.run(request)

      Then("it should NOT pass the validation and yield DependencyLoop error")
      response should containsFailedValidation("dependsOn", DependencyLoop)
    }

  }

  Feature("Detect dependency chain been break") {
    Scenario("Chain break due to mark leaf blocked task as done") { fixture =>
      Given("A task named taskD that depends on completed task named taskA, and un-completed taskB, taskC")
      info(
        """
          |  O TaskD
          |     V TaskA
          |     O TaskB
          |     O TaskC
          |""".stripMargin)
      val taskA = fixture.createTask(fixture.loggedInUser, "TaskA", isDone = true)
      val taskB = fixture.createTask(fixture.loggedInUser, "TaskB", isDone = false)
      val taskC = fixture.createTask(fixture.loggedInUser, "TaskC", isDone = false)
      val taskD = fixture.createTask(fixture.loggedInUser, "TaskD", dependsOn = List(taskA.uuid, taskB.uuid, taskC.uuid))

      And("user request to mark taskD as done while taskB, taskC is still in pending mode")
      info(
        """
          |  V TaskD
          |     V TaskA
          |     O TaskB
          |     O TaskC
          |""".stripMargin)

      val request = EditTask.Request(fixture.loggedInUser, taskD.uuid, isDone = Some(true))

      When("run the use case")
      val response = fixture.run(request)

      Then("it should NOT pass the validation and yield DependencyBreak error")
      And("the error contains the blocking / blocked task information")
      response should containsFailedValidation("isDone", BreakingChain(MarkAsDone, Nil, List(taskB.uuid, taskC.uuid)))
    }

    Scenario("Chain break due to mark middle level blocked task as done") { fixture =>
      Given("A task named taskD that depends on a completed task named taskA, uncompleted taskB and taskC")
      val taskA = fixture.createTask(fixture.loggedInUser, "TaskA", isDone = true)
      val taskB = fixture.createTask(fixture.loggedInUser, "TaskB", isDone = false)
      val taskC = fixture.createTask(fixture.loggedInUser, "TaskC", isDone = false)
      val taskD = fixture.createTask(fixture.loggedInUser, "TaskD", dependsOn = List(taskA.uuid, taskB.uuid, taskC.uuid))

      And("A task named taskE that depends on taskD")
      info(
        """
          |  O TaskE
          |     O TaskD
          |        V TaskA
          |        O TaskB
          |        O TaskC
          |""".stripMargin)
      val taskE = fixture.createTask(fixture.loggedInUser, "TaskE", dependsOn = List(taskD.uuid))

      And("user request to mark taskD as done while taskB, taskC is still in pending mode")
      info(
        """
          |  O TaskE
          |     V TaskD
          |        V TaskA
          |        O TaskB
          |        O TaskC
          |""".stripMargin)
      val request = EditTask.Request(fixture.loggedInUser, taskD.uuid, isDone = Some(true))

      When("run the use case")
      val response = fixture.run(request)

      Then("it should NOT pass the validation and yield DependencyBreak error")
      And("the error contains the blocking / blocked task information")

      response should containsFailedValidation("isDone", BreakingChain(MarkAsDone, List(taskE.uuid), List(taskB.uuid, taskC.uuid)))
    }

    Scenario("Chain break due to mark leaf task as pending, when parent task is mark as done") { fixture =>
      Given("A completed taskC, uncompleted taskB both depends on a completed taskA")
      info(
        """
          |  O TaskC
          |     V TaskA
          |  V TaskB
          |     V TaskA
          |""".stripMargin)
      val taskA = fixture.createTask(fixture.loggedInUser, "TaskA", isDone = true)
      val taskB = fixture.createTask(fixture.loggedInUser, "TaskB", isDone = true, dependsOn = List(taskA.uuid))
      val taskC = fixture.createTask(fixture.loggedInUser, "TaskC", isDone = false, dependsOn = List(taskA.uuid))

      And("user request to mark taskA as not completed")
      info(
        """
          |  O TaskC
          |     V TaskA
          |  V TaskB
          |     V TaskA
          |""".stripMargin)
      val request = EditTask.Request(fixture.loggedInUser, taskA.uuid, isDone = Some(false))

      When("run the use case")
      val response = fixture.run(request)

      Then("it should NOT pass the validation and yield DependencyBreak error")
      And("the error contains the blocking information")
      response should containsFailedValidation("isDone", BreakingChain(MarkAsPending, List(taskB.uuid), Nil))
    }

    Scenario("Chain break due to mark middle level as not completed, when parent is done") { fixture =>
      Given("A task list looks like the following")
      info(
        """
          |  V TaskD
          |     V TaskC
          |        V TaskB
          |        V TaskA
          |""".stripMargin)
      val taskA = fixture.createTask(fixture.loggedInUser, "TaskA", isDone = true)
      val taskB = fixture.createTask(fixture.loggedInUser, "TaskB", isDone = true)
      val taskC = fixture.createTask(fixture.loggedInUser, "TaskC", isDone = true, dependsOn = List(taskA.uuid, taskB.uuid))
      val taskD = fixture.createTask(fixture.loggedInUser, "TaskD", isDone = true, dependsOn = List(taskC.uuid))

      And("user request to mark taskC as not completed")
      info(
        """
          |  V TaskD
          |     O TaskC
          |        V TaskB
          |        V TaskA
          |""".stripMargin)
      val request = EditTask.Request(fixture.loggedInUser, taskC.uuid, isDone = Some(false))

      When("run the use case")
      val response = fixture.run(request)

      Then("it should NOT pass the validation and yield DependencyBreak error")
      And("the error contains the blocking information")
      response should containsFailedValidation("isDone", BreakingChain(MarkAsPending, List(taskD.uuid), Nil))
    }

    Scenario("Not breaking chain when mark root node as done") { fixture =>
      pending
    }

    Scenario("Not breaking chain when mark middle node as done") { fixture =>
      pending
    }

    Scenario("Not breaking chain when mark leaf node as not completed") { fixture =>
      pending
    }

    Scenario("Not breaking chain when mark middle node as not completed") { fixture =>
      pending
    }
  }

  Feature("Update task in system") {
    Scenario("Update all fields inside a task that belongs to logged in user") { fixture =>
      Given("user request to edit a task belongs to him or her")
      val taskDependsOn = List(fixture.task2.uuid)
      val newTagsList = List(fixture.userTag.uuid)
      val request = EditTask.Request(
        fixture.loggedInUser, fixture.task1.uuid,
        description = Some("EditedDescription"),
        note = Some(Some("Note")),
        dependsOn = Some(taskDependsOn),
        tags = Some(newTagsList),
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
      val response = fixture.run(request)

      Then("the task in storage should be updated with new status")
      val expectedTask = Task(
        fixture.task1.uuid, fixture.loggedInUser.uuid,
        description = "EditedDescription",
        note = Some("Note"),
        dependsOn = taskDependsOn,
        tags = newTagsList,
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

      isSameTask(response.success.value.result, expectedTask)

      And("it should also updated to storage")
      val taskInStorage = fixture.taskRepo.read.findByUUID(expectedTask.uuid)
      isSameTask(taskInStorage.value, expectedTask)

      And("the journals should have correct entries")
      response.success.value.journals.changes shouldBe List(
        Change(fixture.generator.randomUUID, Some(fixture.task1), expectedTask, fixture.generator.currentTime)
      )
    }

    Scenario("Only update some fields") { fixture =>
      Given("user request to edit a task belongs to him or her")
      val request = EditTask.Request(
        fixture.loggedInUser, fixture.task3.uuid,
        description = Some("NewDescription"),
      )

      When("run the use case")
      val response = fixture.run(request)

      Then("the task in storage should only has description updated")
      val expectedTask = Task(
        fixture.task3.uuid, fixture.loggedInUser.uuid,
        description = "NewDescription",
        note = fixture.task3.note,
        project = fixture.task3.project,
        tags = fixture.task3.tags,
        dependsOn = fixture.task3.dependsOn,
        waitUntil = fixture.task3.waitUntil,
        due = fixture.task3.due,
        scheduledAt = fixture.task3.scheduledAt,
        isDone = fixture.task3.isDone,
        createTime = fixture.task3.createTime,
        updateTime = fixture.generator.currentTime
      )

      isSameTask(response.success.value.result, expectedTask)

      And("it should also updated to storage")
      val taskInStorage = fixture.taskRepo.read.findByUUID(expectedTask.uuid)
      isSameTask(taskInStorage.value, expectedTask)

      And("the journals should have correct entries")
      response.success.value.journals.changes shouldBe List(
        Change(fixture.generator.randomUUID, Some(fixture.task3), expectedTask, fixture.generator.currentTime)
      )
    }

  }

}
