package com.barrybecker4.optimization

import com.barrybecker4.math.MathUtil
import com.barrybecker4.optimization.optimizee.OptimizerTestSuite
import com.barrybecker4.optimization.optimizee.optimizees.problems.{
  DominatingSetProblem,
  NO_SOLUTION,
  SIMPLE_SS,
  SubsetSumProblem,
  TravelingSalesmanProblem,
  TSP_STANDARD,
  TYPICAL_DS
}
import com.barrybecker4.optimization.strategy.{BRUTE_FORCE, OptimizationStrategyType}

/**
  * Heavier integration cases (extra TSP and subset-sum variants, and TYPICAL_DS brute force).
  *
  * Excluded from the default Gradle `test` task; run with `./gradlew slowTest`.
  *
  * ScalaTest also defines [[org.scalatest.tags.Slow]]: a tag you can attach as
  * `test("name", org.scalatest.tags.Slow) { ... }` in suites; the runner must include
  * `-n org.scalatest.tags.Slow` to run only tagged tests, or `-l` to exclude them.
  * This project uses a separate Gradle task instead so it works without custom ScalaTest args.
  */
class SlowIntegrationSuite extends OptimizerTestSuite {

  test("BruteForce") {
    doTest(BRUTE_FORCE)
  }

  override protected def doTest(optimizationType: OptimizationStrategyType): Unit = {
    for (variation <- Seq(TSP_STANDARD)) {
      MathUtil.RANDOM.setSeed(0)
      val problem = new TravelingSalesmanProblem(variation)
      verifyProblem(problem, variation, optimizationType)
    }
    for (variation <- Seq(SIMPLE_SS, NO_SOLUTION)) {
      MathUtil.RANDOM.setSeed(0)
      val problem = new SubsetSumProblem(variation)
      verifyProblem(problem, variation, optimizationType)
    }
    if (optimizationType == BRUTE_FORCE) {
      MathUtil.RANDOM.setSeed(0)
      val problem = new DominatingSetProblem(TYPICAL_DS)
      verifyProblem(problem, TYPICAL_DS, optimizationType)
    }
  }
}
