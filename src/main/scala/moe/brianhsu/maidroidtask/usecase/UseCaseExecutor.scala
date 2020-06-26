package moe.brianhsu.maidroidtask.usecase

import Validations._
import scala.util.Try
import moe.brianhsu.maidroidtask.entity._

class UseCaseExecutor {
  def runUseCase[T](useCase: UseCase[T]): Try[T] = Try {
    useCase.validate() match {
      case Nil => 
        val result = useCase.doAction()
        appendJournals(useCase.journals)
        result
      case failedValidations => 
        throw new ValidationErrors(failedValidations)
    }
  }

  def appendJournals(journals: List[Journal]) = {}
}

