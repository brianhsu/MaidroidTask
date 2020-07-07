package moe.brianhsu.maidroidtask.usecase

import Validations._

import scala.util.Try
import moe.brianhsu.maidroidtask.entity._
import moe.brianhsu.maidroidtask.usecase.types.ResultHolder

package object types {
  type ResultHolder[T] = Try[UseCaseExecutorResult[T]]
}
case class UseCaseExecutorResult[T](result: T, journals: GroupedJournal)

class UseCaseExecutor {

  def runUseCase[T](useCase: UseCase[T]): ResultHolder[T] = {
    Try {
      useCase.validate() match {
        case Nil =>
          val result = useCase.doAction()
          appendJournals(useCase.groupedJournal.changes)
          UseCaseExecutorResult(result, useCase.groupedJournal)
        case failedValidations =>
          throw ValidationErrors(failedValidations)
      }
    }
  }

  def appendJournals(journals: List[Change]) = {}
}

