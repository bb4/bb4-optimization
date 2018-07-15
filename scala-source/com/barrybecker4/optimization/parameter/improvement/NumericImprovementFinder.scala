package com.barrybecker4.optimization.parameter.improvement

import com.barrybecker4.common.math.Vector
import com.barrybecker4.optimization.optimizee.Optimizee
import com.barrybecker4.optimization.parameter.{NumericParameterArray, ParameterArray, ParameterArrayWithFitness}

import scala.collection.mutable

class NumericImprovementFinder(var params: ParameterArrayWithFitness) {

  /** Try to find a parameterArray that is better than what we have now by evaluating using the optimizee passed in.
    * Try swapping parameters randomly until we find an improvement (if we can).
    * @param optimizee something that can evaluate parameterArrays.
    * @param jumpSize how far to move in the direction of improvement
    * @param cache  set of parameters that have already been tested. This is important for cases where the
    *               parameters are discrete and not continuous.
    * @return the improvement which contains the improved parameter array and possibly a revised jumpSize.
    */
  //def findIncrementalImprovement(optimizee: Optimizee, initialJumpSize: Double,
  //                               cache: mutable.Set[ParameterArray]): Improvement = {
  def findIncrementalImprovement(optimizee: Optimizee, jumpSize: Double,
                                 lastImprovement: Improvement,
                                 cache: mutable.Set[ParameterArray]): Improvement = {
    var currentParams = params
    var oldFitness = currentParams.fitness
    var oldGradient: Vector = null
    if (lastImprovement != null) {
      oldFitness = lastImprovement.parameters.fitness
      oldGradient = lastImprovement.gradient
    }
    val iter = new ImprovementIteration(params, oldGradient)
    var sumOfSqs: Double = 0
    for (i <- 0 until params.pa.size) {
      val testParams = params.pa.copy
      sumOfSqs = iter.incSumOfSqs(i, sumOfSqs, optimizee, currentParams, testParams)
    }
    val gradLength = Math.sqrt(sumOfSqs)
    val step = new ImprovementStep(optimizee, iter, gradLength, cache, jumpSize, oldFitness)
    currentParams = step.findNextParams(currentParams)
    var newJumpSize = step.getJumpSize
    // the improvement may be zero or negative, meaning it did not improve.
    val improvement = step.getImprovement
    val dotProduct = iter.gradient.normalizedDot(iter.oldGradient)
    //println("dot between " + iter.getGradient() + " and " + iter.getOldGradient()+ " is "+ dotProduct);
    newJumpSize = findNewJumpSize(newJumpSize, dotProduct)
    iter.gradient.copyFrom(iter.oldGradient)
    Improvement(currentParams, improvement, newJumpSize, iter.gradient)
  }

  /** If we are headed in pretty much the same direction as last time, then we increase the jumpSize.
    * If we are headed off in a completely new direction, reduce the jumpSize until we start to stabilize.
    * @param jumpSize   the current amount that is stepped in the assumed solution direction.
    * @param dotProduct determines the angle between the new gradient and the old.
    * @return the new jump size - which is usually the same as the old one.
    */
  private def findNewJumpSize(jumpSize: Double, dotProduct: Double) = {
    var newJumpSize = jumpSize
    if (dotProduct > NumericParameterArray.MAX_DOT_PRODUCT)
      newJumpSize *= ImprovementStep.JUMP_SIZE_INC_FACTOR
    else if (dotProduct < NumericParameterArray.MIN_DOT_PRODUCT)
      newJumpSize *= ImprovementStep.JUMP_SIZE_DEC_FACTOR
    //println( "dotProduct = " + dotProduct + " new jumpsize = " + jumpSize );
    newJumpSize
  }


}
