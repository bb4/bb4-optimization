// Copyright by Barry G. Becker, 2013 - 2018. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.strategy.gradient

import com.barrybecker4.optimization.optimizee.Optimizee
import com.barrybecker4.optimization.parameter.{ParameterArray, ParameterArrayWithFitness}
import DiscreteImprovementFinder._
import scala.collection.mutable
import ImprovementFinder.INITIAL_JUMP_SIZE


object DiscreteImprovementFinder {
  private val JUMP_SIZE_DECREASE = 0.98
  private val JUMP_SIZE_INCREASE = 1.01
}

/**
  * Finds incremental improvement in a discrete problem space.
  * @author Barry Becker
  */
class DiscreteImprovementFinder(var startingParams: ParameterArrayWithFitness,
                                baselineParams: ParameterArray,
                                trace: String => Unit = _ => (),
                                useMoves: Boolean = false) extends ImprovementFinder {

  /** Try to find a parameterArray that is better than what we have now by evaluating using the optimizee passed in.
    * Try swapping parameters randomly until we find an improvement (if we can).
    * @param optimizee something that can evaluate parameterArrays.
    * @param cache set of parameters that have already been tested. This is important for cases where the
    *               parameters are discrete and not continuous.
    * @return the improvement which contains the improved parameter array and possibly a revised jumpSize.
    */
  def findIncrementalImprovement(optimizee: Optimizee,
                                 lastImprovement: Option[Improvement],
                                 cache: mutable.Set[ParameterArray]): Improvement = {
    val current = lastImprovement.map(_.parameters).getOrElse(startingParams)
    if (useMoves && !optimizee.evaluateByComparison && current.pa.supportsMoves)
      findImprovementByMoves(optimizee, lastImprovement, current)
    else findImprovementByBuilding(optimizee, lastImprovement, cache)
  }

  /**
    * The same search with moves: each try is a move, and a neighbor is built only for the one that improves. The cache is
    * not consulted, so a move that was tried before is tried again, which costs a delta and not a neighbor.
    */
  private def findImprovementByMoves(optimizee: Optimizee,
                                     lastImprovement: Option[Improvement],
                                     current: ParameterArrayWithFitness): Improvement = {
    var numTries = 0
    var fitnessDelta = 1.0
    var jumpSize = lastImprovement.map(_.jumpSize * JUMP_SIZE_DECREASE).getOrElse(INITIAL_JUMP_SIZE)
    var improvement = Improvement(current, 0, jumpSize)
    while (fitnessDelta >= 0 && numTries < DISCRETE_MAX_TRIES) {
      val move = current.pa.proposeMove(jumpSize)
      val fitness = optimizee.evaluateMove(current.pa, current.fitness, move)
      fitnessDelta = fitness - current.fitness
      if (fitnessDelta < 0)
        improvement = Improvement(ParameterArrayWithFitness(current.pa.applyMove(move), fitness), fitnessDelta, jumpSize)
      numTries += 1
      jumpSize *= JUMP_SIZE_INCREASE
    }
    trace("incremental improvement = " + improvement + " numTries=" + numTries)
    improvement
  }

  private def findImprovementByBuilding(optimizee: Optimizee,
                                        lastImprovement: Option[Improvement],
                                        cache: mutable.Set[ParameterArray]): Improvement = {
    var numTries = 0
    var fitnessDelta = 1.0
    var jumpSize = lastImprovement.map(_.jumpSize * JUMP_SIZE_DECREASE).getOrElse(INITIAL_JUMP_SIZE)
    val currentParams: ParameterArrayWithFitness = lastImprovement.map(_.parameters).getOrElse(startingParams)

    var improvement = Improvement(currentParams, 0, jumpSize)

    while (fitnessDelta >= 0 && numTries < DISCRETE_MAX_TRIES) {
      val nbrParam = currentParams.pa.getRandomNeighbor(jumpSize)
      fitnessDelta = .0

      if (!cache.contains(nbrParam)) {
        cache += nbrParam

        val nbr: ParameterArrayWithFitness =
          if (optimizee.evaluateByComparison) {
            fitnessDelta = optimizee.compareFitness(nbrParam, currentParams.pa)
            val absoluteFitness = optimizee.compareFitness(nbrParam, baselineParams)
            ParameterArrayWithFitness(nbrParam, absoluteFitness)
          } else {
            val fitness = optimizee.evaluateFitness(nbrParam)
            fitnessDelta = fitness - currentParams.fitness
            ParameterArrayWithFitness(nbrParam, fitness)
          }

        if (fitnessDelta < 0)
          improvement = Improvement(nbr, fitnessDelta, jumpSize)
      }
      // Widen search radius every try (including cache hits) so we do not stall when many neighbors repeat.
      numTries += 1
      jumpSize *= JUMP_SIZE_INCREASE
    }

    trace("incremental improvement = " + improvement + " numTries=" + numTries)
    improvement
  }
}