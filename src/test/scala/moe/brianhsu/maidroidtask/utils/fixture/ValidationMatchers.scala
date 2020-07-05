package moe.brianhsu.maidroidtask.utils.fixture

import moe.brianhsu.maidroidtask.entity.Entity
import moe.brianhsu.maidroidtask.usecase.Validations.{ErrorDescription, FailedValidation, ValidationErrors}
import org.scalatest.TryValues
import org.scalatest.matchers.{BeMatcher, MatchResult}

import scala.util.Try

trait ValidationMatchers {

  this: TryValues =>

  class ValidationMatcher(field: String, errorDescription: ErrorDescription) extends BeMatcher[Try[Entity]] {
    override def apply(left: Try[Entity]): MatchResult = {

      val exception = left.failure.exception.asInstanceOf[ValidationErrors]

      MatchResult(
        exception.failedValidations.contains(FailedValidation(field, errorDescription)),
        left.toString + s" does not contains FailedValidation($field, $errorDescription)",
        left.toString + s" contains FailedValidation($field, $errorDescription)"
      )

    }
  }

  val failedValidation: (String, ErrorDescription) => ValidationMatcher = {
    (field: String, errorDescription: ErrorDescription) => new ValidationMatcher(field, errorDescription)
  }
}
