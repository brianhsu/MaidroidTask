package moe.brianhsu.maidroidtask.usecase.base

import moe.brianhsu.maidroidtask.entity._
import moe.brianhsu.maidroidtask.usecase.Validations.{FailedValidation, ValidationRules, Validator}
import moe.brianhsu.maidroidtask.usecase.base.types.ResultHolder

trait UseCase[T] {

  def doAction(): T
  def validations: List[ValidationRules]
  def journal: Journal

  def execute()(implicit executor: UseCaseExecutor): ResultHolder[T] = executor.runUseCase(this)

  def validate(): List[FailedValidation] = {
    validations.flatMap { validation => 
      validation() 
    }
  }

  protected def groupByField(validationsForFields: List[ValidationRules]*): List[ValidationRules] = validationsForFields.toList.flatten

  protected def createValidator[S](fieldName: String,
                                   value: S,
                                   firstValidation: Validator[S],
                                   validators: Validator[S]*): List[ValidationRules] = {
    (firstValidation :: validators.toList).map { validator => () => {
        validator(value).map { errorDescription => FailedValidation(fieldName, errorDescription) }
      }
    }
  }

}

