package com.barrybecker4.optimization.parameter.improvement

import com.barrybecker4.optimization.optimizee.Optimizee
import com.barrybecker4.optimization.parameter.ParameterArray

import scala.collection.mutable

trait ImprovementFinder {

  /** Try to find a parameterArray that is better than what we have now by evaluating using the optimizee passed in.
    * Try swapping parameters randomly until we find an improvement (if we can).
    * @param optimizee something that can evaluate parameterArrays.
    * @param jumpSize how far to move in the direction of improvement
    * @param cache  set of parameters that have already been tested. This is important for cases where the
    *               parameters are discrete and not continuous.
    * @return the improvement which contains the improved parameter array and possibly a revised jumpSize.
    */
  def findIncrementalImprovement(optimizee: Optimizee, jumpSize: Double, improvement: Improvement,
                                 cache: mutable.Set[ParameterArray]): Improvement
}
