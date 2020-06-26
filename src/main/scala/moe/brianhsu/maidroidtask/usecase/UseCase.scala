package moe.brianhsu.maidroidtask.usecase

import Validations._

trait UseCase {

  def validations: List[ValidationRules]

  protected def groupByField(validationsForFields: List[ValidationRules]*) = validationsForFields.toList.flatten
  protected def createValidation[T](fieldName: String, value: T, validators: Validator[T]*): List[ValidationRules] = { 
    validators.toList.map { validator => () => {
        validator(value).map { errorDescription => ValidationError(fieldName, errorDescription) }
      }
    }
  }

  def validate(): List[ValidationError] = {
    validations.flatMap { validation => 
      validation() 
    }
  }
}

