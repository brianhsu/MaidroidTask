package moe.brianhsu.maidroidtask.usecase.project

import java.util.UUID

import moe.brianhsu.maidroidtask.entity.{Change, Journal, Project}
import moe.brianhsu.maidroidtask.usecase.Validations.{AccessDenied, AlreadyTrashed, HasChildren, NotFound}
import moe.brianhsu.maidroidtask.usecase.base.types.ResultHolder
import moe.brianhsu.maidroidtask.utils.fixture.{BaseFixture, BaseFixtureFeature}

class TrashProjectFixture extends BaseFixture {
  def run(request: TrashProject.Request): ResultHolder[Project] = {
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
      val userProject = fixture.createProject(fixture.loggedInUser, "MyProject")
      val task = fixture.createTask(fixture.loggedInUser, "Task", projectUUID = Some(userProject.uuid))

      And("user request to trash that project")
      val trashRequest = TrashProject.Request(fixture.loggedInUser, userProject.uuid)

      When("run the use case")
      val response = fixture.run(trashRequest)

      Then("the task in storage should also be trashed")
      val taskInStorage = fixture.taskRepo.read.findByUUID(task.uuid).value
      taskInStorage.isTrashed shouldBe true

      And("maintain the project uuid in task entity")
      taskInStorage.project.value shouldBe userProject.uuid
      taskInStorage.updateTime shouldBe fixture.generator.currentTime

      And("the project is updated in storage")
      val projectInStorage = fixture.projectRepo.read.findByUUID(userProject.uuid).value
      projectInStorage.isTrashed shouldBe true

      And("generate correct log")
      val journal = response.success.value.journals
      inside(journal) { case Journal(journalUUID, userUUID, request, changes, timestamp) =>
        journalUUID shouldBe fixture.generator.randomUUID
        userUUID shouldBe fixture.loggedInUser.uuid
        request shouldBe trashRequest
        changes should contain theSameElementsAs List(
          Change(fixture.generator.randomUUID, Some(userProject), projectInStorage, fixture.generator.currentTime),
          Change(fixture.generator.randomUUID, Some(task), taskInStorage, fixture.generator.currentTime)
        )
        timestamp shouldBe fixture.generator.currentTime
      }
    }

    Scenario("Trash a project that has trashed children projects") { fixture =>
      Given("a parent project with trashed children projects")
      val parentProject = fixture.createProject(fixture.loggedInUser, "ParentProject")
      val trashedChild1 = fixture.createProject(fixture.loggedInUser, "Trashed 1", Some(parentProject.uuid), isTrashed = true)
      val trashedChild2 = fixture.createProject(fixture.loggedInUser, "Trashed 1", Some(parentProject.uuid), isTrashed = true)

      And("user request to trash the parent project")
      val trashRequest = TrashProject.Request(fixture.loggedInUser, parentProject.uuid)

      When("run the use case")
      val response = fixture.run(trashRequest)

      Then("the parent project should be deleted")
      val trashedParentProject = response.success.value.result
      trashedParentProject.isTrashed shouldBe true
      trashedParentProject.updateTime shouldBe fixture.generator.currentTime

      And("stored in storage")
      val projectInStorage = fixture.projectRepo.read.findByUUID(trashRequest.uuid).value
      projectInStorage shouldBe trashedParentProject

      And("generate correct journal entry")
      inside(response.success.value.journals) { case Journal(journalUUID, userUUID, request, changes, timestamp) =>
        journalUUID shouldBe fixture.generator.randomUUID
        userUUID shouldBe fixture.loggedInUser.uuid
        request shouldBe trashRequest
        timestamp shouldBe fixture.generator.currentTime
        changes should contain theSameElementsAs List(
          Change(
            fixture.generator.randomUUID,
            Some(parentProject),
            trashedParentProject,
            fixture.generator.currentTime
          )
        )
      }
    }

    Scenario("There are multiple task in that project") { fixture =>
      Given("a project with multiple tasks, including trashed one")
      val userProject = fixture.createProject(fixture.loggedInUser, "MyProject")
      val task1 = fixture.createTask(fixture.loggedInUser, "Task 1", projectUUID = Some(userProject.uuid))
      val task2 = fixture.createTask(fixture.loggedInUser, "Task 2", projectUUID = Some(userProject.uuid), isTrashed = true)
      val task3 = fixture.createTask(fixture.loggedInUser, "Task 3", projectUUID = Some(userProject.uuid))

      And("user request to trash that project")
      val trashRequest = TrashProject.Request(fixture.loggedInUser, userProject.uuid)

      When("run the use case")
      val response = fixture.run(trashRequest)

      Then("all tasks in storage should also be trashed")
      val taskInStorage = List(task1.uuid, task2.uuid, task3.uuid).flatMap(fixture.taskRepo.read.findByUUID)
      forAll (taskInStorage) { task => task.isTrashed shouldBe true }

      And("maintain the project uuid in tasks")
      forAll (taskInStorage) { task => task.project shouldBe Some(userProject.uuid) }

      And("the project is updated in storage")
      val projectInStorage = fixture.projectRepo.read.findByUUID(userProject.uuid).value
      projectInStorage.isTrashed shouldBe true

      And("generate correct log")
      val journal = response.success.value.journals
      inside(journal) { case Journal(journalUUID, userUUID, request, changes, timestamp) =>
        journalUUID shouldBe fixture.generator.randomUUID
        userUUID shouldBe fixture.loggedInUser.uuid
        request shouldBe trashRequest
        changes should contain theSameElementsAs List(
          Change(fixture.generator.randomUUID, Some(userProject), projectInStorage, fixture.generator.currentTime),
          Change(fixture.generator.randomUUID, Some(task1), taskInStorage.head, fixture.generator.currentTime),
          Change(fixture.generator.randomUUID, Some(task2), taskInStorage(1), fixture.generator.currentTime),
          Change(fixture.generator.randomUUID, Some(task3), taskInStorage(2), fixture.generator.currentTime)
        )
        timestamp shouldBe fixture.generator.currentTime
      }
    }
  }

  Feature("Trash a project in oneLevelUp way") {
    Scenario("There are only 1 tasks, no-parent project") { fixture =>
      Given("a project with only 1 non-trashed task")
      val userProject = fixture.createProject(fixture.loggedInUser, "MyProject")
      val task = fixture.createTask(fixture.loggedInUser, "Task", projectUUID = Some(userProject.uuid))

      And("user request to trash that project")
      val trashRequest = TrashProject.Request(fixture.loggedInUser, userProject.uuid, TrashProject.MoveOneLevelUp)

      When("run the use case")
      val response = fixture.run(trashRequest)

      Then("the task in storage should update project to no parent project and not mark as trashed")
      val taskInStorage = fixture.taskRepo.read.findByUUID(task.uuid).value
      taskInStorage.project shouldBe None
      taskInStorage.isTrashed shouldBe false
      taskInStorage.updateTime shouldBe fixture.generator.currentTime

      And("the project is marked as trashed")
      val updatedProject = response.success.value.result
      updatedProject.isTrashed shouldBe true
      updatedProject.updateTime shouldBe fixture.generator.currentTime

      And("updated in storage")
      val projectInStorage = fixture.projectRepo.read.findByUUID(updatedProject.uuid).value
      projectInStorage shouldBe updatedProject

      And("generate correct log")
      val journal = response.success.value.journals
      inside(journal) { case Journal(journalUUID, userUUID, request, changes, timestamp) =>
        journalUUID shouldBe fixture.generator.randomUUID
        userUUID shouldBe fixture.loggedInUser.uuid
        request shouldBe trashRequest
        changes should contain theSameElementsAs List(
          Change(fixture.generator.randomUUID, Some(userProject), projectInStorage, fixture.generator.currentTime),
          Change(fixture.generator.randomUUID, Some(task), taskInStorage, fixture.generator.currentTime),
        )
        timestamp shouldBe fixture.generator.currentTime
      }
    }

    Scenario("There are multiple task in that project, no-parent job") { fixture =>
      Given("a project with parent project, and contains multiple tasks, including trashed one")
      val userProject = fixture.createProject(fixture.loggedInUser, "MyProject")

      val task1 = fixture.createTask(fixture.loggedInUser, "Task 1", projectUUID = Some(userProject.uuid))
      val task2 = fixture.createTask(fixture.loggedInUser, "Task 2", projectUUID = Some(userProject.uuid))
      val task3 = fixture.createTask(fixture.loggedInUser, "Task 3", projectUUID = Some(userProject.uuid))

      And("user request to trash that project")
      val trashRequest = TrashProject.Request(fixture.loggedInUser, userProject.uuid, TrashProject.MoveOneLevelUp)

      When("run the use case")
      val response = fixture.run(trashRequest)

      Then("all tasks in storage has been updated to no project")
      val taskInStorage = List(task1.uuid, task2.uuid, task3.uuid).flatMap(fixture.taskRepo.read.findByUUID)
      forAll (taskInStorage) { task =>
        task.isTrashed shouldBe false
        task.project shouldBe None
      }

      And("the project is marked as trashed")
      val updatedProject = response.success.value.result
      updatedProject.isTrashed shouldBe true
      updatedProject.updateTime shouldBe fixture.generator.currentTime

      And("updated in storage")
      val projectInStorage = fixture.projectRepo.read.findByUUID(updatedProject.uuid).value
      projectInStorage shouldBe updatedProject

      And("generate correct log")
      val journal = response.success.value.journals
      inside(journal) { case Journal(journalUUID, userUUID, request, changes, timestamp) =>
        journalUUID shouldBe fixture.generator.randomUUID
        userUUID shouldBe fixture.loggedInUser.uuid
        request shouldBe trashRequest
        changes should contain theSameElementsAs List(
          Change(fixture.generator.randomUUID, Some(userProject), projectInStorage, fixture.generator.currentTime),
          Change(fixture.generator.randomUUID, Some(task1), taskInStorage.head, fixture.generator.currentTime),
          Change(fixture.generator.randomUUID, Some(task2), taskInStorage(1), fixture.generator.currentTime),
          Change(fixture.generator.randomUUID, Some(task3), taskInStorage(2), fixture.generator.currentTime),
        )
        timestamp shouldBe fixture.generator.currentTime
      }
    }

    Scenario("There are only 1 tasks, with parent project") { fixture =>
      Given("a project with parent project, and contains only 1 tasks")
      val parentProject = fixture.createProject(fixture.loggedInUser, "ParentProject")
      val userProject = fixture.createProject(fixture.loggedInUser, "MyProject", parentProject = Some(parentProject.uuid))
      val task = fixture.createTask(fixture.loggedInUser, "Task", projectUUID = Some(userProject.uuid))

      And("user request to trash that project")
      val trashRequest = TrashProject.Request(fixture.loggedInUser, userProject.uuid, TrashProject.MoveOneLevelUp)

      When("run the use case")
      val response = fixture.run(trashRequest)

      Then("the task in storage should update project to parent project and not mark as trashed")
      val taskInStorage = fixture.taskRepo.read.findByUUID(task.uuid).value
      taskInStorage.project.value shouldBe parentProject.uuid
      taskInStorage.isTrashed shouldBe false
      taskInStorage.updateTime shouldBe fixture.generator.currentTime

      And("the project is marked as trashed")
      val updatedProject = response.success.value.result
      updatedProject.isTrashed shouldBe true
      updatedProject.updateTime shouldBe fixture.generator.currentTime

      And("updated in storage")
      val projectInStorage = fixture.projectRepo.read.findByUUID(updatedProject.uuid).value
      projectInStorage shouldBe updatedProject

      And("generate correct log")
      val journal = response.success.value.journals
      inside(journal) { case Journal(journalUUID, userUUID, request, changes, timestamp) =>
        journalUUID shouldBe fixture.generator.randomUUID
        userUUID shouldBe fixture.loggedInUser.uuid
        request shouldBe trashRequest
        changes should contain theSameElementsAs List(
          Change(fixture.generator.randomUUID, Some(userProject), projectInStorage, fixture.generator.currentTime),
          Change(fixture.generator.randomUUID, Some(task), taskInStorage, fixture.generator.currentTime),
        )
        timestamp shouldBe fixture.generator.currentTime
      }
    }

    Scenario("There are multiple task in that project, with parent job") { fixture =>
      Given("a project with multiple tasks and parent project, including trashed one")
      val parentProject = fixture.createProject(fixture.loggedInUser, "ParentProject")
      val userProject = fixture.createProject(fixture.loggedInUser, "MyProject", parentProject = Some(parentProject.uuid))

      val task1 = fixture.createTask(fixture.loggedInUser, "Task 1", projectUUID = Some(userProject.uuid))
      val task2 = fixture.createTask(fixture.loggedInUser, "Task 2", projectUUID = Some(userProject.uuid))
      val task3 = fixture.createTask(fixture.loggedInUser, "Task 3", projectUUID = Some(userProject.uuid))

      And("user request to trash that project")
      val trashRequest = TrashProject.Request(fixture.loggedInUser, userProject.uuid, TrashProject.MoveOneLevelUp)

      When("run the use case")
      val response = fixture.run(trashRequest)

      Then("all tasks in storage should have no assigned project and not trashed")
      val taskInStorage = List(task1.uuid, task2.uuid, task3.uuid).flatMap(fixture.taskRepo.read.findByUUID)
      forAll (taskInStorage) { task =>
        task.isTrashed shouldBe false
        task.project shouldBe userProject.parentProjectUUID
      }

      And("the project is is marked as trashed")
      val updatedProject = response.success.value.result
      updatedProject.isTrashed shouldBe true
      updatedProject.updateTime shouldBe fixture.generator.currentTime

      And("the change is stored in storage")
      val projectInStorage = fixture.projectRepo.read.findByUUID(updatedProject.uuid).value
      projectInStorage shouldBe updatedProject

      And("generate correct log")
      val journal = response.success.value.journals
      inside(journal) { case Journal(journalUUID, userUUID, request, changes, timestamp) =>
        journalUUID shouldBe fixture.generator.randomUUID
        userUUID shouldBe fixture.loggedInUser.uuid
        request shouldBe trashRequest
        changes should contain theSameElementsAs List(
          Change(fixture.generator.randomUUID, Some(userProject), projectInStorage, fixture.generator.currentTime),
          Change(fixture.generator.randomUUID, Some(task1), taskInStorage.head, fixture.generator.currentTime),
          Change(fixture.generator.randomUUID, Some(task2), taskInStorage(1), fixture.generator.currentTime),
          Change(fixture.generator.randomUUID, Some(task3), taskInStorage(2), fixture.generator.currentTime),
        )
        timestamp shouldBe fixture.generator.currentTime
      }
    }

  }


}
