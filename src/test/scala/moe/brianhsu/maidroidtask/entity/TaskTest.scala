package moe.brianhsu.maidroidtask.entity

import moe.brianhsu.maidroidtask.gateway.repo.TaskReadable
import moe.brianhsu.maidroidtask.utils.fixture.{BaseFixture, BaseFixtureFeature}

class TaskFixture extends BaseFixture

class TaskTest extends BaseFixtureFeature[TaskFixture] {
  override protected def createFixture: TaskFixture = new TaskFixture

  Feature("Get blockedBy task list") {
    Scenario("Current task is not blocked by any task") { fixture =>
      implicit val taskRead: TaskReadable = fixture.taskRepo.read

      Given("A task named currentTask")
      val currentTask = fixture.createTask(fixture.loggedInUser, "CurrentTask")

      And("there currentTask does not depends on any task")
      When("call blockedBy method on currentTask")
      val blockedBy = currentTask.blockedBy

      Then("it should return empty list")
      blockedBy shouldBe Nil
    }

    Scenario("Current task is blocked by other two tasks") { fixture =>
      implicit val taskRead: TaskReadable = fixture.taskRepo.read

      Given("A task named currentTask")
      And("there currentTask depends on two task")
      val task1 = fixture.createTask(fixture.loggedInUser, "Task1")
      val task2 = fixture.createTask(fixture.loggedInUser, "Task2")

      val currentTask = fixture.createTask(
        fixture.loggedInUser, "CurrentTask",
        dependsOn = List(task1.uuid, task2.uuid)
      )

      When("call blockedBy method on currentTask")
      val blockedBy = currentTask.blockedBy

      Then("it should return list contains those two tasks")
      blockedBy should contain theSameElementsAs List(task1, task2)
    }

    Scenario("Current task is blocked by other two trashed / done tasks") { fixture =>
      implicit val taskRead: TaskReadable = fixture.taskRepo.read

      Given("A task named currentTask")
      And("there currentTask depends on two task that already trashed or done")
      val task1 = fixture.createTask(fixture.loggedInUser, "Task1", isDone = true)
      val task2 = fixture.createTask(fixture.loggedInUser, "Task2", isTrashed = true)

      val currentTask = fixture.createTask(
        fixture.loggedInUser, "CurrentTask",
        dependsOn = List(task1.uuid, task2.uuid)
      )

      When("call blockedBy method on currentTask")
      val blockedBy = currentTask.blockedBy

      Then("it should return empty list")
      blockedBy shouldBe Nil
    }

    Scenario("Current task is blocked by other tasks that has different status") { fixture =>
      implicit val taskRead: TaskReadable = fixture.taskRepo.read

      Given("A task named currentTask")
      And("there currentTask depends on two task that already trashed or done")
      And("another tasks that is in normal status")
      val task1 = fixture.createTask(fixture.loggedInUser, "Task1", isDone = true)
      val task2 = fixture.createTask(fixture.loggedInUser, "Task2", isTrashed = true)
      val task3 = fixture.createTask(fixture.loggedInUser, "Task3")
      val task4 = fixture.createTask(fixture.loggedInUser, "Task4")

      val currentTask = fixture.createTask(
        fixture.loggedInUser, "CurrentTask",
        dependsOn = List(task1.uuid, task2.uuid, task3.uuid, task4.uuid)
      )

      When("call blockedBy method on currentTask")
      val blockedBy = currentTask.blockedBy

      Then("it should return list contains those tasks that status is normal")
      blockedBy should contain theSameElementsAs List(task3, task4)
    }
  }

  Feature("Get blocking task list") {
    Scenario("Current task is not blocking any task") { fixture =>
      implicit val taskRead: TaskReadable = fixture.taskRepo.read

      Given("A task named currentTask")
      val currentTask = fixture.createTask(fixture.loggedInUser, "CurrentTask")

      And("there is no task depends on currentTask")
      When("get blocking list of currentTask")
      val blocking = currentTask.blocking

      Then("it should be empty list")
      blocking shouldBe Nil
    }

    Scenario("Current task is blocking other two tasks") { fixture =>
      implicit val taskRead: TaskReadable = fixture.taskRepo.read

      Given("A task named currentTask")
      val currentTask = fixture.createTask(fixture.loggedInUser, "CurrentTask")
      val otherTask = fixture.createTask(fixture.loggedInUser, "OtherTask")

      And("several tasks that depends on currentTask")
      val task1 = fixture.createTask(fixture.loggedInUser, "Task1", dependsOn = List(currentTask.uuid))
      val task2 = fixture.createTask(fixture.loggedInUser, "Task2", dependsOn = List(currentTask.uuid, otherTask.uuid))

      When("get blocking list of currentTask")
      val blocking = currentTask.blocking

      Then("it should return all task depends on currentTask")
      blocking should contain theSameElementsAs List(task1, task2)
    }

    Scenario("Current task is blocking other two (trashed / done) tasks") { fixture =>
      implicit val taskRead: TaskReadable = fixture.taskRepo.read

      Given("A task named currentTask")
      val currentTask = fixture.createTask(fixture.loggedInUser, "CurrentTask")
      val otherTask = fixture.createTask(fixture.loggedInUser, "OtherTask")

      And("a trashed tasks that depends on currentTask")
      val task1 = fixture.createTask(
        fixture.loggedInUser, "trashedTask",
        dependsOn = List(currentTask.uuid),
        isTrashed = true
      )

      And("a done tasks that depends on currentTask")
      val task2 = fixture.createTask(
        fixture.loggedInUser, "doneTask",
        dependsOn = List(currentTask.uuid, otherTask.uuid),
        isDone = true
      )

      When("get blocking list of currentTask")
      val blocking = currentTask.blocking

      Then("it should return empty list")
      blocking shouldBe Nil
    }

    Scenario("Current task is blocking several tasks with different status") { fixture =>
      implicit val taskRead: TaskReadable = fixture.taskRepo.read

      Given("A task named currentTask")
      val currentTask = fixture.createTask(fixture.loggedInUser, "CurrentTask")
      val otherTask = fixture.createTask(fixture.loggedInUser, "OtherTask")

      And("a trashed tasks that depends on currentTask")
      val trashedTask = fixture.createTask(
        fixture.loggedInUser, "trashedTask",
        dependsOn = List(currentTask.uuid),
        isTrashed = true
      )

      And("two normal tasks depends on currentTask")
      val task1 = fixture.createTask(fixture.loggedInUser, "Task1", dependsOn = List(currentTask.uuid))
      val task2 = fixture.createTask(fixture.loggedInUser, "Task2", dependsOn = List(currentTask.uuid))

      And("a done tasks that depends on currentTask")
      val doneTask = fixture.createTask(
        fixture.loggedInUser, "doneTask",
        dependsOn = List(currentTask.uuid, otherTask.uuid),
        isDone = true
      )

      When("get blocking list of currentTask")
      val blocking = currentTask.blocking

      Then("it should return those two normal tasks")
      blocking should contain theSameElementsAs List(task1, task2)
    }

  }
  Feature("Circular dependency detection") {
    Scenario("The current task has no parent task") { fixture =>
      implicit val taskRepo: TaskReadable = fixture.taskRepo.read

      Given("a task dependency looks like the following")
      info("    taskA <-- taskB <-- taskC <-- taskD")
      val taskA = fixture.createTask(fixture.loggedInUser, "TaskA")
      val taskB = fixture.createTask(fixture.loggedInUser, "TaskB", dependsOn = List(taskA.uuid))
      val taskC = fixture.createTask(fixture.loggedInUser, "TaskC", dependsOn = List(taskB.uuid))
      val taskD = fixture.createTask(fixture.loggedInUser, "TaskD", dependsOn = List(taskC.uuid))

      And("a current task named taskE without any parent task itself")
      val taskE = fixture.createTask(fixture.loggedInUser, "TaskE")

      When("check if assign taskE as parent task of those task will create loop")
      Then("it should return false for all cases")
      forAll(List(taskA, taskB, taskC, taskD)) { task =>
        task.hasLoopsWith(taskE.uuid) shouldBe false
      }
    }

    Scenario("Task depends on each other") { fixture =>
      implicit val taskRepo: TaskReadable = fixture.taskRepo.read

      Given("taskB has a parent task named taskA")
      val taskA = fixture.createTask(fixture.loggedInUser, "TaskA")
      val taskB = fixture.createTask(fixture.loggedInUser, "TaskB", dependsOn = List(taskA.uuid))

      When("check if add taskB as parent task of taskA will create loop")
      val hasLoop = taskA.hasLoopsWith(taskB.uuid)

      Then("it should return true")
      hasLoop shouldBe true
    }

    Scenario("Root parent task add to task outside of dependency") { fixture =>
      implicit val taskRepo: TaskReadable = fixture.taskRepo.read

      Given("A task dependency looks like the following")
      info("    taskA <-- taskB <-- taskC <-- taskD")
      val taskA = fixture.createTask(fixture.loggedInUser, "TaskA")
      val taskB = fixture.createTask(fixture.loggedInUser, "TaskB", dependsOn = List(taskA.uuid))
      val taskC = fixture.createTask(fixture.loggedInUser, "TaskC", dependsOn = List(taskB.uuid))
      val taskD = fixture.createTask(fixture.loggedInUser, "TaskD", dependsOn = List(taskC.uuid))

      And("another dependency looks like the following")
      info("    taskE <-- taskF <-- taskG")
      val taskE = fixture.createTask(fixture.loggedInUser, "TaskE")
      val taskF = fixture.createTask(fixture.loggedInUser, "TaskF", dependsOn = List(taskE.uuid))
      val taskG = fixture.createTask(fixture.loggedInUser, "TaskG", dependsOn = List(taskF.uuid))

      When("check if taskA be parent of task E, F, G will create loop")
      Then("it should all return false")
      forAll(List(taskE, taskF, taskG)) { task =>
        task.hasLoopsWith(taskA.uuid) shouldBe false
      }
    }

    Scenario("Creating loop for task dependency") { fixture =>
      implicit val taskRepo: TaskReadable = fixture.taskRepo.read

      Given("A task dependency looks like the following")
      info("    taskA <-- taskB <-- taskC <-- taskD")
      val taskA = fixture.createTask(fixture.loggedInUser, "TaskA")
      val taskB = fixture.createTask(fixture.loggedInUser, "TaskB", dependsOn = List(taskA.uuid))
      val taskC = fixture.createTask(fixture.loggedInUser, "TaskC", dependsOn = List(taskB.uuid))
      val taskD = fixture.createTask(fixture.loggedInUser, "TaskD", dependsOn = List(taskC.uuid))

      When("check if assign taskB as parent of taskD will create loop")
      info("    taskA <-- taskB <-- taskC <-- taskD")
      info("                    |                          ^")
      info("                    +--------------------------+")
      val hasLoop = taskD.hasLoopsWith(taskB.uuid)

      Then("it should return true")
      hasLoop shouldBe true
    }

    Scenario("Creating loop for multiple task dependency") { fixture =>
      implicit val taskRepo: TaskReadable = fixture.taskRepo.read

      Given("A task dependency looks like the following")
      info("    taskA <-- taskB <-- taskC <-- taskD")
      info("              taskE <--                ")
      val taskA = fixture.createTask(fixture.loggedInUser, "TaskA")
      val taskB = fixture.createTask(fixture.loggedInUser, "TaskB", dependsOn = List(taskA.uuid))
      val taskE = fixture.createTask(fixture.loggedInUser, "TaskE")
      val taskC = fixture.createTask(fixture.loggedInUser, "TaskC", dependsOn = List(taskB.uuid, taskE.uuid))
      val taskD = fixture.createTask(fixture.loggedInUser, "TaskD", dependsOn = List(taskC.uuid))

      When("check if assign taskB as parent of taskD will create loop")
      info("    taskA <-- taskB <-- taskC <-- taskD")
      info("              taskE <--             ^  ")
      info("                    |               |  ")
      info("                    +---------------+  ")
      val hasLoop = taskD.hasLoopsWith(taskB.uuid)

      Then("it should return true")
      hasLoop shouldBe true

      And("it should return true when check if assign taskE as parent of taskD will create loop")
      taskD.hasLoopsWith(taskE.uuid) shouldBe true

      And("it should return true when check if assign taskA as parent of taskD will create loop")
      taskD.hasLoopsWith(taskA.uuid) shouldBe true
    }

  }
}
