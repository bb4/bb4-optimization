// Copyright by Barry G. Becker, 2000-2018. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.optimizee

import com.barrybecker4.optimization.parameter.ParameterArray


/**
  * Concrete adapter class for optimizee that does not evaluate by comparison.
  * @author Barry Becker
  */
abstract class AbsoluteOptimizee extends Optimizee {
  override def evaluateByComparison = false

  /** Since lower fitness numbers are better, we subtract the fitness for params2 from params1
    * @return the amount that params1 are better than params2. May be positive if params2 are better than params1.
    */
  override def compareFitness(params1: ParameterArray, params2: ParameterArray): Double =
     evaluateFitness(params2) - evaluateFitness(params1)

  /** Optional. Override this only if you know that there is some optimal fitness that you need to reach.
    * @return optimal fitness value. Terminate search when reached.
    */
  override def getOptimalFitness = 0
}