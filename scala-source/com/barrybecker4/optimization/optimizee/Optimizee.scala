// Copyright by Barry G. Becker, 2000-2018. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.optimizee

import com.barrybecker4.optimization.parameter.{Move, ParameterArray}

import scala.util.Random


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
    * A strategy that can describe a neighbor as a [[com.barrybecker4.optimization.parameter.Move]] asks this instead of building the neighbor and calling
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

  /**
    * Lets the problem put right a candidate that a strategy made by crossover or mutation, when it is not acceptable
    * as it is: a piece used twice, the wrong number of tiles of some color. Genetic search calls this on every child
    * and mutant before it scores it. The default accepts every candidate as it is.
    *
    * Return the same instance when there is nothing to fix. A strategy that scored the candidate from a move without
    * building it has to build and score it again if a different array comes back. The result must be the same kind of
    * array as the candidate.
    */
  def repair(candidate: ParameterArray): ParameterArray = candidate

  /**
    * Lets the problem suggest a move that it expects to be more useful than a random one, because it knows something
    * about its own structure, for example a swap that involves a cell whose edges do not match. A strategy that is
    * configured with a guided move probability asks this now and then in place of proposing a random move, and uses a
    * random move when this returns `None`, as the default does. Only used for absolute fitness, with an array that
    * [[com.barrybecker4.optimization.parameter.ParameterArray.supportsMoves]].
    *
    * @param current the array the move starts from
    * @param radius how big a random move would be, a hint for how big to make this one
    * @param rnd the random numbers to use, so that a seeded run stays repeatable
    */
  def proposeGuidedMove(current: ParameterArray, radius: Double, rnd: Random): Option[Move] = None

  /** if non-0, then we terminate when the fitness reaches this value.
    * @return the best (largest) expected fitness value.
    */
  def getOptimalFitness: Double
}