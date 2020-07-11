package moe.brianhsu.maidroidtask.usecase.tag

import java.time.LocalDateTime
import java.util.UUID

import moe.brianhsu.maidroidtask.entity.{Change, Tag}
import moe.brianhsu.maidroidtask.usecase.Validations.{AccessDenied, AlreadyTrashed, Duplicated, NotFound, Required}
import moe.brianhsu.maidroidtask.usecase.base.types.ResultHolder
import moe.brianhsu.maidroidtask.utils.fixture.{BaseFixture, BaseFixtureFeature}

class EditTagFixture extends BaseFixture {
  val otherUserTagUUID = UUID.fromString("341342e6-ee4f-465d-ae7d-4f2f9d33448f")
  val tag1UUID = UUID.fromString("c97675c1-6443-45da-ac91-feb94bf7a185")
  val tag2UUID = UUID.fromString("1c7760e2-be7a-4ba0-83ce-bdfd8a5446f2")
  val otherUserTagName = "OtherUserTag"
  val existTagName = "SomeTag"

  val fixtureCreateTime = LocalDateTime.now

  val otherUserTag = tagRepo.write.insert(Tag(otherUserTagUUID, otherUser.uuid, otherUserTagName, None, isTrashed = false, fixtureCreateTime, fixtureCreateTime))
  val tag1 = tagRepo.write.insert(Tag(tag1UUID, loggedInUser.uuid, "UserTag", None, isTrashed = false, fixtureCreateTime, fixtureCreateTime))
  val tag2 = tagRepo.write.insert(Tag(tag2UUID, loggedInUser.uuid, existTagName, None, isTrashed = false, fixtureCreateTime, fixtureCreateTime))

  def run(request: EditTag.Request): ResultHolder[Tag] = {
    val useCase = new EditTag(request)
    useCase.execute()
  }
}

class EditTagTest extends BaseFixtureFeature[EditTagFixture] {

  override protected def createFixture: EditTagFixture = new EditTagFixture

  Feature("Validation before edit") {
    Scenario("Edit a tag that does not exist") { fixture =>
      Given("user request to edit a tag that does not exist")
      val uuidNotExist = UUID.fromString("b7569124-ab03-46fe-b352-1d01df128b64")
      val request = EditTag.Request(fixture.loggedInUser, uuidNotExist, Some("NewName"))

      When("run the use case")
      val response = fixture.run(request)

      Then("it should NOT pass the validation, and yield NotFound error")
      response should containsFailedValidation("uuid", NotFound)
    }

    Scenario("Edit a tag that belongs to others") { fixture =>
      Given("user request to edit a tag that belongs to other user")
      val request = EditTag.Request(fixture.loggedInUser, fixture.otherUserTagUUID, Some("NewName"))

      When("run the use case")
      val response = fixture.run(request)

      Then("it should NOT pass the validation, and yield AccessDenied error")
      response should containsFailedValidation("uuid", AccessDenied)
    }

    Scenario("Edit a tag with new name contains only spaces, tabs and newlines") { fixture =>
      Given("user request edit a tag that name is empty")
      val request = EditTag.Request(fixture.loggedInUser, fixture.tag1UUID, Some("    \n   \t  "))

      When("run the use case")
      val response = fixture.run(request)

      Then("it should NOT pass the validation, and yield Required error")
      response should containsFailedValidation("name", Required)
    }

    Scenario("Edit a tag, and name has duplication in system with same user") { fixture =>
      Given("user request to edit a tag with name is already in system")
      val request = EditTag.Request(fixture.loggedInUser, fixture.tag1UUID, Some(fixture.existTagName))

      When("run the use case")
      val response = fixture.run(request)

      Then("it should NOT pass the validation, and yield Duplicate error")
      response should containsFailedValidation("name", Duplicated)

    }

    Scenario("the parent tag UUID is not exist") { fixture =>
      Given("user request to edit a tag that new parent tag UUID is not exist")
      val nonExistParentUUID = UUID.fromString("ad8b33c9-5f3e-485e-9eca-65bd6a450be3")
      val request = EditTag.Request(fixture.loggedInUser, fixture.tag1UUID, parentTagUUID = Some(Some(nonExistParentUUID)))

      When("run the use case")
      val response = fixture.run(request)

      Then("it should NOT pass validation and yield NotFound error")
      response should containsFailedValidation("parentTagUUID", NotFound)
    }

    Scenario("the parent tag UUID is belongs to other user") { fixture =>
      Given("user request to edit a tag that new parent tag UUID is belongs to other user")
      val request = EditTag.Request(fixture.loggedInUser, fixture.tag1UUID, parentTagUUID = Some(Some(fixture.otherUserTagUUID)))

      When("run the use case")
      val response = fixture.run(request)

      Then("it should NOT pass validation and yield NotFound error")
      response should containsFailedValidation("parentTagUUID", AccessDenied)
    }

    Scenario("The tag is already trashed") { fixture =>
      Given("user request to edit a tag that is already trashed")
      val trashedTag = fixture.createTag(fixture.loggedInUser, "TrashedTag", isTrashed = true)
      val request = EditTag.Request(fixture.loggedInUser, trashedTag.uuid)

      When("run the use case")
      val response = fixture.run(request)

      Then("it should NOT pass validation and yield AlreadyTrashed error")
      response should containsFailedValidation("uuid", AlreadyTrashed)
    }

    Scenario("The parent tag is already trashed") { fixture =>
      Given("user request to assign new parent tag that is already trashed")
      val trashedTag = fixture.createTag(fixture.loggedInUser, "TrashedTag", isTrashed = true)
      val tagToEdit = fixture.createTag(fixture.loggedInUser, "Tag to edit")
      val request = EditTag.Request(fixture.loggedInUser, tagToEdit.uuid, parentTagUUID = Some(Some(trashedTag.uuid)))

      When("run the use case")
      val response = fixture.run(request)

      Then("it should NOT pass validation and yield AlreadyTrashed error")
      response should containsFailedValidation("parentTagUUID", AlreadyTrashed)
    }

    Scenario("Edit a tag, and name has duplication in system with other user") { fixture =>
      Given("user request to edit a tag with name is already in system but is from different user")
      val request = EditTag.Request(fixture.loggedInUser, fixture.tag1UUID, Some(fixture.otherUserTagName))

      When("run the use case")
      val response = fixture.run(request)

      Then("it should pass the validation")
      val returnedTag = response.success.value.result
      returnedTag shouldBe a[Tag]
    }

    Scenario("Validation passed") { fixture =>
      Given("user request to edit a tag belongs to him/her with non-empty name")
      val request = EditTag.Request(fixture.loggedInUser, fixture.tag1UUID, Some("SomeNewName"))

      When("run the use case")
      val response = fixture.run(request)

      Then("it should pass the validation")
      val returnedTag = response.success.value.result
      returnedTag shouldBe a[Tag]
    }
  }

  Feature("Edit tag in system") {
    Scenario("Update tag name") { fixture =>
      Given("user request to edit a tag to a new tag name")
      val request = EditTag.Request(fixture.loggedInUser, fixture.tag1UUID, Some("SomeNewName"))

      When("run the use case")
      val response = fixture.run(request)

      Then("it should return edited tag contains updated information")
      val returnedTag = response.success.value.result
      inside(returnedTag) { case Tag(uuid, userUUID, name, parentTagUUID, isDeleted, createTime, updateTime) =>
        uuid shouldBe request.uuid
        userUUID shouldBe request.loggedInUser.uuid
        name shouldBe "SomeNewName"
        parentTagUUID shouldBe None
        isDeleted shouldBe false
        createTime shouldBe fixture.fixtureCreateTime
        updateTime shouldBe fixture.generator.currentTime
      }

      And("it should be stored in storage")
      val tagInStorage = fixture.tagRepo.read.findByUUID(request.uuid).value
      tagInStorage shouldBe returnedTag

      And("generate correct journal entry")
      response.success.value.journals.changes shouldBe List(
        Change(fixture.generator.randomUUID, Some(fixture.tag1), returnedTag, fixture.generator.currentTime)
      )
    }

    Scenario("Update parent tag") { fixture =>
      Given("user request to edit a tag to a new parent")
      val request = EditTag.Request(
        fixture.loggedInUser,
        fixture.tag1UUID,
        parentTagUUID = Some(Some(fixture.tag2UUID))
      )

      When("run the use case")
      val response = fixture.run(request)

      Then("it should return edited tag contains updated information")
      val returnedTag = response.success.value.result
      inside(returnedTag) { case Tag(uuid, userUUID, name, parentTagUUID, isDeleted, createTime, updateTime) =>
        uuid shouldBe request.uuid
        userUUID shouldBe request.loggedInUser.uuid
        name shouldBe "UserTag"
        isDeleted shouldBe false
        parentTagUUID shouldBe Some(fixture.tag2UUID)
        createTime shouldBe fixture.fixtureCreateTime
        updateTime shouldBe fixture.generator.currentTime
      }

      And("it should be stored in storage")
      val tagInStorage = fixture.tagRepo.read.findByUUID(request.uuid).value
      tagInStorage shouldBe returnedTag

      And("generate correct journal entry")
      response.success.value.journals.changes shouldBe List(
        Change(fixture.generator.randomUUID, Some(fixture.tag1), returnedTag, fixture.generator.currentTime)
      )
    }
  }
}
