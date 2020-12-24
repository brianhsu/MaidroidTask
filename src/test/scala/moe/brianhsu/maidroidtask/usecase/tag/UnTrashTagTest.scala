package moe.brianhsu.maidroidtask.usecase.tag

import java.util.UUID

import moe.brianhsu.maidroidtask.entity.{Change, Journal, Tag}
import moe.brianhsu.maidroidtask.usecase.Validations.{AccessDenied, NotFound, NotTrashed, ParentIsTrashed}
import moe.brianhsu.maidroidtask.usecase.base.types.ResultHolder
import moe.brianhsu.maidroidtask.utils.fixture.{BaseFixture, BaseFixtureFeature}

class UnTrashTagFixture extends BaseFixture {
  def run(request: UnTrashTag.Request): ResultHolder[Tag] = {
    val useCase = new UnTrashTag(request)
    useCase.execute()
  }
}
class UnTrashTagTest extends BaseFixtureFeature[UnTrashTagFixture]{
  override protected def createFixture: UnTrashTagFixture = new UnTrashTagFixture

  Feature("Validation before untrash") {
    Scenario("The tag uuid is not exist") { fixture =>
      Given("user request to un trash a tag is not exist")
      val nonExistTagUUID = UUID.randomUUID
      val request = UnTrashTag.Request(fixture.loggedInUser, nonExistTagUUID)

      When("run the use case")
      val response = fixture.run(request)

      Then("it should NOT pass validation and yield NotFound error")
      response should containsFailedValidation("uuid", NotFound)
    }

    Scenario("The tag is belongs to others") { fixture =>
      Given("user request to un trash a tag belongs to others")
      val tag = fixture.createTag(fixture.otherUser, "OtherUserTag", isTrashed = true)
      val request = UnTrashTag.Request(fixture.loggedInUser, tag.uuid)

      When("run the use case")
      val response = fixture.run(request)

      Then("it should NOT pass validation and yield AccessDeined error")
      response should containsFailedValidation("uuid", AccessDenied)
    }

    Scenario("The tag is no trashed") { fixture =>
      Given("user request to un trash a non-trashed tag")
      val tag = fixture.createTag(fixture.loggedInUser, "Tag")
      val request = UnTrashTag.Request(fixture.loggedInUser, tag.uuid)

      When("run the use case")
      val response = fixture.run(request)

      Then("it should NOT pass validation and yield NotTrashed error")
      response should containsFailedValidation("uuid", NotTrashed)
    }

    Scenario("The parent tag is still trashed") { fixture =>
      Given("A trashed tag with trashed parent tag")
      val parentTag = fixture.createTag(fixture.loggedInUser, "Parent", isTrashed = true)
      val tag = fixture.createTag(fixture.loggedInUser, "Tag", Some(parentTag.uuid), isTrashed = true)

      And("user request to un trash that tag")
      val request = UnTrashTag.Request(fixture.loggedInUser, tag.uuid)

      When("run the use case")
      val response = fixture.run(request)

      Then("it should NOT pass validation and yield ParentIsTrashed error")
      response should containsFailedValidation("uuid", ParentIsTrashed)
    }

  }

  Feature("Update the status and store it to storage") {
    Scenario("Un trash a tag without parent tag") { fixture =>
      Given("user request to un trash a tag without parent tag")
      val tag = fixture.createTag(fixture.loggedInUser, "Tag", isTrashed = true)
      val unTrashRequest = UnTrashTag.Request(fixture.loggedInUser, tag.uuid)

      When("run the use case")
      val response = fixture.run(unTrashRequest)

      Then("it should return untrashed tag")
      val unTrashdTag = response.success.value.result
      unTrashdTag.isTrashed shouldBe false
      unTrashdTag.updateTime shouldBe fixture.generator.currentTime

      And("stored in storage")
      val tagInStorage = fixture.tagRepo.read.findByUUID(unTrashdTag.uuid).value
      tagInStorage shouldBe unTrashdTag

      And("generate the correct log")
      inside(response.success.value.journals) { case Journal(journalUUID, userUUID, request, changes, timestamp) =>
        journalUUID shouldBe fixture.generator.randomUUID
        userUUID shouldBe fixture.loggedInUser.uuid
        request shouldBe unTrashRequest
        changes should contain theSameElementsAs List(
          Change(fixture.generator.randomUUID, Some(tag), tagInStorage, fixture.generator.currentTime)
        )
        timestamp shouldBe fixture.generator.currentTime
      }
    }

    Scenario("Un trash a tag with parent tag") { fixture =>
      Given("user request to un trash a tag has parent tag")
      val parentTag = fixture.createTag(fixture.loggedInUser, "Parent")
      val tag = fixture.createTag(fixture.loggedInUser, "Tag", Some(parentTag.uuid), isTrashed = true)
      val unTrashRequest = UnTrashTag.Request(fixture.loggedInUser, tag.uuid)

      When("run the use case")
      val response = fixture.run(unTrashRequest)

      Then("it should return untrashed tag")
      val unTrashdTag = response.success.value.result
      unTrashdTag.isTrashed shouldBe false
      unTrashdTag.updateTime shouldBe fixture.generator.currentTime

      And("stored in storage")
      val tagInStorage = fixture.tagRepo.read.findByUUID(unTrashdTag.uuid).value
      tagInStorage shouldBe unTrashdTag

      And("generate the correct log")
      inside(response.success.value.journals) { case Journal(journalUUID, userUUID, request, changes, timestamp) =>
        journalUUID shouldBe fixture.generator.randomUUID
        userUUID shouldBe fixture.loggedInUser.uuid
        request shouldBe unTrashRequest
        changes should contain theSameElementsAs List(
          Change(fixture.generator.randomUUID, Some(tag), tagInStorage, fixture.generator.currentTime)
        )
        timestamp shouldBe fixture.generator.currentTime
      }
    }
  }
}
