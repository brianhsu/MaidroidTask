package moe.brianhsu.maidroidtask.usecase.validator

import moe.brianhsu.maidroidtask.usecase.Validations.{ErrorDescription, Required}

object GenericValidator {

  /**
   * This method will convert a validator that could be used to a Option[T] field.
   *
   * @param   validator   The validator that checks if value in Option is valid or not.
   * @tparam  T           The type of the value inside the Option
   * @return              Some[ErrorDescription] if value inside Option is not valid, None otherwise.
   */
  def option[T](validator: T => Option[ErrorDescription]): Option[T] => Option[ErrorDescription] = { optionValue: Option[T] =>
    optionValue.flatMap(validator)
  }

  /**
   * Validate if a string is empty or not.
   *
   * @param   string  The string to be checked.
   * @return          Some(Required) if string is empty, None otherwise.
   */
  def notEmpty(string: String): Option[ErrorDescription] = if (string.trim.isEmpty) Some(Required) else None

}
