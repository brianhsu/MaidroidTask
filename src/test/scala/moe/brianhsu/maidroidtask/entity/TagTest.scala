package moe.brianhsu.maidroidtask.entity

import moe.brianhsu.maidroidtask.gateway.repo.TagReadable
import moe.brianhsu.maidroidtask.utils.fixture.{BaseFixture, BaseFixtureFeature}

class TagFixture extends BaseFixture

class TagTest extends BaseFixtureFeature[TagFixture] {
  override protected def createFixture: TagFixture = new TagFixture

  Feature("Circular dependency detection") {
    Scenario("The current tag has no parent tag") { fixture =>
      implicit val tagRepo: TagReadable = fixture.tagRepo.read

      Given("a tag dependency looks like the following")
      info("    tagA <-- tagB <-- tagC <-- tagD")
      val tagA = fixture.createTag(fixture.loggedInUser, "TagA")
      val tagB = fixture.createTag(fixture.loggedInUser, "TagB", Some(tagA.uuid))
      val tagC = fixture.createTag(fixture.loggedInUser, "TagC", Some(tagB.uuid))
      val tagD = fixture.createTag(fixture.loggedInUser, "TagD", Some(tagC.uuid))

      And("a current tag named tagE without any parent tag itself")
      val tagE = fixture.createTag(fixture.loggedInUser, "TagE")

      When("check if assign tagE as parent tag of those tag will create loop")
      Then("it should return false for all cases")
      forAll(List(tagA, tagB, tagC, tagD)) { tag =>
        tag.hasLoopsWith(tagE.uuid) shouldBe false
      }
    }

    Scenario("Tag depends on each other") { fixture =>
      implicit val tagRepo: TagReadable = fixture.tagRepo.read

      Given("tagB has a parent tag named tagA")
      val tagA = fixture.createTag(fixture.loggedInUser, "TagA")
      val tagB = fixture.createTag(fixture.loggedInUser, "TagB", Some(tagA.uuid))

      When("check if add tagB as parent tag of tagA will create loop")
      val hasLoop = tagA.hasLoopsWith(tagB.uuid)

      Then("it should return true")
      hasLoop shouldBe true
    }

    Scenario("Root parent tag add to tag outside of dependency") { fixture =>
      implicit val tagRepo: TagReadable = fixture.tagRepo.read

      Given("A tag dependency looks like the following")
      info("    tagA <-- tagB <-- tagC <-- tagD")
      val tagA = fixture.createTag(fixture.loggedInUser, "TagA")
      val tagB = fixture.createTag(fixture.loggedInUser, "TagB", Some(tagA.uuid))
      val tagC = fixture.createTag(fixture.loggedInUser, "TagC", Some(tagB.uuid))
      val tagD = fixture.createTag(fixture.loggedInUser, "TagD", Some(tagC.uuid))

      And("another dependency looks like the following")
      info("    tagE <-- tagF <-- tagG")
      val tagE = fixture.createTag(fixture.loggedInUser, "TagE")
      val tagF = fixture.createTag(fixture.loggedInUser, "TagF", Some(tagE.uuid))
      val tagG = fixture.createTag(fixture.loggedInUser, "TagG", Some(tagF.uuid))

      When("check if tagA be parent of tag E, F, G will create loop")
      Then("it should all return false")
      forAll(List(tagE, tagF, tagG)) { tag =>
        tag.hasLoopsWith(tagA.uuid) shouldBe false
      }
    }

    Scenario("Creating loop for tag dependency") { fixture =>
      implicit val tagRepo: TagReadable = fixture.tagRepo.read

      Given("A tag dependency looks like the following")
      info("    tagA <-- tagB <-- tagC <-- tagD")
      val tagA = fixture.createTag(fixture.loggedInUser, "TagA")
      val tagB = fixture.createTag(fixture.loggedInUser, "TagB", Some(tagA.uuid))
      val tagC = fixture.createTag(fixture.loggedInUser, "TagC", Some(tagB.uuid))
      val tagD = fixture.createTag(fixture.loggedInUser, "TagD", Some(tagC.uuid))

      When("check if assign tagB as parent of tagD will create loop")
      info("    tagA <-- tagB <-- tagC <-- tagD")
      info("                    |                          ^")
      info("                    +--------------------------+")
      val hasLoop = tagD.hasLoopsWith(tagB.uuid)

      Then("it should return true")
      hasLoop shouldBe true
    }
  }
}
