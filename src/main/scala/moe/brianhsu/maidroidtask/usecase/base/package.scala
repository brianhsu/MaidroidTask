package moe.brianhsu.maidroidtask.usecase.base

import scala.util.Try

package object types {
  type ResultHolder[T] = Try[UseCaseExecutorResult[T]]
}

