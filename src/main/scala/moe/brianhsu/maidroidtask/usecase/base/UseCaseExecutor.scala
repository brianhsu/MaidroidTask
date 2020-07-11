package moe.brianhsu.maidroidtask.usecase.base

import moe.brianhsu.maidroidtask.entity._
import moe.brianhsu.maidroidtask.usecase.Validations.ValidationErrors
import moe.brianhsu.maidroidtask.usecase.base.types.ResultHolder

import scala.util.Try

case class UseCaseExecutorResult[T](result: T, journals: Journal)

class UseCaseExecutor {

  def runUseCase[T](useCase: UseCase[T]): ResultHolder[T] = {
    Try {
      useCase.validate() match {
        case Nil =>
          val result = useCase.doAction()
          appendJournals(useCase.journal.changes)
          UseCaseExecutorResult(result, useCase.journal)
        case failedValidations =>
          throw ValidationErrors(failedValidations)
      }
    }
  }

  def appendJournals(journals: List[Change]) = {}
}

