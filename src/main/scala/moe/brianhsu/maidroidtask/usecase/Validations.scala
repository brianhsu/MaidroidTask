package moe.brianhsu.maidroidtask.usecase

object Validations {
  type ValidationRules = () => Option[ValidationError]
  type Validator[T] = T => Option[ErrorDescription]

  case class ValidationError(fieldName: String, description: ErrorDescription)

  trait ErrorDescription
  case object IsMalformed extends ErrorDescription
  case object AccessDenied extends ErrorDescription
}

