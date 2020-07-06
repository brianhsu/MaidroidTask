package moe.brianhsu.maidroidtask.usecase

import Validations._

import scala.util.Try
import moe.brianhsu.maidroidtask.entity._

case class UseCaseExecutorResult[T](result: Try[T], journals: List[Journal])

class UseCaseExecutor {

  def runUseCase[T](useCase: UseCase[T]): UseCaseExecutorResult[T] = {
    lazy val journals = useCase.journals
    val result = Try {
      useCase.validate() match {
        case Nil =>
          val result = useCase.doAction()
          appendJournals(journals)
          result
        case failedValidations =>
          throw ValidationErrors(failedValidations)
      }
    }
    UseCaseExecutorResult(result, if (result.isSuccess) journals else Nil)
  }

  def appendJournals(journals: List[Journal]) = {}
}

