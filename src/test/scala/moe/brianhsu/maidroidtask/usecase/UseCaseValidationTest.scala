package moe.brianhsu.maidroidtask.usecase

import org.scalatest.featurespec.AnyFeatureSpec
import org.scalatest._
import org.scalatest.matchers.should.Matchers._


class UseCaseValidationSpec extends AnyFeatureSpec with GivenWhenThen {

  import Validations._

  Feature("Validate request") {
    info("As a developer,")
    info("I want to able to validate requests in UseCases before execute")

    Scenario("The validation rule is empty") {
      Given("A UseCase object without validation rule")
      val useCase = new UseCase {
        override def validations = Nil
      }

      When("call validate() for the usecase object")
      val validationResult = useCase.validate()

      Then("the validation result should be empty since it does not run validate")
      validationResult shouldBe empty
    }

    Scenario("There is one validation, and the validation passed") {
      Given("A UseCase object with validation rules")
      class ValidationExample extends UseCase {
        var isValidated = false
        val value: Int = 10

        def checkValueLargeThenZero(n: Int): Option[ErrorDescription] = {
          isValidated = true
          if (n > 0) None else Some(IsMalformed)
        }

        override def validations = createValidation(fieldName = "value", value, checkValueLargeThenZero)
      }

      val useCase = new ValidationExample

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
      class ValidationExample extends UseCase {
        var isValidated = false
        val value: Int = 5 

        def checkValueLargeThenTen(n: Int): Option[ErrorDescription] = {
          isValidated = true
          if (n > 10) None else Some(IsMalformed)
        }

        override def validations = createValidation(fieldName = "value", value, checkValueLargeThenTen)
      }

      val useCase = new ValidationExample

      Then("it should not run any valiadtion before validate()")
      useCase.isValidated shouldBe false

      And("when call validate() for the usecase object")
      val validationResult = useCase.validate()

      Then("it should run validation")
      useCase.isValidated shouldBe true

      And("it should return list contains ValidationError")
      validationResult should not be empty
      validationResult should contain theSameElementsInOrderAs List(ValidationError("value", IsMalformed))
    }

    Scenario("There are multiple validations, and the validation pass") {
      Given("A UseCase object with validation rules")
      class ValidationExample extends UseCase {
        val value: Int = 100

        def largeThenTen(n: Int): Option[ErrorDescription] = if (n > 10) None else Some(IsMalformed)
        def largeThenZero(n: Int): Option[ErrorDescription] = if (n > 0) None else Some(IsMalformed)

        override def validations = createValidation(fieldName = "value", value, largeThenTen, largeThenZero)
      }

      val useCase = new ValidationExample

      When("when call validate() for the usecase object")
      val validationResult = useCase.validate()

      Then("it should return empty list since it will pass the validate")
      validationResult.size shouldBe 0
    }

    Scenario("There are multiple validations, and one of the validation failed") {
      Given("A UseCase object with validation rules")
      class ValidationExample extends UseCase {
        val value: Int = 100

        def largeThenTen(n: Int): Option[ErrorDescription] = if (n > 10) None else Some(IsMalformed)
        def largeThen1000(n: Int): Option[ErrorDescription] = if (n > 1000) None else Some(AccessDenied)

        override def validations = createValidation(fieldName = "value", value, largeThenTen, largeThen1000)
      }

      val useCase = new ValidationExample

      When("when call validate() for the usecase object")
      val validationResult = useCase.validate()

      Then("it should return correct validation error")
      validationResult should not be empty
      validationResult should contain theSameElementsInOrderAs List(ValidationError("value", AccessDenied))
    }

    Scenario("There are multiple validations, and all of the validation failed") {
      Given("A UseCase object with validation rules")
      class ValidationExample extends UseCase {
        val value: Int = 0

        def largeThenTen(n: Int): Option[ErrorDescription] = if (n > 10) None else Some(IsMalformed)
        def largeThen1000(n: Int): Option[ErrorDescription] = if (n > 1000) None else Some(AccessDenied)

        override def validations = createValidation(fieldName = "value", value, largeThenTen, largeThen1000)
      }

      val useCase = new ValidationExample

      When("when call validate() for the usecase object")
      val validationResult = useCase.validate()

      Then("it should return correct validation error")
      validationResult should not be empty
      validationResult should contain theSameElementsInOrderAs List(
        ValidationError("value", IsMalformed),
        ValidationError("value", AccessDenied)
      )
    }

    Scenario("Group validations by field, and all of the validation pass") {
      Given("A UseCase object with validation rules")
      class ValidationExample extends UseCase {
        val value1: Int = 10000
        val value2: Int = 20000

        def largeThen10(n: Int): Option[ErrorDescription] = if (n > 10) None else Some(IsMalformed)
        def largeThen1000(n: Int): Option[ErrorDescription] = if (n > 1000) None else Some(AccessDenied)

        override def validations = groupByField(
          createValidation(fieldName = "value1", value1, largeThen10, largeThen1000),
          createValidation(fieldName = "value2", value2, largeThen10, largeThen1000)
        )
      }

      val useCase = new ValidationExample

      When("when call validate() for the usecase object")
      val validationResult = useCase.validate()

      Then("it should return correct validation error")
      validationResult shouldBe empty
    }

    Scenario("Group validations by field, and some of the validation failed") {
      Given("A UseCase object with validation rules")
      class ValidationExample extends UseCase {
        val value1: Int = 0
        val value2: Int = 100 

        def largeThen10(n: Int): Option[ErrorDescription] = if (n > 10) None else Some(IsMalformed)
        def largeThen1000(n: Int): Option[ErrorDescription] = if (n > 1000) None else Some(AccessDenied)

        override def validations = groupByField(
          createValidation(fieldName = "value1", value1, largeThen10, largeThen1000),
          createValidation(fieldName = "value2", value2, largeThen10, largeThen1000)
        )
      }

      val useCase = new ValidationExample

      When("when call validate() for the usecase object")
      val validationResult = useCase.validate()

      Then("it should return correct validation error")
      validationResult should not be empty
      validationResult.size shouldBe 3
      validationResult should contain theSameElementsInOrderAs List(
        ValidationError("value1", IsMalformed),
        ValidationError("value1", AccessDenied),
        ValidationError("value2", AccessDenied)
      )
      
    }

  }
}
