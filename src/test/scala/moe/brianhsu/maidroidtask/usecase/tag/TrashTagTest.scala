package moe.brianhsu.maidroidtask.usecase.tag

import java.time.LocalDateTime
import java.util.UUID

import moe.brianhsu.maidroidtask.entity.{Change, Journal, Tag}
import moe.brianhsu.maidroidtask.usecase.Validations.{AccessDenied, HasChildren, NotFound}
import moe.brianhsu.maidroidtask.usecase.base.types.ResultHolder
import moe.brianhsu.maidroidtask.utils.fixture.{BaseFixture, BaseFixtureFeature}

class TrashTagFixture extends BaseFixture {

  val fixtureCreateTime: LocalDateTime = LocalDateTime.now

  val otherUserTag: Tag = Tag(UUID.randomUUID(), otherUser.uuid, "OtherUserTag", None, isTrashed = false, fixtureCreateTime, fixtureCreateTime)
  val parentTag: Tag = Tag(UUID.randomUUID(), loggedInUser.uuid, "ParentTag", None, isTrashed = false, fixtureCreateTime, fixtureCreateTime)
  val childTag: Tag = Tag(UUID.randomUUID(), loggedInUser.uuid, "ChildTag", Some(parentTag.uuid), isTrashed = false, fixtureCreateTime, fixtureCreateTime)

  tagRepo.write.insert(otherUserTag)
  tagRepo.write.insert(parentTag)
  tagRepo.write.insert(childTag)

  def run(request: TrashTag.Request): ResultHolder[Tag] = {
    val useCase = new TrashTag(request)
    useCase.execute()
  }
}

class TrashTagTest extends BaseFixtureFeature[TrashTagFixture] {
  override protected def createFixture: TrashTagFixture = new TrashTagFixture

  Feature("Validation before test") {
    Scenario("Trash non exist tag") { fixture =>
      Given("user request to trash a non-exist tag UUID")
      val nonExistTagUUID = UUID.fromString("b73bc2cb-aebf-4f21-bf7b-0440d7bb52f9")
      val request = TrashTag.Request(fixture.loggedInUser, nonExistTagUUID)

      When("run the use case")
      val response = fixture.run(request)

      Then("it should NOT pass the validation and yield NotFound error")
      response should containsFailedValidation("uuid", NotFound)
    }

    Scenario("Trash tag belongs to others") { fixture =>
      Given("user request to trash a tag belongs to other user")
      val request = TrashTag.Request(fixture.loggedInUser, fixture.otherUserTag.uuid)

      When("run the use case")
      val response = fixture.run(request)

      Then("it should NOT pass the validation and yield AccessDenied error")
      response should containsFailedValidation("uuid", AccessDenied)
    }

    Scenario("Trash tag that has child tags") { fixture =>
      Given("user request to trash a tag that still has child tag")
      val request = TrashTag.Request(fixture.loggedInUser, fixture.parentTag.uuid)

      When("run the use case")
      val response = fixture.run(request)

      Then("it should NOT pass the validation and yield HasChildren error")
      response should containsFailedValidation("uuid", HasChildren)
    }

    Scenario("Trash tag belongs to logged in user") { fixture =>
      Given("user request to trash a tag belongs to them self")
      val request = TrashTag.Request(fixture.loggedInUser, fixture.childTag.uuid)

      When("run the use case")
      val response = fixture.run(request)

      Then("it should pass the validation")
      response.success.value.result shouldBe a[Tag]
    }
  }

  Feature("Trash the tag in storage") {
    Scenario("Trash the without deleted child tag") { fixture =>
      Given("user request to trash a tag belongs to him/her")
      val request = TrashTag.Request(fixture.loggedInUser, fixture.childTag.uuid)

      When("run the use case")
      val response = fixture.run(request)

      Then("it should returned trashed tag")
      val returnedTag = response.success.value.result
      inside(returnedTag) { case Tag(uuid, userUUID, name, parentTagUUID, isTrashed, createTime, updateTime) =>
        uuid shouldBe request.uuid
        userUUID shouldBe request.loggedInUser.uuid
        name shouldBe "ChildTag"
        parentTagUUID shouldBe Some(fixture.parentTag.uuid)
        isTrashed shouldBe true
        createTime shouldBe fixture.fixtureCreateTime
        updateTime shouldBe fixture.generator.currentTime
      }

      And("the tag in storage is updated")
      val tagInStorage = fixture.tagRepo.read.findByUUID(request.uuid).value
      tagInStorage shouldBe returnedTag

      And("generate the correct journal entry")
      response.success.value.journals.changes shouldBe List(
        Change(fixture.generator.randomUUID, Some(fixture.childTag), tagInStorage, fixture.generator.currentTime)
      )
    }

    Scenario("Trash a tag with trashed child tag") { fixture =>
      Given("A parent tag with trashed children tags")
      val parentTag = fixture.createTag(fixture.loggedInUser, "Parent Tag")
      val trashedTag1 = fixture.createTag(fixture.loggedInUser, "Trashed Tag 1", Some(parentTag.uuid), isTrashed = true)
      val trashedTag2 = fixture.createProject(fixture.loggedInUser, "Trashed Tag 2", Some(parentTag.uuid), isTrashed = true)

      And("user request to trash parent ")
      val request = TrashTag.Request(fixture.loggedInUser, parentTag.uuid)

      When("run the use case")
      val response = fixture.run(request)

      Then("the parent tag should be trashed")
      val trashedParentTag = response.success.value.result
      trashedParentTag.isTrashed shouldBe true

      And("stored in storage")
      val parentTagInStorage = fixture.tagRepo.read.findByUUID(parentTag.uuid).value
      parentTagInStorage shouldBe trashedParentTag

      And("generate correct journal log")
      inside(response.success.value.journals) { case Journal(journalUUID, userUUID, journalRequest, changes, timestamp) =>
        journalUUID shouldBe fixture.generator.randomUUID
        userUUID shouldBe fixture.loggedInUser.uuid
        journalRequest shouldBe request
        changes should contain theSameElementsAs List(
          Change(
            fixture.generator.randomUUID,
            Some(parentTag),
            parentTagInStorage,
            fixture.generator.currentTime
          )
        )
      }
    }
  }

  Feature("Trash tag should also unlink the tag from tasks") {
    Scenario("One task only has one tag") { fixture =>
      Given("a tag that we would like to trash")
      val tag = fixture.createTag(fixture.loggedInUser, "SomeTag")

      And("a task that has and only has the tag")
      val task = fixture.createTask(fixture.loggedInUser, "Task", List(tag.uuid))

      When("we request to trash the tag")
      val request = TrashTag.Request(fixture.loggedInUser, tag.uuid)
      val response = fixture.run(request)
      response.isSuccess shouldBe true

      Then("the task should also be updated, and contains no tag")
      val updatedTask = fixture.taskRepo.read.findByUUID(task.uuid).value
      updatedTask.tags shouldBe Nil
      updatedTask.updateTime shouldBe fixture.generator.currentTime

      And("The journal log should contains the entry that update the task")
      response.success.value.journals.changes.map(_.current.uuid) should contain (task.uuid)
    }

    Scenario("One task has multiple tags") { fixture =>
      Given("a tag that we would like to trash")
      val someTag = fixture.createTag(fixture.loggedInUser, "Tag1")
      val anotherTag = fixture.createTag(fixture.loggedInUser, "Tag 2")
      val tagToBeTrashed = fixture.createTag(fixture.loggedInUser, "SomeTag")

      And("a task that has multiple tags, including above tag")
      val task = fixture.createTask(
        fixture.loggedInUser,
        "Task",
        tags = List(someTag.uuid, tagToBeTrashed.uuid, anotherTag.uuid)
      )

      When("we request to trash the tag")
      val request = TrashTag.Request(fixture.loggedInUser, tagToBeTrashed.uuid)
      val response = fixture.run(request)
      response.isSuccess shouldBe true

      Then("the task should also be updated, and contains tags that is not trashed")
      val updatedTask = fixture.taskRepo.read.findByUUID(task.uuid).value
      updatedTask.tags should contain theSameElementsAs List(someTag.uuid, anotherTag.uuid)
      updatedTask.updateTime shouldBe fixture.generator.currentTime

      And("The journal log should contains the entry that update the task")
      response.success.value.journals.changes.map(_.current.uuid) should contain (task.uuid)
    }

    Scenario("Multiple tasks has multple tags") { fixture =>
      Given("a tag that we would like to trash")
      val someTag = fixture.createTag(fixture.loggedInUser, "Tag1")
      val anotherTag = fixture.createTag(fixture.loggedInUser, "Tag 2")
      val tagToBeTrashed = fixture.createTag(fixture.loggedInUser, "SomeTag")

      And("multiple task that has multiple tags, including above tag")
      val task1 = fixture.createTask(
        fixture.loggedInUser,
        "Task",
        tags = List(someTag.uuid, tagToBeTrashed.uuid, anotherTag.uuid)
      )
      val task2 = fixture.createTask(
        fixture.loggedInUser,
        "Task",
        tags = List(tagToBeTrashed.uuid)
      )
      val task3 = fixture.createTask(
        fixture.loggedInUser,
        "Task",
        tags = List(tagToBeTrashed.uuid, anotherTag.uuid)
      )

      When("we request to trash the tag")
      val request = TrashTag.Request(fixture.loggedInUser, tagToBeTrashed.uuid)
      val response = fixture.run(request)
      response.isSuccess shouldBe true

      Then("all task should also be updated, and contains tags that is not trashed")
      val updatedTask1 = fixture.taskRepo.read.findByUUID(task1.uuid).value
      val updatedTask2 = fixture.taskRepo.read.findByUUID(task2.uuid).value
      val updatedTask3 = fixture.taskRepo.read.findByUUID(task3.uuid).value
      updatedTask1.tags should contain theSameElementsAs List(someTag.uuid, anotherTag.uuid)
      updatedTask2.tags shouldBe Nil
      updatedTask3.tags should contain theSameElementsAs List(anotherTag.uuid)

      And("The journal log should contains the entry that update the task")
      response.success.value.journals.changes.map(_.current.uuid) should contain.allOf(updatedTask1.uuid, updatedTask2.uuid, updatedTask3.uuid)
    }
  }
}
