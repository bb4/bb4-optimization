// Copyright by Barry G. Becker, 2026. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.strategy.restart

import com.barrybecker4.optimization.optimizee.{BudgetedOptimizee, EvaluationBudgetExceededException, ExhaustionWatchingOptimizee, Optimizee}
import com.barrybecker4.optimization.parameter.{ParameterArray, ParameterArrayWithFitness}
import com.barrybecker4.optimization.strategy.OptimizationStrategy

import scala.annotation.tailrec
import scala.util.Random

object RestartingStrategy {

  /** The i'th term (counting from 1) of the Luby sequence 1 1 2 1 1 2 4 1 1 2 1 1 2 4 8 ... */
  @tailrec
  def luby(i: Int): Int = {
    require(i >= 1, s"The Luby sequence starts at 1, got $i")
    var k = 1
    while ((1L << k) - 1 < i) k += 1
    if ((1L << k) - 1 == i) 1 << (k - 1) else luby(i - (1 << (k - 1)) + 1)
  }
}

/**
  * Runs another strategy again and again, each time with a limited number of evaluations, and keeps the best result.
  * It helps with a problem where the inner strategy either finds a solution soon or gets stuck for good, which is the
  * case for many constraint problems. The budget of run `i` is `luby(i) * baseBudget` (see [[RestartingConfig]]).
  *
  * The first run starts from the initial guess. After that a run starts from a random sample, or, with a chance that
  * grows while restarts keep failing to beat the best, from a perturbed copy of the best solution.
  * The search stops when the goal fitness is reached, when the restarts or the total budget are used up, or when an
  * evaluation limit set outside this strategy (a [[BudgetedOptimizee]] around it) is reached.
  *
  * Works with absolute fitness only: the results of separate runs cannot be compared when fitness is relative to each
  * run's own starting point. The inner strategy must report its progress with `notifyOfChange`, as the built-in
  * ones do, so that the best of a run that was cut off is not lost.
  *
  * @param optimizee    the thing to be optimized.
  * @param innerFactory builds the strategy for one run, given the optimizee that limits its evaluations.
  * @param rnd          chooses between a random and a perturbed start.
  */
class RestartingStrategy(optimizee: Optimizee, innerFactory: Optimizee => OptimizationStrategy, rnd: Random = new Random(1))
  extends OptimizationStrategy(optimizee) {

  private var config: RestartingConfig = RestartingConfig()
  private var restarts = 0
  private var evaluations = 0

  def getConfig: RestartingConfig = config

  def setConfig(config: RestartingConfig): Unit = { this.config = config }

  /** Number of runs of the inner strategy in the last call to `doOptimization`. */
  def runCount: Int = restarts

  /** Evaluations used by all the runs of the last call to `doOptimization`. */
  def evaluationsUsed: Int = evaluations

  override def doOptimization(params: ParameterArray, fitnessRange: Double): ParameterArrayWithFitness = {
    require(!optimizee.evaluateByComparison, "Restarting needs absolute fitness, not comparison")
    val outerLimit = new ExhaustionWatchingOptimizee(optimizee)
    var best = ParameterArrayWithFitness(params, Double.MaxValue)
    var base = config.baseBudget
    var failedInARow = 0
    restarts = 0
    evaluations = 0

    while (restarts < config.maxRestarts && evaluations < config.totalBudget
      && !isOptimalFitnessReached(best) && !outerLimit.sawExhaustion) {
      restarts += 1
      val budget = budgetOfRun(restarts, base)
      val start = if (restarts == 1) params else nextStart(best, failedInARow)
      val (result, finishedInBudget) = runInner(outerLimit, start, budget, fitnessRange)
      if (restarts == 1 && finishedInBudget && config.adaptBaseBudget) base = math.max(1, evaluations)
      failedInARow = if (result.fitness < best.fitness) 0 else failedInARow + 1
      if (result.fitness < best.fitness) best = result
      log(restarts, best, budget.toDouble, 0.0, "restart " + restarts)
      trace("restart " + restarts + " budget " + budget + " best fitness = " + best.fitness)
    }
    best
  }

  /** The Luby length times the base, but no more than what is left of the total budget. */
  private def budgetOfRun(run: Int, base: Int): Int = {
    val wanted = RestartingStrategy.luby(run).toLong * base
    math.max(1L, math.min(wanted, config.totalBudget.toLong - evaluations)).toInt
  }

  /** @return the best of the run, and whether the strategy ended by itself rather than by running out of budget. */
  private def runInner(outerLimit: Optimizee, start: ParameterArray, budget: Int, fitnessRange: Double)
      : (ParameterArrayWithFitness, Boolean) = {
    val limited = new BudgetedOptimizee(outerLimit, budget)
    val inner = innerFactory(limited)
    copyRunConfigurationTo(inner)
    val result =
      try inner.doOptimization(start, fitnessRange)
      catch { case _: EvaluationBudgetExceededException => inner.bestSoFar.getOrElse(ParameterArrayWithFitness(start, Double.MaxValue)) }
    evaluations += limited.evaluationsUsed
    inner.bestSoFar.foreach(notifyOfChange)
    (result, !limited.wasBudgetExhausted)
  }

  /** A random sample, or, with a chance that rises with each failed restart, a perturbation of the best. */
  private def nextStart(best: ParameterArrayWithFitness, failedInARow: Int): ParameterArray = {
    val chance = math.min(config.maxPerturbationProbability, config.perturbationStep * failedInARow)
    if (rnd.nextDouble() < chance) best.pa.getRandomNeighbor(config.perturbRadius) else best.pa.getRandomSample
  }
}
