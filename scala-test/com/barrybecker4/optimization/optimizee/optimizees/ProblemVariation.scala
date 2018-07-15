// Copyright by Barry G. Becker, 2013-2014. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.optimizee.optimizees

import com.barrybecker4.optimization.parameter.{ParameterArray, ParameterArrayWithFitness}
import com.barrybecker4.optimization.strategy.OptimizationStrategyType


/**
  * A variation on an OptimizeeProblem
  * @author Barry Becker
  */
trait ProblemVariation {

  /** @return An optimal ordering of the cities to visit such that cost is minimized.  */
  def getExactSolution: ParameterArrayWithFitness

  /** @param opt the strategy type to get the expected error tolerance for.
    * @return the error tolerance percent for a specific optimization strategy
    */
  def getErrorTolerancePercent(opt: OptimizationStrategyType): Double

  def getName: String = getClass.getSimpleName
}
