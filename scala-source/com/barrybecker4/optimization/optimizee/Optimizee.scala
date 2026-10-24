// Copyright by Barry G. Becker, 2000-2018. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.optimizee

import com.barrybecker4.optimization.parameter.{Move, ParameterArray}


/**
  * This interface needs to be implemented for any object you wish to have optimized.
  * Optimization of an Optimizee is done by the Optimizer1 class.
  *
  * @author Barry Becker
  */
trait Optimizee {

  /** @return the name of the thing to be optimized */
  def getName: String

  /** If true is returned, then compareFitness will be used and evaluateFitness will not,
    * otherwise the reverse will be true.
    * @return return true if we evaluate the fitness by comparison
    */
  def evaluateByComparison: Boolean

  /** Assigns a measure of fitness to the specified set of parameters.
    * This method must return a value greater than or equal to 0.
    * This method is used if evaluateByComparison returns false.
    * Zero is considered optimal. Higher values imply lesser fitness.
    * @param params the set of parameters to misc
    * @return the fitness measure. The lower the better
    */
  def evaluateFitness(params: ParameterArray): Double

  /** Compares two sets of parameters.  Used if evaluateByComparison returns true.
    * @return the amount that params1 are better than params2. May be positive if params2 are better than params1.
    */
  def compareFitness(params1: ParameterArray, params2: ParameterArray): Double

  /**
    * The fitness that `current` would have after `move`, for an optimizee that does not evaluate by comparison.
    * A strategy that can describe a neighbor as a [[Move]] asks this instead of building the neighbor and calling
    * [[evaluateFitness]] on it, and builds the neighbor only if it keeps the move.
    *
    * The default builds the neighbor and evaluates it all over, so no optimizee has to change. Override it when the
    * new fitness can be worked out from the move alone, for example the length of a tour after a swap, which changes
    * a few edges. It must give what `evaluateFitness(current.applyMove(move))` gives
    * (see [[DeltaCheckingOptimizee]], which checks that). It may be called from several threads at once, so it should
    * not keep state between calls.
    *
    * @param current the array the move starts from
    * @param currentFitness `evaluateFitness(current)`, which the strategy already knows
    * @param move a move that `current` understands
    * @return the fitness after the move
    */
  def evaluateMove(current: ParameterArray, currentFitness: Double, move: Move): Double =
    evaluateFitness(current.applyMove(move))

  /** if non-0, then we terminate when the fitness reaches this value.
    * @return the best (largest) expected fitness value.
    */
  def getOptimalFitness: Double
}