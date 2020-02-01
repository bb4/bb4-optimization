package com.barrybecker4.optimization

import com.barrybecker4.math.MathUtil
import com.barrybecker4.optimization.optimizee.OptimizerTestSuite
import com.barrybecker4.optimization.optimizee.optimizees.problems.{ParabolaMinFunctionProblem, ParabolaMinVariation}
import com.barrybecker4.optimization.strategy.OptimizationStrategyType


class ParabolaMinFunctionProblemSuite extends OptimizerTestSuite {
  override protected def doTest(optimizationType: OptimizationStrategyType): Unit = {
    for (variation <- ParabolaMinVariation.VALUES) {
      MathUtil.RANDOM.setSeed(0)
      val problem = new ParabolaMinFunctionProblem(variation)
      verifyProblem(problem, variation, optimizationType)
    }
  }
}
