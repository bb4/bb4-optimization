package com.barrybecker4.optimization.strategy.gradient

import com.barrybecker4.optimization.optimizee.Optimizee
import com.barrybecker4.optimization.parameter.ParameterArray

import scala.collection.mutable

object ImprovementFinder {
  /** make steps of this size toward the local maxima, until we need something else. */
  val INITIAL_JUMP_SIZE = 0.9
}

/**
  * Find gradient in direction of maximal improvement and move incrementally toward the solution.
  * @author Barry Becker
  */
trait ImprovementFinder[P <: ParameterArray] {

  /** Try to find a parameterArray that is better than what we have now by evaluating using the optimizee passed in.
    * Try swapping parameters randomly until we find an improvement (if we can).
    * @param optimizee something that can evaluate parameterArrays.
    * @param cache  set of parameters that have already been tested. This is important for cases where the
    *               parameters are discrete and not continuous.
    * @return the improvement which contains the improved parameter array and possibly a revised jumpSize.
    */
  def findIncrementalImprovement(optimizee: Optimizee[P], improvement: Improvement[P],
                                 cache: mutable.Set[P]): Improvement[P]
}
