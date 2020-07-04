package moe.brianhsu.maidroidtask.usecase.fixture

import org.scalatest.{GivenWhenThen, OptionValues, Outcome, TryValues}
import org.scalatest.featurespec.FixtureAnyFeatureSpec
import org.scalatest.matchers.should.Matchers

trait BaseFixtureFeature[T] extends FixtureAnyFeatureSpec with GivenWhenThen with TryValues with Matchers with OptionValues {
  protected def createFixture: T
  override protected def withFixture(test: OneArgTest): Outcome = test(createFixture)
  override type FixtureParam = T
}
