package moe.brianhsu.maidroidtask.usecase

object Validations {
  type ValidationRules = () => Option[FailedValidation]
  type Validator[T] = T => Option[ErrorDescription]

  case class ValidationErrors(failedValidations: List[FailedValidation]) extends Exception("Validate failed.")
  case class FailedValidation(fieldName: String, description: ErrorDescription)

  trait ErrorDescription
  case object IsMalformed extends ErrorDescription
  case object AccessDenied extends ErrorDescription
}

