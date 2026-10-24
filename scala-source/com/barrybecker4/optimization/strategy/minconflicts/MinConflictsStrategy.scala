// Copyright by Barry G. Becker, 2026. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.strategy.minconflicts

import com.barrybecker4.math.MathUtil
import com.barrybecker4.optimization.optimizee.{EvaluationBudgetExceededException, Optimizee}
import com.barrybecker4.optimization.parameter.{ParameterArray, ParameterArrayWithFitness}
import com.barrybecker4.optimization.strategy.OptimizationStrategy

import scala.util.Random

/**
  * The min-conflicts heuristic for constraint problems whose fitness is the number of broken constraints (so that 0 is
  * a solution). Each step takes a position at random from [[Optimizee.conflictedPositions]], scores every move of
  * that position ([[com.barrybecker4.optimization.parameter.ParameterArray.movesAt]]), and takes the one with the fewest
  * conflicts left, or, with the noise probability, a random one. Unlike hill climbing it moves even when no move improves,
  * which walks across plateaus.
  *
  * It is only as good as the conflict information. When the optimizee does not report conflicted positions
  * (the default), the position is chosen at random from all of them and the search is a randomized greedy search that
  * does far less than it can. The array must have local moves ([[com.barrybecker4.optimization.parameter.ParameterArray.supportsLocalMoves]]).
  *
  * Works with absolute fitness only. When the optimizee is wrapped with an evaluation budget the best solution found
  * so far is returned when the budget is used up.
  *
  * @param optimizee the thing to be optimized.
  * @param rnd       chooses positions, noise and ties.
  */
class MinConflictsStrategy(optimizee: Optimizee, rnd: Random = MathUtil.RANDOM) extends OptimizationStrategy(optimizee) {

  private var config: MinConflictsConfig = MinConflictsConfig.Default

  def getConfig: MinConflictsConfig = config

  def setConfig(config: MinConflictsConfig): Unit = { this.config = config }

  override def doOptimization(params: ParameterArray, fitnessRange: Double): ParameterArrayWithFitness = {
    require(!optimizee.evaluateByComparison, "Min-conflicts needs absolute fitness, not comparison")
    require(params.supportsLocalMoves, "Min-conflicts needs an array with local moves")
    var best = ParameterArrayWithFitness(params, Double.PositiveInfinity)
    try best = search(params)
    catch { case _: EvaluationBudgetExceededException => best = bestSoFar.getOrElse(best) }
    best
  }

  private def search(start: ParameterArray): ParameterArrayWithFitness = {
    var current = scored(start)
    var best = current
    notifyOfChange(best)
    var stagnant = 0
    var step = 0
    while (step < config.maxSteps && !isOptimalFitnessReached(best)) {
      step += 1
      current = takeStep(current)
      if (current.fitness < best.fitness) { best = current; stagnant = 0; notifyOfChange(best) } else stagnant += 1
      if (shouldRestart(stagnant)) { current = scored(best.pa.getRandomSample); stagnant = 0 }
      trace("step " + step + " current = " + current.fitness + " best = " + best.fitness)
    }
    best
  }

  private def shouldRestart(stagnantSteps: Int): Boolean =
    config.restartAfterStagnantSteps > 0 && stagnantSteps >= config.restartAfterStagnantSteps

  private def scored(array: ParameterArray): ParameterArrayWithFitness =
    ParameterArrayWithFitness(array, cleaned(optimizee.evaluateFitness(array)))

  /** A NaN must never look like an improvement. */
  private def cleaned(fitness: Double): Double = if (fitness.isNaN) Double.PositiveInfinity else fitness

  /** A conflicted position, or any position if the optimizee cannot say which are. */
  private def pickPosition(current: ParameterArray): Int = {
    val conflicted = optimizee.conflictedPositions(current)
    if (conflicted.nonEmpty) conflicted(rnd.nextInt(conflicted.size)) else rnd.nextInt(current.size)
  }

  private def takeStep(current: ParameterArrayWithFitness): ParameterArrayWithFitness = {
    val moves = current.pa.movesAt(pickPosition(current.pa)).toIndexedSeq
    if (moves.isEmpty) current
    else {
      val fitnesses = moves.map(move => cleaned(optimizee.evaluateMove(current.pa, current.fitness, move)))
      val chosen = if (rnd.nextDouble() < config.noiseProbability) rnd.nextInt(moves.size) else indexOfBest(fitnesses)
      ParameterArrayWithFitness(current.pa.applyMove(moves(chosen)), fitnesses(chosen))
    }
  }

  /** The index of the lowest fitness; a tie goes to a random one of them, or the first. */
  private def indexOfBest(fitnesses: IndexedSeq[Double]): Int = {
    val ties = fitnesses.indices.filter(i => fitnesses(i) == fitnesses.min)
    if (config.randomTieBreaking) ties(rnd.nextInt(ties.size)) else ties.head
  }
}
