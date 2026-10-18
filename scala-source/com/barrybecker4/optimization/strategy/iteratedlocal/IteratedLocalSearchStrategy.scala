// Copyright by Barry G. Becker, 2000-2026. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.strategy.iteratedlocal

import com.barrybecker4.math.MathUtil
import com.barrybecker4.optimization.optimizee.{EvaluationBudgetExceededException, Optimizee}
import com.barrybecker4.optimization.parameter.{ParameterArray, ParameterArrayWithFitness}
import com.barrybecker4.optimization.strategy.statespace.StateSpaceSearchStrategy
import com.barrybecker4.optimization.strategy.OptimizationStrategy

import scala.collection.mutable
import scala.util.Random

object IteratedLocalSearchStrategy {
  def strategyFor(
      optimizee: Optimizee,
      config: IteratedLocalSearchConfig,
      rnd: Random = new Random(1)
  ): IteratedLocalSearchStrategy = {
    val strategy = new IteratedLocalSearchStrategy(optimizee, rnd)
    strategy.setConfig(config)
    strategy
  }
}

/**
 * Iterated local search (ILS) and variable neighborhood search (VNS) for discrete problems such as permutations
 * ([[com.barrybecker4.optimization.parameter.PermutedParameterArray]]) and variable length integer sets
 * ([[com.barrybecker4.optimization.parameter.VariableLengthIntSet]]). It works with any [[ParameterArray]],
 * but for continuous parameters [[ParticleSwarmStrategy]] or [[DifferentialEvolutionStrategy]] are better choices.
 *
 * One round of the search:
 *  1. ''Shake'': from the current solution make `k` successive random neighbor moves (the shake level `k`).
 *  1. ''Local search'': from the shaken solution, repeatedly try random neighbors and move to the first one that is
 *     better, until a number of consecutive tries fail to improve (a local optimum).
 *  1. ''Accept'': move to the local optimum if it is better (or, depending on the
 *     [[LocalSearchAcceptance]], equally good or probabilistically worse); otherwise stay.
 *
 * With a fixed shake level this is iterated local search. With the level rising after each failed round and
 * resetting after an improvement it is variable neighborhood search. See [[IteratedLocalSearchConfig]] for the two
 * presets that [[OptimizationStrategyType.ITERATED_LOCAL_SEARCH]] and
 * [[OptimizationStrategyType.VARIABLE_NEIGHBORHOOD_SEARCH]] use. If no new best solution has been found for a while,
 * the search restarts from a random solution (the best found is never lost).
 *
 * How it relates to the other strategies for discrete problems: the local search is the same random neighbor,
 * first improvement descent with a cache of tried solutions that [[HillClimbingStrategy]] uses; what is added is
 * the shake and accept loop, which keeps exploring around good solutions instead of stopping at the first local
 * optimum (like [[SimulatedAnnealingStrategy]], it can escape local optima, but it decides per local optimum, not per
 * move) and instead of restarting from unrelated samples (like [[GlobalHillClimbingStrategy]]).
 *
 * The parameter array only offers random neighbors, not an enumeration of all neighbors, so "local optimum" means
 * `maxStaleNeighbors` failed tries in a row; for a very large neighborhood, raise it. Every distinct solution is
 * evaluated at most once: a shake or neighbor that was already visited is skipped (which also stops a local search
 * from walking back into a local optimum that was already explored).
 *
 * '''Comparison mode (`evaluateByComparison == true`):''' like [[HillClimbingStrategy]], a neighbor is an improvement
 * when `compareFitness(neighbor, current) < 0`, and the reported fitness of a solution is
 * `compareFitness(solution, initialGuess)`.
 *
 * The [[rnd]] parameter is used for the Metropolis acceptance coin flips; neighbors and random samples come from the
 * [[scala.util.Random]] embedded in the [[ParameterArray]] (as for [[SimulatedAnnealingStrategy]]).
 *
 * Budget: when the optimizee is wrapped with an evaluation budget, this strategy catches
 * [[EvaluationBudgetExceededException]] and returns the best solution found so far.
 *
 * @param optimizee the thing to be optimized.
 * @param rnd       random source for the Metropolis acceptance decisions.
 */
class IteratedLocalSearchStrategy(optimizee: Optimizee, rnd: Random = MathUtil.RANDOM)
  extends OptimizationStrategy(optimizee) {

  private var config: IteratedLocalSearchConfig = IteratedLocalSearchConfig.Default

  def getConfig: IteratedLocalSearchConfig = config

  def setConfig(config: IteratedLocalSearchConfig): Unit = {
    this.config = config
  }

  /**
   * @param params       the initial guess; the search starts here, and in comparison mode it is the baseline.
   * @param fitnessRange approximate scale of the objective; only used by [[LocalSearchAcceptance.Metropolis]].
   * @return the best parameters found, with their fitness.
   */
  override def doOptimization(params: ParameterArray, fitnessRange: Double): ParameterArrayWithFitness = {
    val cfg = config
    val temperatureScale = if (fitnessRange > 0.0 && fitnessRange.isFinite) fitnessRange else 1.0
    val baseline = params
    val visited = mutable.HashSet[String]()
    var best: Option[ParameterArrayWithFitness] = None

    /** Absolute fitness, or in comparison mode the fitness relative to the initial guess. */
    def fitnessOf(candidate: ParameterArray): Double =
      if (optimizee.evaluateByComparison) optimizee.compareFitness(candidate, baseline)
      else {
        val fitness = optimizee.evaluateFitness(candidate)
        if (fitness.isNaN) Double.PositiveInfinity else fitness // a NaN must never look like an improvement
      }

    /** Negative if `candidate` is better than `reference`, positive if it is worse. */
    def deltaBetween(candidate: ParameterArrayWithFitness, reference: ParameterArrayWithFitness): Double =
      if (optimizee.evaluateByComparison) optimizee.compareFitness(candidate.pa, reference.pa)
      else candidate.fitness - reference.fitness

    def recordBest(candidate: ParameterArrayWithFitness): Unit =
      if (best.forall(current => candidate.fitness < current.fitness)) {
        best = Some(candidate)
        notifyOfChange(candidate)
      }

    /** First improvement descent over random neighbors; stops after `maxStaleNeighbors` failed tries in a row. */
    def localSearch(start: ParameterArrayWithFitness): ParameterArrayWithFitness = {
      var current = start
      var staleNeighbors = 0
      while (staleNeighbors < cfg.maxStaleNeighbors && !isOptimalFitnessReached(current)) {
        val neighbor = current.pa.getRandomNeighbor(cfg.localRadius)
        if (visited.add(StateSpaceSearchStrategy.stateKey(neighbor))) {
          val scored = ParameterArrayWithFitness(neighbor, fitnessOf(neighbor))
          recordBest(scored)
          if (deltaBetween(scored, current) < 0) {
            current = scored
            staleNeighbors = 0
          } else staleNeighbors += 1
        } else staleNeighbors += 1
      }
      current
    }

    /** @return the evaluated shaken solution, or None if the shake landed on a solution that was already visited. */
    def shake(from: ParameterArrayWithFitness, level: Int): Option[ParameterArrayWithFitness] = {
      var shaken = from.pa
      for (_ <- 0 until level) shaken = shaken.getRandomNeighbor(cfg.localRadius)
      if (visited.add(StateSpaceSearchStrategy.stateKey(shaken))) {
        val scored = ParameterArrayWithFitness(shaken, fitnessOf(shaken))
        recordBest(scored)
        Some(scored)
      } else None
    }

    /** @return a local optimum found from a random solution, or None if that solution was already visited. */
    def restart(from: ParameterArrayWithFitness): Option[ParameterArrayWithFitness] = {
      val sample = from.pa.getRandomSample
      if (visited.add(StateSpaceSearchStrategy.stateKey(sample))) {
        val scored = ParameterArrayWithFitness(sample, fitnessOf(sample))
        recordBest(scored)
        Some(localSearch(scored))
      } else None
    }

    /** Whether a local optimum that is not strictly better than the current solution should still replace it. */
    def acceptsNonImproving(delta: Double): Boolean = cfg.acceptance match {
      case LocalSearchAcceptance.Better => false
      case LocalSearchAcceptance.BetterOrEqual => delta == 0.0
      case LocalSearchAcceptance.Metropolis(relativeTemperature) =>
        rnd.nextDouble() < math.exp(-delta / (relativeTemperature * temperatureScale))
    }

    try {
      val initial = ParameterArrayWithFitness(params, fitnessOf(params))
      visited += StateSpaceSearchStrategy.stateKey(params)
      recordBest(initial)

      var current = localSearch(initial)
      var level = cfg.minShakeLevel
      var stagnantIterations = 0
      var iteration = 0

      while (iteration < cfg.maxIterations && !isOptimalFitnessReached(best.get)) {
        val bestBefore = best.get.fitness
        var improvedCurrent = false

        shake(current, level).foreach { shaken =>
          val localOptimum = localSearch(shaken)
          val delta = deltaBetween(localOptimum, current)
          improvedCurrent = delta < 0
          if (improvedCurrent || acceptsNonImproving(delta)) current = localOptimum
        }

        iteration += 1
        stagnantIterations = if (best.get.fitness < bestBefore) 0 else stagnantIterations + 1
        level = if (improvedCurrent || level >= cfg.maxShakeLevel) cfg.minShakeLevel else level + 1
        log(iteration, best.get, level.toDouble, bestBefore - best.get.fitness, "shake level " + level)
        trace("iteration " + iteration + " best fitness = " + best.get.fitness + " next shake level = " + level)

        if (cfg.restartAfterStagnantIterations > 0 && stagnantIterations >= cfg.restartAfterStagnantIterations) {
          restart(current).foreach { restarted =>
            current = restarted
            level = cfg.minShakeLevel
          }
          stagnantIterations = 0
        }
      }
    } catch {
      case _: EvaluationBudgetExceededException =>
        () // fall through and return the best found so far
    }

    best.getOrElse(ParameterArrayWithFitness(params, Double.MaxValue))
  }
}
