package com.barrybecker4.optimization

import com.barrybecker4.optimization.optimizee.OptimizerTestSuite
import com.barrybecker4.optimization.optimizee.optimizees.problems.{TravelingSalesmanProblem, TravelingSalesmanVariation}
import com.barrybecker4.optimization.strategy.{BRUTE_FORCE, OptimizationStrategyType}

/**
  * @author Barry Becker
  */
class TravelingSalesmanProblemSuite extends OptimizerTestSuite {
  test("BruteForce") {
    doTest(BRUTE_FORCE)
  }

  override protected def doTest(optimizationType: OptimizationStrategyType): Unit = {
    for (variation <- TravelingSalesmanVariation.VALUES) {
      val problem = new TravelingSalesmanProblem(variation)
      verifyProblem(problem, variation, optimizationType)
    }
  }
}
