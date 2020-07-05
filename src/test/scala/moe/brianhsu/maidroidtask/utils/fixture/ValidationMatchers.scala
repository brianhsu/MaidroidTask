package moe.brianhsu.maidroidtask.utils.fixture

import moe.brianhsu.maidroidtask.entity.Entity
import moe.brianhsu.maidroidtask.usecase.Validations.{ErrorDescription, FailedValidation, ValidationErrors}
import org.scalatest.TryValues
import org.scalatest.matchers.{MatchResult, Matcher}

import scala.util.{Failure, Success, Try}

trait ValidationMatchers {

  this: TryValues =>

  class ValidationMatcher(field: String, errorDescription: ErrorDescription) extends Matcher[Try[Entity]] {
    override def apply(left: Try[Entity]): MatchResult = {
      left match {
        case Success(_) => MatchResult(matches = false, s"$left is not a Failure", s"$left is a Failure")
        case Failure(ValidationErrors(failedValidations)) =>
          MatchResult(
            failedValidations.contains(FailedValidation(field, errorDescription)),
            s"${failedValidations} does not contains FailedValidation($field, $errorDescription)",
            s"${failedValidations} contains FailedValidation($field, $errorDescription)"
          )
        case Failure(exception) =>
          MatchResult(
            matches = false,
            s"${exception} is not a ValidationErrors",
            s"${exception} is a ValidationErrors",
          )
      }
    }
  }

  val containsFailedValidation: (String, ErrorDescription) => ValidationMatcher = {
    (field: String, errorDescription: ErrorDescription) => new ValidationMatcher(field, errorDescription)
  }

}
