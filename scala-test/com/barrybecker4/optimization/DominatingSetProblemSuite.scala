package com.barrybecker4.optimization

import com.barrybecker4.optimization.optimizee.OptimizerTestSuite
import com.barrybecker4.optimization.optimizee.optimizees.problems.{DominatingSetProblem, DominatingSetVariation, TYPICAL_DS}
import com.barrybecker4.optimization.strategy.{BRUTE_FORCE, OptimizationStrategyType}
import com.barrybecker4.optimization.parameter.{NumericParameterArray, ParameterArray}


class DominatingSetProblemSuite extends OptimizerTestSuite {

  test("BruteForce") {doTest(BRUTE_FORCE)}

  override protected def doTest(optimizationType: OptimizationStrategyType[ParameterArray]): Unit = {
    for (variation <- DominatingSetVariation.VALUES) {
      val problem = new DominatingSetProblem(variation)
      if (!(optimizationType == BRUTE_FORCE && variation === TYPICAL_DS))
        verifyProblem(problem, variation, optimizationType)
    }
  }
}
