package moe.brianhsu.maidroidtask.usecase

import java.util.UUID

object Validations {
  type ValidationRules = () => Option[FailedValidation]
  type Validator[T] = T => Option[ErrorDescription]

  case class ValidationErrors(failedValidations: List[FailedValidation]) extends Exception("Validate failed.")
  case class FailedValidation(fieldName: String, description: ErrorDescription)

  trait ErrorDescription
  case object IsMalformed extends ErrorDescription
  case object AccessDenied extends ErrorDescription
  case object Duplicated extends ErrorDescription
  case object Required extends ErrorDescription
  case object NotFound extends ErrorDescription
  case object HasChildren extends ErrorDescription
  case object AlreadyTrashed extends ErrorDescription
  case object NotTrashed extends ErrorDescription
  case object ParentIsTrashed extends ErrorDescription
  case object DependencyLoop extends ErrorDescription
  
  object BreakingChain {
    trait Operation
    case object MarkAsDone extends Operation
    case object MarkAsUndone extends Operation
  }
  case class BreakingChain(operation: BreakingChain.Operation, blocking: List[UUID], blockedBy: List[UUID]) extends ErrorDescription

}

