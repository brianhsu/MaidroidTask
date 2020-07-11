package moe.brianhsu.maidroidtask.usecase

import org.scalatest._
import org.scalatest.featurespec.AnyFeatureSpec

import scala.language.reflectiveCalls
import java.time.LocalDateTime
import java.util.UUID

import moe.brianhsu.maidroidtask.entity._
import moe.brianhsu.maidroidtask.usecase.base.{UseCase, UseCaseExecutor, UseCaseRequest}
import org.scalatest.matchers.should.Matchers

class UseCaseExecutorTest extends AnyFeatureSpec with GivenWhenThen with Matchers with TryValues {

  import Validations._


  Feature("Run UseCase using UseCaseExecutor") {

    info("As a developer,")
    info("I want to execute UseCases using UseCaseExecutor")

    implicit val executor: UseCaseExecutor = new UseCaseExecutor

    Scenario("Run UseCase without any error") {
      Given("A UseCase that should not have any error")
      object DummyRequest extends UseCaseRequest {
        override def uuid: UUID = UUID.randomUUID
      }

      val useCase: UseCase[Int] = new UseCase[Int] {
        override def validations: List[ValidationRules] = Nil
        override def doAction() = 100
        override def journal: Journal = Journal(UUID.randomUUID, UUID.randomUUID, DummyRequest, Nil, LocalDateTime.now)
      }

      When("call execute() for the usecase object")
      val result = useCase.execute()

      Then("it should return Success contains the correct value")
      result.success.value.result shouldBe 100
    }

    Scenario("Run UseCase throw runtime error") {
      Given("A UseCase that throw runtime error in doAction()")

      val useCase: UseCase[Int] = new UseCase[Int] {
        override def validations: List[ValidationRules] = Nil
        override def journal: Journal = null
        override def doAction(): Int = {
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
        override def journal: Journal = null
        override def validations: List[ValidationRules] = createValidator(
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
    implicit object LoggedExecutor extends UseCaseExecutor {
      var loggedJournals: List[Change] = Nil

      override def appendJournals(journals: List[Change]): Unit = {
        this.loggedJournals = journals
      }
    }

    Scenario("Run UseCase with journals") {
      Given("a bunch of journals")

      case object InsertedEntity extends Entity {
        override def uuid: UUID = UUID.randomUUID
      }
      case object UpdatedEntity extends Entity {
        override def uuid: UUID = UUID.randomUUID
      }
      case object DeletedEntity extends Entity {
        override def uuid: UUID = UUID.randomUUID()
      }
      case object DummyRequest extends UseCaseRequest {
        override def uuid: UUID = UUID.randomUUID
      }
      val historyUUID = UUID.fromString("00b1bdbe-e66d-49fb-b7b9-4d74f00dc300")
      val userUUID = UUID.fromString("a45ed52e-6c4f-4b3b-9b75-ddcf4cbdcc82")
      val journalList = List(
        Change(historyUUID, None, InsertedEntity, LocalDateTime.parse("2020-07-01T10:00:00")),
        Change(historyUUID, Some(InsertedEntity), UpdatedEntity, LocalDateTime.parse("2020-07-01T11:00:00")),
        Change(historyUUID, Some(UpdatedEntity), DeletedEntity, LocalDateTime.parse("2020-07-01T11:00:00"))
      )

      And("a use case with journals")
      val useCase: UseCase[Int] = new UseCase[Int] {
        override def journal: Journal = Journal(UUID.randomUUID, userUUID, DummyRequest, journalList, LocalDateTime.now)
        override def validations: List[ValidationRules] = Nil
        override def doAction(): Int = 100
      }

      When("execute with UseCaseExecutor")
      val result = useCase.execute()

      Then("UseCaseExecutor should call appendLogs correctly")
      LoggedExecutor.loggedJournals shouldBe journalList
    }
  }

}
