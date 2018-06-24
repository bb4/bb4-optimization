package com.barrybecker4.optimization

import com.barrybecker4.optimization.optimizee.OptimizerTestSuite
import com.barrybecker4.optimization.optimizee.optimizees.problems.{TYPICAL_DS, DominatingSetProblem, DominatingSetVariation}
import com.barrybecker4.optimization.strategy.{BRUTE_FORCE, OptimizationStrategyType}


class DominatingSetProblemSuite extends OptimizerTestSuite {

  test("BruteForce") {doTest(BRUTE_FORCE)}

  override protected def doTest(optimizationType: OptimizationStrategyType): Unit = {
    for (variation <- DominatingSetVariation.VALUES) {
      val problem = new DominatingSetProblem(variation)
      if (!(optimizationType == BRUTE_FORCE && variation === TYPICAL_DS))
        verifyProblem(problem, variation, optimizationType)
    }
  }
}
