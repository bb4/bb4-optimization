// Copyright by Barry G. Becker, 2013 - 2018. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.parameter.improvement

import com.barrybecker4.optimization.optimizee.Optimizee
import com.barrybecker4.optimization.parameter.{ParameterArray, ParameterArrayWithFitness}
import DiscreteImprovementFinder._

import scala.collection.mutable


object DiscreteImprovementFinder {
  /** don't try more than this many times to find improvement on any iteration */
  private val MAX_TRIES = 1000

  private val JUMP_SIZE_DECREASE = 0.98
  private val JUMP_SIZE_INCREASE = 1.01
}

/**
  * Finds incremental improvement in a discrete problem space.
  * @author Barry Becker
  */
class DiscreteImprovementFinder(var params: ParameterArrayWithFitness) {

  /** Try to find a parameterArray that is better than what we have now by evaluating using the optimizee passed in.
    * Try swapping parameters randomly until we find an improvement (if we can).
    * @param optimizee something that can evaluate parameterArrays.
    * @param initialJumpSize  how far to move in the direction of improvement
    * @param cache  set of parameters that have already been tested. This is important for cases where the
    *               parameters are discrete and not continuous.
    * @return the improvement which contains the improved parameter array and possibly a revised jumpSize.
    */
  def findIncrementalImprovement(optimizee: Optimizee, initialJumpSize: Double,
                                 cache: mutable.Set[ParameterArray]): Improvement = {
    var numTries = 0
    var fitnessDelta = .0
    var jumpSize = initialJumpSize * JUMP_SIZE_DECREASE
    var improvement = Improvement(params, 0, jumpSize)
    do {
      val nbrParam = params.pa.getRandomNeighbor(jumpSize)
      fitnessDelta = 0
      if (!cache.contains(nbrParam)) {
        cache += nbrParam
        var nbr: ParameterArrayWithFitness = null
        if (optimizee.evaluateByComparison) {
          fitnessDelta = optimizee.compareFitness(nbrParam, params.pa)
          nbr = ParameterArrayWithFitness(nbrParam, params.fitness + fitnessDelta)
        } else {
          val fitness = optimizee.evaluateFitness(nbrParam)
          fitnessDelta = params.fitness - fitness
          nbr = ParameterArrayWithFitness(nbrParam, fitness)
        }

        if (fitnessDelta > 0) improvement = Improvement(nbr, fitnessDelta, jumpSize)
      }
      numTries += 1
      jumpSize *= JUMP_SIZE_INCREASE
    } while (fitnessDelta <= 0 && numTries < MAX_TRIES)

    println("incremental improvement = " + improvement.improvement + " numTries=" + numTries + " jumpSize=" + jumpSize
      + "\n num nodes in improvedParams=" + improvement.parameters.pa.size
      + " fit=" + improvement.parameters.fitness)
    improvement
  }
}