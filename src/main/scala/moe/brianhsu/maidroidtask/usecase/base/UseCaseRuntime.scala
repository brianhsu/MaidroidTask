package moe.brianhsu.maidroidtask.usecase.base

import moe.brianhsu.maidroidtask.gateway.generator.DynamicDataGenerator
import moe.brianhsu.maidroidtask.gateway.repo.{ProjectRepo, TagRepo, TaskRepo}

trait UseCaseRuntime {
  val generator: DynamicDataGenerator
  val taskRepo: TaskRepo
  val tagRepo: TagRepo
  val projectRepo: ProjectRepo
  implicit val executor: UseCaseExecutor
}
