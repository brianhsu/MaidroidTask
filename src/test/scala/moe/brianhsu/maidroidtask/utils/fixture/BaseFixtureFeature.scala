package moe.brianhsu.maidroidtask.utils.fixture

import moe.brianhsu.maidroidtask.entity.Task
import org.scalatest.{Assertion, GivenWhenThen, Inside, Inspectors, OptionValues, Outcome, TryValues}
import org.scalatest.featurespec.FixtureAnyFeatureSpec
import org.scalatest.matchers.should.Matchers

trait BaseFixtureFeature[T] extends FixtureAnyFeatureSpec with GivenWhenThen
                            with TryValues with Matchers
                            with OptionValues with Inside
                            with ValidationMatchers with Inspectors {
  protected def createFixture: T
  override protected def withFixture(test: OneArgTest): Outcome = test(createFixture)
  override type FixtureParam = T

  def isSameTask(task: Task, expectedTask: Task): Assertion = {
    inside(task) { case Task(uuid, userUUID, description, note, project, tags,
    dependsOn, waitUntil, due, scheduledAt,
    isDone, isTrashed, createTime, updateTime) =>

      uuid shouldBe expectedTask.uuid
      userUUID shouldBe expectedTask.userUUID
      description shouldBe expectedTask.description
      note shouldBe expectedTask.note
      project shouldBe expectedTask.project
      tags shouldBe expectedTask.tags
      dependsOn shouldBe expectedTask.dependsOn
      waitUntil shouldBe expectedTask.waitUntil
      due shouldBe expectedTask.due
      scheduledAt shouldBe expectedTask.scheduledAt
      isDone shouldBe expectedTask.isDone
      isTrashed shouldBe expectedTask.isTrashed
      updateTime shouldBe expectedTask.updateTime
    }
  }

}
