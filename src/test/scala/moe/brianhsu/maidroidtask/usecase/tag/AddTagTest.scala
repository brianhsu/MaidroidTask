package moe.brianhsu.maidroidtask.usecase.tag

import java.util.UUID

import moe.brianhsu.maidroidtask.entity.{Change, Tag}
import moe.brianhsu.maidroidtask.usecase.UseCaseExecutorResult
import moe.brianhsu.maidroidtask.usecase.Validations.{AccessDenied, Duplicated, FailedValidation, NotFound, Required, ValidationErrors}
import moe.brianhsu.maidroidtask.usecase.types.ResultHolder
import moe.brianhsu.maidroidtask.utils.fixture.{BaseFixture, BaseFixtureFeature}

class AddTagFixture extends BaseFixture {
  val uuidInSystem = UUID.fromString("fedc2a03-031c-4c3f-8e8d-176009f5928")
  val otherUserTagUUID = UUID.fromString("8d077394-7e32-4e86-a721-b14bd004f2a8")

  tagRepo.write.insert(Tag(uuidInSystem, loggedInUser.uuid, "ExistTag", None, isTrashed = false, generator.currentTime, generator.currentTime))
  tagRepo.write.insert(Tag(otherUserTagUUID, otherUser.uuid, "OtherUserTag", None, isTrashed = false, generator.currentTime, generator.currentTime))

  def run(request: AddTag.Request): ResultHolder[Tag] = {
    val useCase = new AddTag(request)
    useCase.execute()
  }
}

class AddTagTest extends BaseFixtureFeature[AddTagFixture] {
  override protected def createFixture: AddTagFixture = new AddTagFixture

  Feature("Validation before add tag") {

    info("As a system administrator, I would like the system to prevent")
    info("user add tags that is not valid.")

    Scenario("There is duplicate UUID in system") { fixture =>
      Given("user request to add tag that has duplicate UUID in system")
      val request = AddTag.Request(fixture.loggedInUser, fixture.uuidInSystem, "TagName")

      When("run the use case")
      val response = fixture.run(request)

      Then("it should NOT pass validation and yield Duplicated error")
      response should containsFailedValidation("uuid", Duplicated)
    }

    Scenario("the tag name is empty") { fixture =>
      Given("user request to add tag with tag name only consist of space, newline and tabs")
      val request = AddTag.Request(fixture.loggedInUser, fixture.generator.randomUUID, "    \n \t   ")

      When("run the use case")
      val response = fixture.run(request)

      Then("it should NOT pass validation and yield Required error")
      response should containsFailedValidation("name", Required)
    }

    Scenario("the parent tag UUID is not exist") { fixture =>
      Given("user request to add a tag that the parent tag UUID is not exist")
      val nonExistParentUUID = UUID.fromString("d0729388-7b51-48b9-a671-6b985ad4ae65")
      val request = AddTag.Request(
        fixture.loggedInUser, fixture.generator.randomUUID,
        "ChildTag", Some(nonExistParentUUID)
      )

      When("run the use case")
      val response = fixture.run(request)

      Then("it should NOT pass validation and yield NotFound error")
      response should containsFailedValidation("parentTagUUID", NotFound)
    }

    Scenario("the parent tag UUID is belongs to others") { fixture =>
      Given("user request to add a tag depends on other users tag")
      val request = AddTag.Request(
        fixture.loggedInUser, fixture.generator.randomUUID,
        "ChildTag", Some(fixture.otherUserTagUUID)
      )

      When("run the use case")
      val response = fixture.run(request)

      Then("it should NOT pass validation and yield NotFound error")
      response should containsFailedValidation("parentTagUUID", AccessDenied)
    }

    Scenario("There are same tag name in system for logged in user") { fixture =>
      Given("user request to add tag that has duplicate name in system")
      val request = AddTag.Request(
        fixture.loggedInUser,
        fixture.generator.randomUUID,
        "ExistTag"
      )

      When("run the use case")
      val response = fixture.run(request)

      Then("it should NOT pass validation and yield Duplicate error")
      response should containsFailedValidation("name", Duplicated)
    }

    Scenario("There are same tag name in system for other user") { fixture =>
      Given("user request to add tag that has duplicate name in system, but belongs to other user")
      val request = AddTag.Request(
        fixture.loggedInUser,
        fixture.generator.randomUUID,
        "OtherUserTag"
      )

      When("run the use case")
      val response = fixture.run(request)

      Then("it should NOT pass validation and yield Duplicate error")
      response.success.value.result shouldBe a[Tag]
    }

    Scenario("There is same tag name, but has been delete") { fixture =>
      Given("user has a deleted tag")
      val deletedTag = fixture.createTag(fixture.loggedInUser, "TagName", isTrashed = true)

      And("user request to add a tag has same name")
      val request = AddTag.Request(
        fixture.loggedInUser,
        fixture.generator.randomUUID,
        "TagName"
      )

      When("run the use case")
      val response = fixture.run(request)

      Then("it should pass the validation")
      response.success.value.result shouldBe a[Tag]
    }

    Scenario("Validation passed") { fixture =>
      Given("user request to add tag with non-empty name")
      val request = AddTag.Request(fixture.loggedInUser, fixture.generator.randomUUID, "TagName")

      When("run the use case")
      val response = fixture.run(request)

      Then("it should pass the validation")
      response.success.value.result shouldBe a[Tag]
    }
  }

  Feature("Add tag to system") {

    info("As a user, I would like to add tag to the system")

    Scenario("Add to storage") { fixture =>
      Given("user request to add tag")
      val newTagUUID = fixture.generator.randomUUID
      val parentTagUUID = Some(fixture.uuidInSystem)
      val request = AddTag.Request(fixture.loggedInUser, newTagUUID, "TagName", parentTagUUID)

      When("run the use case")
      val response = fixture.run(request)

      Then("the returned tag should contains correct information")
      val returnedTag = response.success.value.result
      inside(returnedTag) { case Tag(uuid, userUUID, name, parentTagUUID, isDeleted, createTime, updateTime) =>
        uuid shouldBe request.uuid
        userUUID shouldBe request.loggedInUser.uuid
        name shouldBe request.name
        parentTagUUID.value shouldBe fixture.uuidInSystem
        isDeleted shouldBe false
        createTime shouldBe fixture.generator.currentTime
        updateTime shouldBe fixture.generator.currentTime
      }

      And("it should stored in storage")
      val tagInStorage = fixture.tagRepo.read.findByUUID(returnedTag.uuid).value
      tagInStorage shouldBe returnedTag

      And("generate correct journal entry")
      response.success.value.journals.changes should contain theSameElementsInOrderAs List(
        Change(fixture.generator.randomUUID, None, returnedTag, fixture.generator.currentTime)
      )
    }
  }
}
