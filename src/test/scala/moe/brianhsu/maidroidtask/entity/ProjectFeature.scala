package moe.brianhsu.maidroidtask.entity

import moe.brianhsu.maidroidtask.utils.fixture.{BaseFixture, BaseFixtureFeature}

class ProjectFixture extends BaseFixture

class ProjectFeature extends BaseFixtureFeature[ProjectFixture] {
  override protected def createFixture: ProjectFixture = new ProjectFixture

  Feature("Circular dependency detection") {
    Scenario("The current project has no parent project") { fixture =>
      implicit val projectRepo = fixture.projectRepo

      Given("a project dependency looks like the following")
      info("    projectA <-- projectB <-- projectC <-- projectD")
      val projectA = fixture.createProject(fixture.loggedInUser, "ProjectA")
      val projectB = fixture.createProject(fixture.loggedInUser, "ProjectB", Some(projectA.uuid))
      val projectC = fixture.createProject(fixture.loggedInUser, "ProjectC", Some(projectB.uuid))
      val projectD = fixture.createProject(fixture.loggedInUser, "ProjectD", Some(projectC.uuid))

      And("a current project named projectE without any parent project itself")
      val projectE = fixture.createProject(fixture.loggedInUser, "ProjectE")

      When("check if assign projectE as parent project of those project will create loop")
      Then("it should return false for all cases")
      forAll(List(projectA, projectB, projectC, projectD)) { project =>
        project.hasLoopsWith(projectE.uuid) shouldBe false
      }
    }

    Scenario("Project depends on each other") { fixture =>
      implicit val projectRepo = fixture.projectRepo

      Given("projectB has a parent project named projectA")
      val projectA = fixture.createProject(fixture.loggedInUser, "ProjectA")
      val projectB = fixture.createProject(fixture.loggedInUser, "ProjectB", Some(projectA.uuid))

      When("check if add projectB as parent project of projectA will create loop")
      val hasLoop = projectA.hasLoopsWith(projectB.uuid)

      Then("it should return true")
      hasLoop shouldBe true
    }

    Scenario("Root parent project add to project outside of dependency") { fixture =>
      implicit val projectRepo = fixture.projectRepo

      Given("A project dependency looks like the following")
      info("    projectA <-- projectB <-- projectC <-- projectD")
      val projectA = fixture.createProject(fixture.loggedInUser, "ProjectA")
      val projectB = fixture.createProject(fixture.loggedInUser, "ProjectB", Some(projectA.uuid))
      val projectC = fixture.createProject(fixture.loggedInUser, "ProjectC", Some(projectB.uuid))
      val projectD = fixture.createProject(fixture.loggedInUser, "ProjectD", Some(projectC.uuid))

      And("another dependency looks like the following")
      info("    projectE <-- projectF <-- projectG")
      val projectE = fixture.createProject(fixture.loggedInUser, "ProjectE")
      val projectF = fixture.createProject(fixture.loggedInUser, "ProjectF", Some(projectE.uuid))
      val projectG = fixture.createProject(fixture.loggedInUser, "ProjectG", Some(projectF.uuid))

      When("check if projectA be parent of project E, F, G will create loop")
      Then("it should all return false")
      forAll(List(projectE, projectF, projectG)) { project =>
        project.hasLoopsWith(projectA.uuid) shouldBe false
      }
    }

    Scenario("Creating loop for project dependency") { fixture =>
      implicit val projectRepo = fixture.projectRepo

      Given("A project dependency looks like the following")
      info("    projectA <-- projectB <-- projectC <-- projectD")
      val projectA = fixture.createProject(fixture.loggedInUser, "ProjectA")
      val projectB = fixture.createProject(fixture.loggedInUser, "ProjectB", Some(projectA.uuid))
      val projectC = fixture.createProject(fixture.loggedInUser, "ProjectC", Some(projectB.uuid))
      val projectD = fixture.createProject(fixture.loggedInUser, "ProjectD", Some(projectC.uuid))

      When("check if assign projectB as parent of projectD will create loop")
      info("    projectA <-- projectB <-- projectC <-- projectD")
      info("                    |                          ^")
      info("                    +--------------------------+")
      val hasLoop = projectD.hasLoopsWith(projectB.uuid)

      Then("it should return true")
      hasLoop shouldBe true
    }
  }
}
