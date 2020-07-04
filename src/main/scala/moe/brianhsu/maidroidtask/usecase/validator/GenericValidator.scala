package moe.brianhsu.maidroidtask.usecase.validator

import moe.brianhsu.maidroidtask.usecase.Validations.{ErrorDescription, Required}

object GenericValidator {
  def notEmpty(string: String): Option[ErrorDescription] = if (string.trim.isEmpty) Some(Required) else None

}
