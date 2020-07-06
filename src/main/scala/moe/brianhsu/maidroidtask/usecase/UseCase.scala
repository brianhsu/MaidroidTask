package moe.brianhsu.maidroidtask.usecase

import Validations._
import moe.brianhsu.maidroidtask.entity._

trait UseCase[T] {

  def doAction(): T
  def journals: List[Journal]
  def validations: List[ValidationRules]

  def execute()(implicit executor: UseCaseExecutor): UseCaseExecutorResult[T] = executor.runUseCase(this)

  def validate(): List[FailedValidation] = {
    validations.flatMap { validation => 
      validation() 
    }
  }

  protected def groupByField(validationsForFields: List[ValidationRules]*) = validationsForFields.toList.flatten

  protected def createValidator[T](fieldName: String, value: T, firstValidation: Validator[T], validators: Validator[T]*): List[ValidationRules] = {
    (firstValidation :: validators.toList).map { validator => () => {
        validator(value).map { errorDescription => FailedValidation(fieldName, errorDescription) }
      }
    }
  }

}

