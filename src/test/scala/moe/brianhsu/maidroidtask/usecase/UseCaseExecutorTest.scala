package moe.brianhsu.maidroidtask.usecase

import org.scalatest._
import org.scalatest.featurespec.AnyFeatureSpec
import org.scalatest.matchers.should.Matchers._
import org.scalatest.TryValues._

import scala.language.reflectiveCalls
import scala.util._

import java.time.LocalDateTime
import java.util.UUID

import moe.brianhsu.maidroidtask.entity._

class UseCaseExecutorSpec extends AnyFeatureSpec with GivenWhenThen {

  import Validations._


  Feature("Run UseCase using UseCaseExecutor") {

    info("As a developer,")
    info("I want to execute UseCases using UseCaseExecutor")

    implicit val executor = new UseCaseExecutor

    Scenario("Run UseCase without any error") {
      Given("A UseCase that should not have any error")

      val useCase = new UseCase[Int] {
        override def validations = Nil
        override def doAction() = 100
        override def journals = Nil
      }

      When("call execute() for the usecase object")
      val result = useCase.execute()

      Then("it should return Success contains the correct value")
      result.success.value shouldBe 100
    }

    Scenario("Run UseCase throw runtime error") {
      Given("A UseCase that throw runtime error in doAction()")

      val useCase = new UseCase[Int] {
        override def validations = Nil
        override def journals = Nil
        override def doAction() = {
          throw new Exception("Error!")
        }
      }

      When("call execute() for the usecase object")
      val result = useCase.execute()

      Then("it will return Failure with correct exception")
      result.failure.exception should have message "Error!"
    }

    Scenario("Run UseCase that has validation errors") {
      Given("A UseCase that have validation errors")

      val useCase = new UseCase[Int] {
        override def journals = Nil
        override def validations = createValidator(
          "someValue", 100, 
          (x: Int) => if (x < 0) None else Some(IsMalformed),
          (x: Int) => if (x > 1000) None else Some(AccessDenied)
        )
        override def doAction() = 100
      }

      When("call execute() for the usecase object")
      val result = useCase.execute()

      Then("the validation result should be empty since it does not run validate")
      result.failure.exception should have message "Validate failed."
      result.failure.exception.asInstanceOf[ValidationErrors].failedValidations should contain theSameElementsInOrderAs List(    
         FailedValidation("someValue", IsMalformed),
         FailedValidation("someValue", AccessDenied)
      )
    }
  }

  Feature("Run UseCase with UseCaseExecutor to log journal entry") {
    implicit val executor = new UseCaseExecutor {
      var loggedJournals: List[Journal] = Nil

      override def appendJournals(journals: List[Journal]) = {
        this.loggedJournals = journals
      }
    }

    Scenario("Run UseCase with journals") {
      Given("a bunch of journals")
      case object InsertedEntity extends Entity
      case object UpdatedEntity extends Entity
      case object DeletedEntity extends Entity
      val historyUUID = UUID.fromString("00b1bdbe-e66d-49fb-b7b9-4d74f00dc300")
      val insertUUID = UUID.fromString("b9a3dbb4-262d-42f4-892e-62471f012512")
      val updateUUID = UUID.fromString("8aea921b-72c6-44b1-8faf-8d4d579b52a1")
      val deleteUUID = UUID.fromString("dd69a00c-e893-433b-9394-77b332aa4591")
      val userUUID = UUID.fromString("a45ed52e-6c4f-4b3b-9b75-ddcf4cbdcc82")
      val journalList = List(
        InsertLog(historyUUID, userUUID, insertUUID, InsertedEntity, LocalDateTime.parse("2020-07-01T10:00:00")),
        UpdateLog(historyUUID, userUUID, updateUUID, UpdatedEntity, LocalDateTime.parse("2020-07-01T11:00:00")),
        DeleteLog(historyUUID, userUUID, deleteUUID, DeletedEntity, LocalDateTime.parse("2020-07-01T11:00:00"))
      )

      And("a use case with journals")
      val useCase = new UseCase[Int] {
        override def validations = Nil
        override def doAction() = 100
        override def journals: List[Journal] = journalList
      }

      When("execute with UseCaseExecutor")
      val result = useCase.execute()

      Then("UseCaseExecutor should call appendLogs correctly")
      executor.loggedJournals shouldBe journalList
    }
  }

}
