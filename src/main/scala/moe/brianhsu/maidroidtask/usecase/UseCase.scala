package moe.brianhsu.maidroidtask.usecase

import Validations._
import moe.brianhsu.maidroidtask.entity._
import moe.brianhsu.maidroidtask.gateway.generator.DynamicDataGenerator
import moe.brianhsu.maidroidtask.gateway.repo.{ProjectRepo, TagRepo, TaskRepo, UserRepo}
import moe.brianhsu.maidroidtask.usecase.types.ResultHolder

trait UseCaseRuntime {
  val generator: DynamicDataGenerator
  val taskRepo: TaskRepo
  val tagRepo: TagRepo
  val projectRepo: ProjectRepo
  implicit val executor: UseCaseExecutor
}

trait UseCase[T] {

  def doAction(): T
  def validations: List[ValidationRules]
  def groupedJournal: GroupedJournal

  def execute()(implicit executor: UseCaseExecutor): ResultHolder[T] = executor.runUseCase(this)

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

