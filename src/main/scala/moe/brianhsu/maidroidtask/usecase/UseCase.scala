package moe.brianhsu.maidroidtask.usecase

import Validations._
import scala.util.Try

trait UseCaseExecutor {
  def runUseCase[T](useCase: UseCase[T]): Try[T]
}

class BaseUseCaseExecutor extends UseCaseExecutor {
  override def runUseCase[T](useCase: UseCase[T]): Try[T] = Try {
    useCase.validate() match {
      case Nil => useCase.doAction()
      case failedValidations => throw new ValidationErrors(failedValidations)
    }
  }
}

trait UseCase[T] {

  def doAction(): T
  protected def validations: List[ValidationRules]

  protected def groupByField(validationsForFields: List[ValidationRules]*) = validationsForFields.toList.flatten
  protected def createValidator[T](fieldName: String, value: T, validators: Validator[T]*): List[ValidationRules] = { 
    validators.toList.map { validator => () => {
        validator(value).map { errorDescription => FailedValidation(fieldName, errorDescription) }
      }
    }
  }

  def execute()(implicit executor: UseCaseExecutor): Try[T] = executor.runUseCase(this)
  def validate(): List[FailedValidation] = {
    validations.flatMap { validation => 
      validation() 
    }
  }
}

