package com.barrybecker4.optimization.strategy.gradient

import com.barrybecker4.common.math.Vector
import com.barrybecker4.optimization.optimizee.Optimizee
import com.barrybecker4.optimization.parameter.{ParameterArray, ParameterArrayWithFitness}

import scala.collection.mutable
import NumericImprovementFinder._
import ImprovementFinder.INITIAL_JUMP_SIZE

object NumericImprovementFinder {

  /** If the dot product of the new gradient with the old is less than this, then decrease the jump size. */
  val MIN_DOT_PRODUCT = 0.3

  /** If the dot product of the new gradient with the old is greater than this, then increase the jump size. */
  val MAX_DOT_PRODUCT = 0.98
}

/**
  * @param startingParams parameters to improve
  * @author Barry Becker
  */
class NumericImprovementFinder(val startingParams: ParameterArrayWithFitness) extends ImprovementFinder {


  /** Try to find a parameterArray that is better than what we have now by evaluating using the optimizee passed in.
    * Try swapping parameters randomly until we find an improvement (if we can).
    * @param optimizee something that can evaluate parameterArrays.
    * @param cache  set of parameters that have already been tested. This is important for cases where the
    *               parameters are discrete and not continuous.
    * @return the improvement which contains the improved parameter array and possibly a revised jumpSize.
    */
  def findIncrementalImprovement(optimizee: Optimizee, lastImprovement: Improvement,
                                 cache: mutable.Set[ParameterArray]): Improvement = {
    var currentParams: ParameterArrayWithFitness = null
    var oldGradient: Vector = null
    var jumpSize: Double = 0
    if (lastImprovement == null) {
      currentParams = startingParams
      jumpSize = INITIAL_JUMP_SIZE
    }
    if (lastImprovement != null) {
      currentParams = lastImprovement.parameters
      oldGradient = lastImprovement.gradient.get
      jumpSize = lastImprovement.jumpSize
    }
    var oldFitness: Double = currentParams.fitness
    val iter = new ImprovementIteration(currentParams, oldGradient)
    var sumOfSqs: Double = 0
    for (i <- 0 until currentParams.pa.size) {
      sumOfSqs += iter.incSumOfSqs(i, optimizee)
    }
    val gradLength = Math.sqrt(sumOfSqs)
    val step = new ImprovementStep(optimizee, iter, gradLength, cache, jumpSize, oldFitness)
    currentParams = step.findNextParams(currentParams)
    var newJumpSize = step.getJumpSize
    // the improvement may be zero or negative, meaning it did not improve.
    val improvement = step.getImprovement
    val dotProduct = iter.gradient.normalizedDot(iter.oldGradient)
    println("dot between " + iter.gradient + " and " + iter.oldGradient+ " is " + dotProduct)
    newJumpSize = findNewJumpSize(newJumpSize, dotProduct)
    iter.gradient.copyFrom(iter.oldGradient)
    Improvement(currentParams, improvement, newJumpSize, Some(iter.gradient))
  }

  /** If we are headed in pretty much the same direction as last time, then we increase the jumpSize.
    * If we are headed off in a completely new direction, reduce the jumpSize until we start to stabilize.
    * @param jumpSize   the current amount that is stepped in the assumed solution direction.
    * @param dotProduct determines the angle between the new gradient and the old.
    * @return the new jump size - which is usually the same as the old one.
    */
  private def findNewJumpSize(jumpSize: Double, dotProduct: Double) = {
    var newJumpSize = jumpSize
    if (dotProduct > MAX_DOT_PRODUCT)
      newJumpSize *= ImprovementStep.JUMP_SIZE_INC_FACTOR
    else if (dotProduct < MIN_DOT_PRODUCT)
      newJumpSize *= ImprovementStep.JUMP_SIZE_DEC_FACTOR
    //println( "dotProduct = " + dotProduct + " new jumpsize = " + jumpSize );
    newJumpSize
  }
}