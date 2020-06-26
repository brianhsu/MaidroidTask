package moe.brianhsu.maidroidtask.usecase

import org.scalatest.featurespec.AnyFeatureSpec
import org.scalatest._
import org.scalatest.matchers.should.Matchers._
import scala.util._
import org.scalatest.TryValues._

class UseCaseExecutorSpec extends AnyFeatureSpec with GivenWhenThen {

  import Validations._

  implicit val executor = new BaseUseCaseExecutor

  Feature("Run UseCase using UseCaseExecutor") {
    info("As a developer,")
    info("I want to execute UseCases using UseCaseExecutor")

    Scenario("Run UseCase without any error") {
      Given("A UseCase that should not have any error")

      val useCase = new UseCase[Int] {
        override protected def validations = Nil
        override def doAction() = 100
      }

      When("call execute() for the usecase object")
      val result = useCase.execute()

      Then("it should return Success contains the correct value")
      result.success.value shouldBe 100
    }

    Scenario("Run UseCase throw runtime error") {
      Given("A UseCase that throw runtime error in doAction()")

      val useCase = new UseCase[Int] {
        override protected def validations = Nil
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
        override protected def validations = createValidator(
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
}
