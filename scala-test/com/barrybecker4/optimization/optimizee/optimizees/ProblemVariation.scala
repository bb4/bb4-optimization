package com.barrybecker4.optimization.optimizee.optimizees

import com.barrybecker4.optimization.parameter1.ParameterArray
import com.barrybecker4.optimization.strategy.OptimizationStrategyType


/**
  * A variation on an OptimizeeProblem
  * @author Barry Becker
  */
trait ProblemVariation {

  /** @return An optimal ordering of the cities to visit such that cost is minimized.  */
  def getExactSolution: ParameterArray

  /** @param opt the strategy type to get the expected error tolerance for.
    * @return the error tolerance percent for a specific optimization strategy
    */
  def getErrorTolerancePercent(opt: OptimizationStrategyType): Double
}
