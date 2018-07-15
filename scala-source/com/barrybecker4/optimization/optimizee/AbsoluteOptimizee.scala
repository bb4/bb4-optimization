// Copyright by Barry G. Becker, 2000-2018. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.optimizee

import com.barrybecker4.optimization.parameter.ParameterArray


/**
  * Concrete adapter class for optimizee that does not evaluate by comparison.
  * @author Barry Becker
  */
abstract class AbsoluteOptimizee extends Optimizee {
  override def evaluateByComparison = false

  override def compareFitness(params1: ParameterArray, params2: ParameterArray): Double =
    evaluateFitness(params1) - evaluateFitness(params2)

  /** Optional. Override this only if you know that there is some optimal fitness that you need to reach.
    * @return optimal fitness value. Terminate search when reached.
    */
  override def getOptimalFitness = 0
}