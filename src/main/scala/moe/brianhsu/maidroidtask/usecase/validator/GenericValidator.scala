package moe.brianhsu.maidroidtask.usecase.validator

import moe.brianhsu.maidroidtask.usecase.Validations.{ErrorDescription, Required}

object GenericValidator {

  def ifAssigned[T](validator: T => Option[ErrorDescription]): Option[T] => Option[ErrorDescription] = { optionValue: Option[T] =>
    optionValue.flatMap(validator)
  }

  def notEmpty(string: String): Option[ErrorDescription] = if (string.trim.isEmpty) Some(Required) else None

}
