package com.barrybecker4.optimization

import com.barrybecker4.optimization.optimizee.OptimizerTestSuite
import com.barrybecker4.optimization.optimizee.optimizees.problems.{SubsetSumProblem, SubsetSumVariation}
import com.barrybecker4.optimization.strategy.{BRUTE_FORCE, OptimizationStrategyType}

/**
  * The subset sum problem is NP-complete.
  *
  * @author Barry Becker
  */
class SubsetSumProblemSuite extends OptimizerTestSuite {
  test("BruteForce") {
    doTest(BRUTE_FORCE)
  }

  override protected def doTest(optimizationType: OptimizationStrategyType[NumericParameterType]): Unit = {
    for (variation <- SubsetSumVariation.VALUES) {
      val problem = new SubsetSumProblem(variation)
      verifyProblem(problem, variation, optimizationType)
    }
  }
}
