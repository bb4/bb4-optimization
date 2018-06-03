// Copyright by Barry G. Becker, 2013 - 2018. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.parameter.improvement

import com.barrybecker4.optimization.optimizee.Optimizee
import com.barrybecker4.optimization.parameter.ParameterArray

import scala.collection.mutable


object DiscreteImprovementFinder {
  /** don't try more than this many times to find improvement on any iteration */
  private val MAX_TRIES = 1000
}

/**
  * Represents a 1 dimensional, variable length, array of unique integer parameters.
  * The order of the integers does not matter.
  * @author Barry Becker
  */
class DiscreteImprovementFinder(var params: ParameterArray) {

  /** Try to find a parameterArray that is better than what we have now by evaluating using the optimizee passed in.
    * Try swapping parameters randomly until we find an improvement (if we can).
    * @param optimizee something that can evaluate parameterArrays.
    * @param initialJumpSize  how far to move in the direction of improvement
    * @param cache  set of parameters that have already been tested. This is important for cases where the
    *               parameters are discrete and not continuous.
    * @return the improvement which contains the improved parameter array and possibly a revised jumpSize.
    */
  def findIncrementalImprovement(optimizee: Optimizee, initialJumpSize: Double, cache: mutable.Set[ParameterArray]): Improvement = {
    var numTries = 0
    var fitnessDelta = .0
    var jumpSize = initialJumpSize * 0.98
    var improvement = Improvement(params, 0, jumpSize)
    do {
      val nbr = params.getRandomNeighbor(jumpSize)
      fitnessDelta = 0
      if (!cache.contains(nbr)) {
        cache += nbr
        if (optimizee.evaluateByComparison)
          fitnessDelta = optimizee.compareFitness(nbr, params)
        else {
          val fitness = optimizee.evaluateFitness(nbr)
          fitnessDelta = params.getFitness - fitness
          nbr.setFitness(fitness)
        }
        if (fitnessDelta > 0) improvement = Improvement(nbr, fitnessDelta, jumpSize)
      }
      numTries += 1
      jumpSize *= 1.001
    } while (fitnessDelta <= 0 && numTries < DiscreteImprovementFinder.MAX_TRIES)

    println("incremental improvement = " + improvement.improvement + " numTries=" + numTries + " jumpSize=" + jumpSize
      + "\n num nodes in improvedParams=" + improvement.parameters.size
      + " fit=" + improvement.parameters.getFitness)
    improvement
  }
}