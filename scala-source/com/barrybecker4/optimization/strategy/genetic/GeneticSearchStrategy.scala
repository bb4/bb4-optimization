// Copyright by Barry G. Becker, 2000-2026. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.strategy.genetic

import com.barrybecker4.common.format.FormatUtil
import com.barrybecker4.math.MathUtil
import com.barrybecker4.optimization.optimizee.{EvaluationBudgetExceededException, Optimizee}
import com.barrybecker4.optimization.parameter.ParameterArrayWithFitness.given
import com.barrybecker4.optimization.parameter.{ParameterArray, ParameterArrayWithFitness}
import com.barrybecker4.optimization.strategy.OptimizationStrategy

import scala.collection.mutable.ArrayBuffer
import scala.util.Random

/**
  * Genetic Algorithm (evolutionary) optimization strategy.
  * Many different strategies are possible to alter the population for each successive iteration.
  * The 2 primary ones that I use here are unary mutation and cross-over.
  * See Chapter 6 in "How to Solve it: Modern Heuristics" for more info.
  *
  * This class seeds the population and reports progress; the generational loop is [[Evolution]], refilling a culled
  * population is [[PopulationRefiller]], and the best individual is kept by [[ChampionTracker]].
  *
  * '''Comparison mode (`evaluateByComparison == true`):''' every generation the population is re-scored against the
  * generation baseline, which is the best individual so far, so a member's fitness is its improvement over that
  * baseline (negative is better). The champion's fitness is kept as the ''cumulative'' improvement over the
  * initial guess, i.e. the sum of the per-generation improvements. For score-difference optimizees this equals
  * `compareFitness(champion, initialGuess)`.
  *
  * Budget: when the optimizee is wrapped with an evaluation budget, the best individual evaluated so far is
  * returned.
  *
  * @param optimizee the thing to be optimized.
  */
class GeneticSearchStrategy(
    optimizee: Optimizee,
    rnd: Random = MathUtil.RANDOM,
    private var searchConfig: GeneticSearchConfig = GeneticSearchConfig.default
) extends OptimizationStrategy(optimizee) {

  /** @deprecated use [[getConfig]] / [[setConfig]] */
  def config: GeneticSearchConfig = searchConfig

  def getConfig: GeneticSearchConfig = searchConfig

  def setConfig(c: GeneticSearchConfig): Unit = {
    searchConfig = c
  }

  def setImprovementEpsilon(eps: Double): Unit = {
    searchConfig = searchConfig.copy(improvementEpsilon = eps)
  }

  /** This is the desired number of members to be maintained in the population at any time.
    * Might not always get this many if there are duplicates or the search space is small.
    */
  private var desiredPopulationSize = 0

  /** Best individual seen across generations (absolute or cumulative compare-mode scores). */
  private val champions = new ChampionTracker(optimizee.evaluateByComparison, updateRunBest)

  /** Finds a local minima using a genetic algorithm (evolutionary) search.
    * Stop iterating as soon as the average evaluation score of the population
    * does not significantly improve.
    * @param params the initial value for the parameters to optimize.
    * @param fitnessRange the approximate absolute value of the fitnessRange.
    * @return the optimized params.
    */
  override def doOptimization(params: ParameterArray, fitnessRange: Double): ParameterArrayWithFitness = {
    desiredPopulationSize = params.getSamplePopulationSize
    champions.reset()
    val seeded =
      try Some(findInitialPopulation(params))
      catch {
        case _: EvaluationBudgetExceededException => None
      }
    seeded match {
      case None => champions.orElse(ParameterArrayWithFitness(params, Double.MaxValue))
      case Some(population) =>
        val start = population.min
        champions.consider(start)
        evolve(population, start, fitnessRange)
    }
  }

  /** Visible for tests in `com.barrybecker4.optimization`. */
  private[optimization] def findInitialPopulation(params: ParameterArray): ArrayBuffer[ParameterArrayWithFitness] = {
    if (desiredPopulationSize <= 0) desiredPopulationSize = params.getSamplePopulationSize
    val seedMember = evaluateSeed(params)
    champions.consider(seedMember)
    val population = ArrayBuffer(seedMember)

    val neighbors = InitialPopulation.proposeNeighbors(params, desiredPopulationSize, searchConfig.initialNeighborRadius)
    evaluateOffspring(neighbors, params).foreach { member =>
      population += member
      champions.considerMember(member)
    }
    if (population.size <= 1) throw new IllegalStateException("No random neighbors found for " + params)
    population
  }

  /** In compare mode the seed is the baseline everything else is scored against, so its own score is 0. */
  private def evaluateSeed(params: ParameterArray): ParameterArrayWithFitness = {
    val fitness = if (optimizee.evaluateByComparison) 0.0 else optimizee.evaluateFitness(params)
    ParameterArrayWithFitness(params, fitness)
  }

  private def evolve(population: ArrayBuffer[ParameterArrayWithFitness],
                     start: ParameterArrayWithFitness,
                     fitnessRange: Double): ParameterArrayWithFitness = {
    val evolution = newEvolution(population, fitnessRange)
    val budgetExhausted = runGenerations(evolution, start)
    val result = champions.orElse(start)
    if (!budgetExhausted) evolution.stopReason(isOptimalFitnessReached(result)).foreach(r => trace("stopped because " + r))
    trace("----------------------- done -------------------")
    if (isLoggingToFile) log(evolution.generations, result, 0, 0, FormatUtil.formatNumber(evolution.generations))
    result
  }

  private def newEvolution(population: ArrayBuffer[ParameterArrayWithFitness], fitnessRange: Double): Evolution =
    new Evolution(population, champions, newRefiller(), reevaluatePopulationFitness, searchConfig,
      desiredPopulationSize, fitnessRange)

  private def newRefiller(): PopulationRefiller =
    new PopulationRefiller(optimizee, searchConfig, rnd, evaluateOffspring, addedToPopulation)

  /** @return true if the evaluation budget ran out before the evolution finished */
  private def runGenerations(evolution: Evolution, start: ParameterArrayWithFitness): Boolean =
    try {
      while (evolution.stopReason(isOptimalFitnessReached(champions.orElse(start))).isEmpty)
        reportProgress(evolution.nextGeneration())
      false
    } catch {
      case _: EvaluationBudgetExceededException => true
    }

  private def reportProgress(report: GenerationReport): Unit = {
    log(report.index, report.champion, report.radius, report.delta, "---")
    trace(s"currBest = ${report.champion} \nrecBest = ${report.previousChampion}        ct=${report.index}")
    trace(s"delta fitness =${report.delta}      rbrRadius = ${report.radius}  " +
      s"improvementEpsilon = ${searchConfig.improvementEpsilon}")
    notifyOfChange(report.champion)
  }

  private def addedToPopulation(member: ParameterArrayWithFitness): Unit = {
    val inChampionFrame = champions.toChampionFrame(member)
    champions.consider(inChampionFrame)
    notifyOfChange(inChampionFrame)
  }

  /**
    * Evaluate a batch of candidate parameter arrays (sequential by default).
    * [[ConcurrentGeneticSearchStrategy]] overrides for parallel fitness evaluation.
    * Parent selection / crossover stay on the calling thread.
    *
    * If the evaluation budget runs out part way, the candidates scored so far are still recorded as champion
    * candidates before the exception propagates.
    */
  protected def evaluateOffspring(
      candidates: Seq[ParameterArray],
      generationBaseline: ParameterArray
  ): Seq[ParameterArrayWithFitness] = {
    val scored = ArrayBuffer[ParameterArrayWithFitness]()
    try candidates.foreach(pa => scored += evaluateParameterArray(pa, generationBaseline))
    catch {
      case e: EvaluationBudgetExceededException =>
        recordPartialResults(scored)
        throw e
    }
    scored.toSeq
  }

  /** For subclasses: keep what was learned from the candidates scored before an evaluation failed. */
  protected final def recordPartialResults(scored: Iterable[ParameterArrayWithFitness]): Unit =
    scored.foreach(champions.considerMember)

  protected def evaluateParameterArray(pa: ParameterArray, generationBaseline: ParameterArray): ParameterArrayWithFitness = {
    val fitness =
      if (optimizee.evaluateByComparison) optimizee.compareFitness(pa, generationBaseline)
      else optimizee.evaluateFitness(pa)
    ParameterArrayWithFitness(pa, fitness)
  }

  /** Refresh per-individual fitness when using relative fitness vs the previous generation best. */
  protected def reevaluatePopulationFitness(population: ArrayBuffer[ParameterArrayWithFitness],
                                            baseline: ParameterArray): Unit =
    if (optimizee.evaluateByComparison) {
      for (i <- population.indices) {
        val pa = population(i).pa
        population(i) = ParameterArrayWithFitness(pa, optimizee.compareFitness(pa, baseline))
      }
    }

  /** Runs one refill pass as in the generation loop; visible for tests. */
  private[optimization] def refillAfterCullForTesting(
      population: ArrayBuffer[ParameterArrayWithFitness],
      keepSize: Int,
      generationBaseline: ParameterArray,
      desiredPopSize: Int
  ): Unit =
    newRefiller().refill(population, keepSize, desiredPopSize, generationBaseline, searchConfig.nbrRadius)

  /** Inject elitism for tests after a cull. */
  private[optimization] def ensureEliteForTesting(population: ArrayBuffer[ParameterArrayWithFitness]): Unit =
    if (searchConfig.elitismEnabled) champions.ensureInPopulation(population, desiredPopulationSize)

  private[optimization] def setGlobalChampionForTesting(c: ParameterArrayWithFitness): Unit =
    champions.set(c)
}
