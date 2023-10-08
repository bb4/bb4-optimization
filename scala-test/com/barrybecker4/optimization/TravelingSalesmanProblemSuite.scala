package com.barrybecker4.optimization

import com.barrybecker4.optimization.optimizee.OptimizerTestSuite
import com.barrybecker4.optimization.optimizee.optimizees.problems.{TSP_US_CAPITALS, TravelingSalesmanProblem, TravelingSalesmanVariation}
import com.barrybecker4.optimization.parameter.VariableLengthIntSet
import com.barrybecker4.optimization.strategy.{BRUTE_FORCE, OptimizationStrategyType}

/**
  * @author Barry Becker
  */
class TravelingSalesmanProblemSuite extends OptimizerTestSuite {

  test("BruteForce") {
    doTest(BRUTE_FORCE)
  }

  override protected def doTest(optimizationType: OptimizationStrategyType[VariableLengthIntSet]): Unit = {
    for (variation <- TravelingSalesmanVariation.VALUES) {
      val problem = new TravelingSalesmanProblem(variation)
      if (!(optimizationType == BRUTE_FORCE && variation === TSP_US_CAPITALS))
        verifyProblem(problem, variation, optimizationType)
    }
  }
}
