package moe.brianhsu.maidroidtask.usecase.tag

import java.time.LocalDateTime
import java.util.UUID

import moe.brianhsu.maidroidtask.entity.{Journal, Tag, TrashLog}
import moe.brianhsu.maidroidtask.usecase.Validations.{AccessDenied, FailedValidation, HasChildren, NotFound, ValidationErrors}
import moe.brianhsu.maidroidtask.utils.fixture.{BaseFixture, BaseFixtureFeature}

import scala.util.Try

class TrashTagFixture extends BaseFixture {

  val fixtureCreateTime = LocalDateTime.now

  val otherUserTag = Tag(UUID.randomUUID(), otherUser.uuid, "OtherUserTag", None, isTrashed = false, fixtureCreateTime, fixtureCreateTime)
  val parentTag = Tag(UUID.randomUUID(), loggedInUser.uuid, "ParentTag", None, isTrashed = false, fixtureCreateTime, fixtureCreateTime)
  val childTag = Tag(UUID.randomUUID(), loggedInUser.uuid, "ChildTag", Some(parentTag.uuid), isTrashed = false, fixtureCreateTime, fixtureCreateTime)

  tagRepo.write.insert(otherUserTag)
  tagRepo.write.insert(parentTag)
  tagRepo.write.insert(childTag)

  def run(request: TrashTag.Request): (Try[Tag], List[Journal]) = {
    val useCase = new TrashTag(request)
    (useCase.execute(), useCase.journals)
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
      val (response, _) = fixture.run(request)

      Then("it should NOT pass the validation and yield NotFound error")
      response should containsFailedValidation("uuid", NotFound)
    }

    Scenario("Trash tag belongs to others") { fixture =>
      Given("user request to trash a tag belongs to other user")
      val request = TrashTag.Request(fixture.loggedInUser, fixture.otherUserTag.uuid)

      When("run the use case")
      val (response, _) = fixture.run(request)

      Then("it should NOT pass the validation and yield AccessDenied error")
      response should containsFailedValidation("uuid", AccessDenied)
    }

    Scenario("Trash tag that has child tags") { fixture =>
      Given("user request to trash a tag that still has child tag")
      val request = TrashTag.Request(fixture.loggedInUser, fixture.parentTag.uuid)

      When("run the use case")
      val (response, _) = fixture.run(request)

      Then("it should NOT pass the validation and yield HasChildren error")
      response should containsFailedValidation("uuid", HasChildren)
    }

    Scenario("Trash tag belongs to logged in user") { fixture =>
      Given("user request to trash a tag belongs to them self")
      val request = TrashTag.Request(fixture.loggedInUser, fixture.childTag.uuid)

      When("run the use case")
      val (response, _) = fixture.run(request)

      Then("it should pass the validation")
      response.success.value shouldBe a[Tag]
    }
  }

  Feature("Trash the tag in storage") {
    Scenario("Trash the tag successfully") { fixture =>
      Given("user request to trash a tag belongs to him/her")
      val request = TrashTag.Request(fixture.loggedInUser, fixture.childTag.uuid)

      When("run the use case")
      val (response, journals) = fixture.run(request)

      Then("it should returned trashed tag")
      val returnedTag = response.success.value
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
      journals shouldBe List(
        TrashLog(
          fixture.generator.randomUUID,
          request.loggedInUser.uuid,
          request.uuid,
          tagInStorage,
          fixture.generator.currentTime
        )
      )
    }
  }
}
