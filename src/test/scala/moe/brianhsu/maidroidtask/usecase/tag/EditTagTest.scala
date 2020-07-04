package moe.brianhsu.maidroidtask.usecase.tag

import java.time.LocalDateTime
import java.util.UUID

import moe.brianhsu.maidroidtask.entity.{Journal, Tag, UpdateLog}
import moe.brianhsu.maidroidtask.usecase.Validations.{AccessDenied, Duplicated, FailedValidation, NotFound, Required, ValidationErrors}
import moe.brianhsu.maidroidtask.utils.fixture.{BaseFixture, BaseFixtureFeature}

import scala.util.Try

class EditTagFixture extends BaseFixture {
  val otherUserTagUUID = UUID.fromString("341342e6-ee4f-465d-ae7d-4f2f9d33448f")
  val tag1UUID = UUID.fromString("c97675c1-6443-45da-ac91-feb94bf7a185")
  val tag2UUID = UUID.fromString("1c7760e2-be7a-4ba0-83ce-bdfd8a5446f2")
  val otherUserTagName = "OtherUserTag"
  val existTagName = "SomeTag"

  val fixtureCreateTime = LocalDateTime.now

  tagRepo.write.insert(Tag(otherUserTagUUID, otherUser.uuid, otherUserTagName, fixtureCreateTime, fixtureCreateTime))
  tagRepo.write.insert(Tag(tag1UUID, loggedInUser.uuid, "UserTag", fixtureCreateTime, fixtureCreateTime))
  tagRepo.write.insert(Tag(tag2UUID, loggedInUser.uuid, existTagName, fixtureCreateTime, fixtureCreateTime))

  def run(request: EditTag.Request): (Try[Tag], List[Journal]) = {
    val useCase = new EditTag(request)
    (useCase.execute(), useCase.journals)
  }
}

class EditTagTest extends BaseFixtureFeature[EditTagFixture] {

  override protected def createFixture: EditTagFixture = new EditTagFixture

  Feature("Validation before edit") {
    Scenario("Edit a tag that does not exist") { fixture =>
      Given("user request to edit a tag that does not exist")
      val uuidNotExist = UUID.fromString("b7569124-ab03-46fe-b352-1d01df128b64")
      val request = EditTag.Request(fixture.loggedInUser, uuidNotExist, "NewName")

      When("run the use case")
      val (response, _) = fixture.run(request)

      Then("it should NOT pass the validation, and yield NotFound error")
      val exception = response.failure.exception
      exception shouldBe a[ValidationErrors]
      exception.asInstanceOf[ValidationErrors].failedValidations shouldBe List(FailedValidation("uuid", NotFound))
    }

    Scenario("Edit a tag that belongs to others") { fixture =>
      Given("user request to edit a tag that belongs to other user")
      val request = EditTag.Request(fixture.loggedInUser, fixture.otherUserTagUUID, "NewName")

      When("run the use case")
      val (response, _) = fixture.run(request)

      Then("it should NOT pass the validation, and yield AccessDenied error")
      val exception = response.failure.exception
      exception shouldBe a[ValidationErrors]
      exception.asInstanceOf[ValidationErrors].failedValidations shouldBe List(FailedValidation("uuid", AccessDenied))
    }

    Scenario("Edit a tag with new name contains only spaces, tabs and newlines") { fixture =>
      Given("user request edit a tag that name is empty")
      val request = EditTag.Request(fixture.loggedInUser, fixture.tag1UUID, "    \n   \t  ")

      When("run the use case")
      val (response, _) = fixture.run(request)

      Then("it should NOT pass the validation, and yield Required error")
      val exception = response.failure.exception
      exception shouldBe a[ValidationErrors]
      exception.asInstanceOf[ValidationErrors].failedValidations shouldBe List(FailedValidation("name", Required))
    }

    Scenario("Edit a tag, and name has duplication in system with same user") { fixture =>
      Given("user request to edit a tag with name is already in system")
      val request = EditTag.Request(fixture.loggedInUser, fixture.tag1UUID, fixture.existTagName)

      When("run the use case")
      val (response, _) = fixture.run(request)

      Then("it should NOT pass the validation, and yield Duplicate error")
      val exception = response.failure.exception
      exception shouldBe a[ValidationErrors]
      exception.asInstanceOf[ValidationErrors].failedValidations shouldBe List(FailedValidation("name", Duplicated))
    }

    Scenario("Edit a tag, and name has duplication in system with other user") { fixture =>
      Given("user request to edit a tag with name is already in system but is from different user")
      val request = EditTag.Request(fixture.loggedInUser, fixture.tag1UUID, fixture.otherUserTagName)

      When("run the use case")
      val (response, _) = fixture.run(request)

      Then("it should pass the validation")
      val returnedTag = response.success.value
      returnedTag shouldBe a[Tag]
    }

    Scenario("Validation passed") { fixture =>
      Given("user request to edit a tag belongs to him/her with non-empty name")
      val request = EditTag.Request(fixture.loggedInUser, fixture.tag1UUID, "SomeNewName")

      When("run the use case")
      val (response, _) = fixture.run(request)

      Then("it should pass the validation")
      val returnedTag = response.success.value
      returnedTag shouldBe a[Tag]
    }
  }

  Feature("Edit tag in system") {
    Scenario("Update tag name") { fixture =>
      Given("user request to edit a tag to a new tag name")
      val request = EditTag.Request(fixture.loggedInUser, fixture.tag1UUID, "SomeNewName")

      When("run the use case")
      val (response, journals) = fixture.run(request)

      Then("it should return edited tag contains updated information")
      val returnedTag = response.success.value
      inside(returnedTag) { case Tag(uuid, userUUID, name, createTime, updateTime) =>
        uuid shouldBe request.uuid
        userUUID shouldBe request.loggedInUser.uuid
        name shouldBe "SomeNewName"
        createTime shouldBe fixture.fixtureCreateTime
        updateTime shouldBe fixture.generator.currentTime
      }

      And("it should be stored in storage")
      val tagInStorage = fixture.tagRepo.read.findByUUID(request.uuid).value
      tagInStorage shouldBe returnedTag

      And("generate correct journal entry")
      journals shouldBe List(
        UpdateLog(
          fixture.generator.randomUUID, request.loggedInUser.uuid,
          request.uuid, returnedTag, fixture.generator.currentTime
        )
      )
    }
  }
}
