package moe.brianhsu.maidroidtask.usecase

import org.scalatest.featurespec.AnyFeatureSpec
import org.scalatest._
import org.scalatest.matchers.should.Matchers._
import scala.language.reflectiveCalls
import moe.brianhsu.maidroidtask.entity._

class DoNothingUseCase extends UseCase[Unit] {
  override def groupedJournal: GroupedJournal = null
  override def validations: List[Validations.ValidationRules] = Nil
  override def doAction(): Unit = {}
}

class UseCaseValidationSpec extends AnyFeatureSpec with GivenWhenThen {

  import Validations._

  Feature("Validate request") {
    info("As a developer,")
    info("I want to able to validate requests in UseCases before execute")

    Scenario("The validation rule is empty") {
      Given("A UseCase object without validation rule")
      val useCase = new DoNothingUseCase

      When("call validate() for the usecase object")
      val validationResult = useCase.validate()

      Then("the validation result should be empty since it does not run validate")
      validationResult shouldBe empty
    }

    Scenario("There is one validation, and the validation passed") {
      Given("A UseCase object with validation rules")
      val useCase = new DoNothingUseCase {
        var isValidated = false
        val value: Int = 10

        def checkValueLargeThenZero(n: Int): Option[ErrorDescription] = {
          isValidated = true
          if (n > 0) None else Some(IsMalformed)
        }

        override def validations = createValidator(fieldName = "value", value, checkValueLargeThenZero)
      }

      Then("it should not run any valiadtion before validate()")
      useCase.isValidated shouldBe false

      And("when call validate() for the usecase object")
      val validationResult = useCase.validate()

      Then("it should run validation")
      useCase.isValidated shouldBe true

      And("it should return empty list since it will pass the validate")
      validationResult shouldBe empty
    }

    Scenario("There is one validation, and the validation failed") {
      Given("A UseCase object with validation rules")
      val useCase = new DoNothingUseCase {
        var isValidated = false
        val value: Int = 5 

        def checkValueLargeThenTen(n: Int): Option[ErrorDescription] = {
          isValidated = true
          if (n > 10) None else Some(IsMalformed)
        }

        override def validations = createValidator(fieldName = "value", value, checkValueLargeThenTen)
      }


      Then("it should not run any valiadtion before validate()")
      useCase.isValidated shouldBe false

      And("when call validate() for the usecase object")
      val validationResult = useCase.validate()

      Then("it should run validation")
      useCase.isValidated shouldBe true

      And("it should return list contains FailedValidation")
      validationResult should not be empty
      validationResult should contain theSameElementsInOrderAs List(FailedValidation("value", IsMalformed))
    }

    Scenario("There are multiple validations, and the validation pass") {
      Given("A UseCase object with validation rules")
      val useCase = new DoNothingUseCase {
        val value: Int = 100

        def largeThenTen(n: Int): Option[ErrorDescription] = if (n > 10) None else Some(IsMalformed)
        def largeThenZero(n: Int): Option[ErrorDescription] = if (n > 0) None else Some(IsMalformed)

        override def validations = createValidator(fieldName = "value", value, largeThenTen, largeThenZero)
      }

      When("when call validate() for the usecase object")
      val validationResult = useCase.validate()

      Then("it should return empty list since it will pass the validate")
      validationResult.size shouldBe 0
    }

    Scenario("There are multiple validations, and one of the validation failed") {
      Given("A UseCase object with validation rules")
      val useCase = new DoNothingUseCase {
        val value: Int = 100

        def largeThenTen(n: Int): Option[ErrorDescription] = if (n > 10) None else Some(IsMalformed)
        def largeThen1000(n: Int): Option[ErrorDescription] = if (n > 1000) None else Some(AccessDenied)

        override def validations = createValidator(fieldName = "value", value, largeThenTen, largeThen1000)
      }

      When("when call validate() for the usecase object")
      val validationResult = useCase.validate()

      Then("it should return correct validation error")
      validationResult should not be empty
      validationResult should contain theSameElementsInOrderAs List(FailedValidation("value", AccessDenied))
    }

    Scenario("There are multiple validations, and all of the validation failed") {
      Given("A UseCase object with validation rules")
      val useCase = new DoNothingUseCase {
        val value: Int = 0

        def largeThenTen(n: Int): Option[ErrorDescription] = if (n > 10) None else Some(IsMalformed)
        def largeThen1000(n: Int): Option[ErrorDescription] = if (n > 1000) None else Some(AccessDenied)

        override def validations = createValidator(fieldName = "value", value, largeThenTen, largeThen1000)
      }

      When("when call validate() for the usecase object")
      val validationResult = useCase.validate()

      Then("it should return correct validation error")
      validationResult should not be empty
      validationResult should contain theSameElementsInOrderAs List(
        FailedValidation("value", IsMalformed),
        FailedValidation("value", AccessDenied)
      )
    }

    Scenario("Group validations by field, and all of the validation pass") {
      Given("A UseCase object with validation rules")
      val useCase = new DoNothingUseCase {
        val value1: Int = 10000
        val value2: Int = 20000

        def largeThen10(n: Int): Option[ErrorDescription] = if (n > 10) None else Some(IsMalformed)
        def largeThen1000(n: Int): Option[ErrorDescription] = if (n > 1000) None else Some(AccessDenied)

        override def validations = groupByField(
          createValidator(fieldName = "value1", value1, largeThen10, largeThen1000),
          createValidator(fieldName = "value2", value2, largeThen10, largeThen1000)
        )
      }

      When("when call validate() for the usecase object")
      val validationResult = useCase.validate()

      Then("it should return correct validation error")
      validationResult shouldBe empty
    }

    Scenario("Group validations by field, and some of the validation failed") {
      Given("A UseCase object with validation rules")
      val useCase = new DoNothingUseCase {
        val value1: Int = 0
        val value2: Int = 100 

        def largeThen10(n: Int): Option[ErrorDescription] = if (n > 10) None else Some(IsMalformed)
        def largeThen1000(n: Int): Option[ErrorDescription] = if (n > 1000) None else Some(AccessDenied)

        override def validations = groupByField(
          createValidator(fieldName = "value1", value1, largeThen10, largeThen1000),
          createValidator(fieldName = "value2", value2, largeThen10, largeThen1000)
        )
      }


      When("when call validate() for the usecase object")
      val validationResult = useCase.validate()

      Then("it should return correct validation error")
      validationResult should not be empty
      validationResult.size shouldBe 3
      validationResult should contain theSameElementsInOrderAs List(
        FailedValidation("value1", IsMalformed),
        FailedValidation("value1", AccessDenied),
        FailedValidation("value2", AccessDenied)
      )
      
    }

  }
}
