package com.barrybecker4.optimization.strategy.gradient

import com.barrybecker4.math.linear.Vector
import com.barrybecker4.optimization.optimizee.Optimizee
import com.barrybecker4.optimization.parameter.{ParameterArray, ParameterArrayWithFitness}

import scala.collection.mutable
import ImprovementFinder.INITIAL_JUMP_SIZE

/**
  * @param startingParams parameters to improve
  * @author Barry Becker
  */
class NumericImprovementFinder(val startingParams: ParameterArrayWithFitness,
                               baselineParams: ParameterArray,
                               useCache: Boolean = false,
                               trace: String => Unit = _ => ()) extends ImprovementFinder {


  /** Try to find a parameterArray that is better than what we have now by evaluating using the optimizee passed in.
    * Try swapping parameters randomly until we find an improvement (if we can).
    * @param optimizee something that can evaluate parameterArrays.
    * @param cache  set of parameters that have already been tested. This is important for cases where the
    *               parameters are discrete and not continuous.
    * @return the improvement which contains the improved parameter array and possibly a revised jumpSize.
    */
  def findIncrementalImprovement(optimizee: Optimizee, lastImprovement: Option[Improvement],
                                 cache: mutable.Set[ParameterArray]): Improvement = {
    val currentParams: ParameterArrayWithFitness = lastImprovement.map(_.parameters).getOrElse(startingParams)
    val priorGradient: Option[Vector] = lastImprovement.flatMap(_.gradient)
    val jumpSize: Double = lastImprovement.map(_.jumpSize).getOrElse(INITIAL_JUMP_SIZE)
    val oldFitness: Double = currentParams.fitness
    val numericPa = requireNumericParameterArray(currentParams.pa)
    val iter = new ImprovementIteration(currentParams, numericPa, priorGradient)
    var sumOfSqs: Double = 0
    for (i <- 0 until currentParams.pa.size) {
      sumOfSqs += iter.incSumOfSqs(i, optimizee)
    }
    val gradLength = Math.sqrt(sumOfSqs)
    val step = new ImprovementStep(optimizee, iter, gradLength, cache, jumpSize, oldFitness,
      baselineParams, useCache, trace)
    val newParams = step.findNextParams(currentParams)
    var newJumpSize = step.jumpSize
    // the improvement may be zero or negative, meaning it did not improve.
    val improvement = step.getImprovement
    val dotProduct = iter.gradient.normalizedDot(iter.previousGradientForDot)
    newJumpSize = findNewJumpSize(newJumpSize, dotProduct)
    Improvement(newParams, improvement, newJumpSize, Some(iter.gradient))
  }

  /** If we are headed in pretty much the same direction as last time, then we increase the jumpSize.
    * If we are headed off in a completely new direction, reduce the jumpSize until we start to stabilize.
    * @param jumpSize   the current amount that is stepped in the assumed solution direction.
    * @param dotProduct determines the angle between the new gradient and the old.
    * @return the new jump size - which is usually the same as the old one.
    */
  private def findNewJumpSize(jumpSize: Double, dotProduct: Double) = {
    var newJumpSize = jumpSize
    if (dotProduct > GRADIENT_MAX_DOT_PRODUCT)
      newJumpSize *= ImprovementStep.JUMP_SIZE_INC_FACTOR
    else if (dotProduct < GRADIENT_MIN_DOT_PRODUCT)
      newJumpSize *= ImprovementStep.JUMP_SIZE_DEC_FACTOR
    newJumpSize
  }
}
