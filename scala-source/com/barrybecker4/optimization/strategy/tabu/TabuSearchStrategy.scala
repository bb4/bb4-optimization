// Copyright by Barry G. Becker, 2026. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.strategy.tabu

import com.barrybecker4.math.MathUtil
import com.barrybecker4.optimization.optimizee.{EvaluationBudgetExceededException, Optimizee}
import com.barrybecker4.optimization.parameter.{ParameterArray, ParameterArrayWithFitness}
import com.barrybecker4.optimization.strategy.OptimizationStrategy
import com.barrybecker4.optimization.strategy.statespace.StateSpaceSearchStrategy

import scala.collection.mutable
import scala.util.Random

/**
  * Tabu search for discrete problems. From the current solution it scores a sample of moves and takes the best one that
  * is not tabu, even if that is worse than the current solution, so it climbs out of local optima. A move is tabu when it
  * would put back something that a recent move took away (see [[com.barrybecker4.optimization.parameter.ParameterArray.createdAttributes]]);
  * the things a move takes away stay forbidden for a tenure drawn from [[TabuSearchConfig]]. A tabu move is still
  * allowed when it gives a new best solution (aspiration), and when every candidate is tabu the best of them is taken.
  *
  * It uses [[com.barrybecker4.optimization.parameter.ParameterArray.proposeLocalMove]] and [[Optimizee.evaluateMove]],
  * so a problem that scores a move by a delta (as the N-queens benchmark does) makes it cheap. For an array without local moves it
  * scores random neighbors instead and forbids returning to a solution it has just left; that works but evaluates a
  * whole solution per candidate, so it is slow.
  *
  * Works with absolute fitness only. When the optimizee is wrapped with an evaluation budget the best solution found
  * so far is returned when the budget is used up.
  *
  * @param optimizee the thing to be optimized.
  * @param rnd       chooses the moves and the tenures.
  */
class TabuSearchStrategy(optimizee: Optimizee, rnd: Random = MathUtil.RANDOM) extends OptimizationStrategy(optimizee) {

  private var config: TabuSearchConfig = TabuSearchConfig.Default

  /** What a candidate would change, scored but not yet built. */
  private case class Candidate(build: () => ParameterArray, fitness: Double, created: Seq[Any], destroyed: Seq[Any])

  def getConfig: TabuSearchConfig = config

  def setConfig(config: TabuSearchConfig): Unit = { this.config = config }

  override def doOptimization(params: ParameterArray, fitnessRange: Double): ParameterArrayWithFitness = {
    require(!optimizee.evaluateByComparison, "Tabu search needs absolute fitness, not comparison")
    var best = ParameterArrayWithFitness(params, Double.PositiveInfinity)
    try best = search(params)
    catch { case _: EvaluationBudgetExceededException => best = bestSoFar.getOrElse(best) }
    best
  }

  private def search(start: ParameterArray): ParameterArrayWithFitness = {
    val tabuUntil = mutable.HashMap[Any, Int]()
    var current = scored(start)
    var best = current
    notifyOfChange(best)
    var stagnant = 0
    var iteration = 0
    while (iteration < config.maxIterations && !isOptimalFitnessReached(best)) {
      iteration += 1
      current = step(current, best, iteration, tabuUntil)
      if (current.fitness < best.fitness) { best = current; stagnant = 0; notifyOfChange(best) } else stagnant += 1
      if (shouldRestart(stagnant)) { current = scored(best.pa.getRandomSample); tabuUntil.clear(); stagnant = 0 }
      trace("iteration " + iteration + " current = " + current.fitness + " best = " + best.fitness)
    }
    best
  }

  private def shouldRestart(stagnantIterations: Int): Boolean =
    config.restartAfterStagnantIterations > 0 && stagnantIterations >= config.restartAfterStagnantIterations

  private def scored(array: ParameterArray): ParameterArrayWithFitness = ParameterArrayWithFitness(array, cleaned(optimizee.evaluateFitness(array)))

  /** A NaN must never look like an improvement. */
  private def cleaned(fitness: Double): Double = if (fitness.isNaN) Double.PositiveInfinity else fitness

  /** Takes the best allowed candidate, and forbids undoing it for a while. */
  private def step(current: ParameterArrayWithFitness, best: ParameterArrayWithFitness, iteration: Int,
                   tabuUntil: mutable.HashMap[Any, Int]): ParameterArrayWithFitness = {
    val candidates = candidatesFrom(current)
    val isTabu = (c: Candidate) => c.created.exists(attribute => tabuUntil.getOrElse(attribute, 0) > iteration)
    val allowed = candidates.filter(c => !isTabu(c) || (config.aspiration && c.fitness < best.fitness))
    val chosen = bestOf(if (allowed.nonEmpty) allowed else candidates)
    val tenure = config.tenureMin + rnd.nextInt(config.tenureMax - config.tenureMin + 1)
    chosen.destroyed.foreach(attribute => tabuUntil(attribute) = iteration + tenure)
    ParameterArrayWithFitness(chosen.build(), chosen.fitness)
  }

  /** The lowest fitness, with the first one winning a tie; the candidates are in random order already. */
  private def bestOf(candidates: Seq[Candidate]): Candidate = candidates.minBy(_.fitness)

  private def candidatesFrom(current: ParameterArrayWithFitness): Seq[Candidate] =
    if (current.pa.supportsLocalMoves) (1 to config.candidateMoves).map(_ => moveCandidate(current))
    else (1 to config.candidateMoves).map(_ => neighborCandidate(current))

  private def moveCandidate(current: ParameterArrayWithFitness): Candidate = {
    val move = current.pa.proposeLocalMove(rnd)
    val fitness = cleaned(optimizee.evaluateMove(current.pa, current.fitness, move))
    Candidate(() => current.pa.applyMove(move), fitness, current.pa.createdAttributes(move), current.pa.destroyedAttributes(move))
  }

  /** Without moves the only thing to forbid is going back to the solution that was left. */
  private def neighborCandidate(current: ParameterArrayWithFitness): Candidate = {
    val neighbor = current.pa.getRandomNeighbor(config.fallbackRadius)
    val fitness = cleaned(optimizee.evaluateFitness(neighbor))
    Candidate(() => neighbor, fitness, Seq(StateSpaceSearchStrategy.stateKey(neighbor)), Seq(StateSpaceSearchStrategy.stateKey(current.pa)))
  }
}
