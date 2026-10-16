// Copyright by Barry G. Becker, 2000-2026. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.strategy

import com.barrybecker4.optimization.optimizee.{EvaluationBudgetExceededException, Optimizee}
import com.barrybecker4.common.format.FormatUtil
import com.barrybecker4.math.MathUtil
import com.barrybecker4.optimization.parameter.{ParameterArray, ParameterArrayWithFitness, ParameterCrossover}
import ParameterArrayWithFitness.given

import scala.collection.mutable.ArrayBuffer
import scala.util.Random

object GeneticSearchStrategy {

  /** @deprecated use [[GeneticSearchConfig.DEFAULT_IMPROVEMENT_EPS]] */
  private[strategy] val DEFAULT_IMPROVEMENT_EPS: Double = GeneticSearchConfig.DEFAULT_IMPROVEMENT_EPS

  /** True when two parameter arrays represent the same candidate (not merely the same reference). */
  private[strategy] def sameSolution(a: ParameterArray, b: ParameterArray): Boolean =
    (a eq b) || (a.getClass == b.getClass && a.toCSVString == b.toCSVString)

  private[strategy] def containsSolution(
      population: Iterable[ParameterArrayWithFitness],
      pa: ParameterArray
  ): Boolean =
    population.exists(m => sameSolution(m.pa, pa))
}

/**
  * Genetic Algorithm (evolutionary) optimization strategy.
  * Many different strategies are possible to alter the population for each successive iteration.
  * The 2 primary ones that I use here are unary mutation and cross-over.
  * See Chapter 6 in "How to Solve it: Modern Heuristics" for more info.
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
    nbrRadius = c.nbrRadius
    improvementEpsilon = c.improvementEpsilon
  }

  /** radius to look for neighbors in  */
  private var nbrRadius = searchConfig.nbrRadius

  /** This is the desired number of members to be maintained in the population at any time.
    * Might not always get this many if there are duplicates or the search space is small.
    */
  private var desiredPopulationSize = 0

  /** If we don't improve by at least this amount between iterations, terminate.  */
  protected var improvementEpsilon: Double = searchConfig.improvementEpsilon

  /** Best individual seen across generations (absolute or compare-mode scores). */
  private var globalChampion: Option[ParameterArrayWithFitness] = None

  def setImprovementEpsilon(eps: Double): Unit = {
    improvementEpsilon = eps
    searchConfig = searchConfig.copy(improvementEpsilon = eps)
  }

  /** Finds a local minima using a genetic algorithm (evolutionary) search.
    * Stop iterating as soon as the average evaluation score of the population
    * does not significantly improve.
    * @param params the initial value for the parameters to optimize.
    * @param fitnessRange the approximate absolute value of the fitnessRange.
    * @return the optimized params.
    */
  override def doOptimization(params: ParameterArray, fitnessRange: Double): ParameterArrayWithFitness = {
    desiredPopulationSize = params.getSamplePopulationSize
    globalChampion = None
    nbrRadius = searchConfig.nbrRadius

    val population =
      try findInitialPopulation(params)
      catch {
        case _: EvaluationBudgetExceededException =>
          return championOrFallback(params)
      }
    val lastBest = population.min
    considerChampion(lastBest)
    try evolve(lastBest, population, fitnessRange)
    catch {
      case _: EvaluationBudgetExceededException =>
        championOr(population.min)
    }
  }

  /** Visible for tests in `com.barrybecker4.optimization`. */
  private[optimization] def findInitialPopulation(params: ParameterArray): ArrayBuffer[ParameterArrayWithFitness] = {
    if (desiredPopulationSize <= 0) desiredPopulationSize = params.getSamplePopulationSize
    val population = ArrayBuffer[ParameterArrayWithFitness]()
    val fitness =
      if (optimizee.evaluateByComparison) Double.MaxValue
      else optimizee.evaluateFitness(params)
    val p1 = ParameterArrayWithFitness(params, fitness)
    population += p1
    considerChampion(p1)

    val pending = ArrayBuffer[ParameterArray]()
    var i = 0
    val max = 100 * desiredPopulationSize
    while (population.size + pending.size < desiredPopulationSize && i < max) {
      val nbr = params.getRandomNeighbor(searchConfig.initialNeighborRadius)
      if (!GeneticSearchStrategy.containsSolution(population, nbr) &&
          !pending.exists(p => GeneticSearchStrategy.sameSolution(p, nbr))) {
        pending += nbr
      }
      i += 1
    }
    val scored = evaluateOffspring(pending.toSeq, params)
    scored.foreach { m =>
      if (!GeneticSearchStrategy.containsSolution(population, m.pa)) {
        population += m
        considerChampion(m)
      }
    }
    if (population.size <= 1) throw new IllegalStateException("No random neighbors found for " + params)
    population
  }

  /** Runs one refill pass as in [[evolve]]; visible for tests. */
  private[optimization] def refillAfterCullForTesting(
      population: ArrayBuffer[ParameterArrayWithFitness],
      keepSize: Int,
      generationBaseline: ParameterArray,
      desiredPopSize: Int
  ): Unit = {
    val saved = desiredPopulationSize
    desiredPopulationSize = desiredPopSize
    try replaceCulledWithKeeperVariants(population, keepSize, generationBaseline)
    finally desiredPopulationSize = saved
  }

  /** Inject elitism for tests after a cull. */
  private[optimization] def ensureEliteForTesting(population: ArrayBuffer[ParameterArrayWithFitness]): Unit =
    ensureElite(population)

  private[optimization] def setGlobalChampionForTesting(c: ParameterArrayWithFitness): Unit = {
    globalChampion = Some(c)
    updateRunBest(c)
  }

  private def evolve(lastBest: ParameterArrayWithFitness,
                     population: ArrayBuffer[ParameterArrayWithFitness],
                     fitnessRange: Double): ParameterArrayWithFitness = {
    var currentBest = lastBest
    var ct = 0
    var deltaFitness = -1.0
    var numWithNoImprovement = 0

    val lowThresh = 0.005 * fitnessRange
    val highThresh = 0.05 * fitnessRange

    var recentBest = lastBest
    var pop = population
    var budgetHit = false
    while (!budgetHit && !shouldStopEvolution(deltaFitness, numWithNoImprovement, currentBest, ct)) {
      try {
        val generationBaseline = recentBest.pa
        reevaluatePopulationFitness(pop, generationBaseline)
        recentBest = syncBestAfterReevaluate(pop, generationBaseline)
        pop = cullPopulation(pop)
        replaceCulledWithKeeperVariants(pop, pop.size, generationBaseline)
        ensureElite(pop)
        currentBest = pop.min
        considerChampion(currentBest)
        // Prefer absolute champion over population min when elitism tracks a better individual
        currentBest = championOr(currentBest)
        trace("currBest = " + currentBest + " \nrecBest = " + recentBest + "        ct=" + ct)
        deltaFitness = computeFitnessDelta(recentBest, currentBest, ct)
        trace("delta fitness =" +
          deltaFitness + "      rbrRadius = " + nbrRadius + "  improvementEpsilon = " + improvementEpsilon)
        scaleNeighborRadius(deltaFitness, lowThresh, highThresh)
        recentBest = currentBest
        notifyOfChange(currentBest)
        ct += 1
        if (deltaFitness > -improvementEpsilon) {
          numWithNoImprovement += 1
        }
      } catch {
        case _: EvaluationBudgetExceededException =>
          budgetHit = true
      }
    }

    val result = championOr(currentBest)
    if (!budgetHit) logEvolveTermination(result, deltaFitness, numWithNoImprovement, ct)
    trace("----------------------- done -------------------")
    log(ct, result, 0, 0, FormatUtil.formatNumber(ct))
    result
  }

  private def considerChampion(candidate: ParameterArrayWithFitness): Unit = {
    updateRunBest(candidate)
    if (globalChampion.forall(_.fitness > candidate.fitness))
      globalChampion = Some(candidate)
  }

  private def championOr(fallback: ParameterArrayWithFitness): ParameterArrayWithFitness =
    globalChampion.filter(_.fitness <= fallback.fitness).getOrElse(fallback)

  private def championOrFallback(params: ParameterArray): ParameterArrayWithFitness =
    globalChampion.getOrElse {
      if (optimizee.evaluateByComparison) ParameterArrayWithFitness(params, Double.MaxValue)
      else ParameterArrayWithFitness(params, Double.MaxValue)
    }

  private def ensureElite(population: ArrayBuffer[ParameterArrayWithFitness]): Unit = {
    if (!searchConfig.elitismEnabled) return
    globalChampion.foreach { champ =>
      if (!GeneticSearchStrategy.containsSolution(population, champ.pa)) {
        if (population.size < desiredPopulationSize) population += champ
        else {
          val worstIdx = population.indices.maxBy(i => population(i).fitness)
          population(worstIdx) = champ
        }
      }
    }
  }

  /** After re-scoring vs `baselinePa`, refresh the tracked champion so its `fitness` matches the buffer. */
  private def syncBestAfterReevaluate(
      pop: ArrayBuffer[ParameterArrayWithFitness],
      baselinePa: ParameterArray
  ): ParameterArrayWithFitness =
    pop.find(_.pa eq baselinePa).getOrElse(pop.min)

  private def shouldStopEvolution(deltaFitness: Double, numWithNoImprovement: Int,
                                  currentBest: ParameterArrayWithFitness, ct: Int): Boolean =
    (deltaFitness >= -improvementEpsilon && numWithNoImprovement > searchConfig.maxStagnantGenerations) ||
      isOptimalFitnessReached(currentBest) ||
      ct > searchConfig.maxIterations

  private def scaleNeighborRadius(deltaFitness: Double, lowThresh: Double, highThresh: Double): Unit = {
    val factor =
      if (deltaFitness < -highThresh) searchConfig.nbrRadiusExpandFactor
      else if (deltaFitness > -lowThresh) searchConfig.nbrRadiusShrinkFactor
      else 1.0
    nbrRadius = math.min(1.5, nbrRadius * factor)
  }

  /** [[com.barrybecker4.optimization.parameter.types.AbstractParameter.tweakNumericValue]] requires |r| <= 1.5. */
  private def effectiveMutationRadius(keeperIndex: Int): Double = {
    val raw = (keeperIndex + searchConfig.nbrRadiusSoftener) / searchConfig.nbrRadiusSoftener * nbrRadius
    math.min(1.5, raw)
  }

  private def logEvolveTermination(currentBest: ParameterArrayWithFitness, deltaFitness: Double,
                                   numWithNoImprovement: Int, ct: Int): Unit = {
    if (isOptimalFitnessReached(currentBest))
      trace("stopped because we found the optimal fitness.")
    else if (deltaFitness >= -improvementEpsilon && numWithNoImprovement > searchConfig.maxStagnantGenerations) {
      trace("stopped because we made no IMPROVEMENT. The delta, " +
        deltaFitness + " was >= " + -improvementEpsilon)
    }
    else if (ct > searchConfig.maxIterations)
      trace(s"Stopped because we exceeded the MAX ITERATIONS(${searchConfig.maxIterations}): num iterations = $ct")
    else
      throw new IllegalStateException(s"stopped for unexpected cause. ct = $ct deltaFit=$deltaFitness " +
        s"currBest=$currentBest nbrRad=$nbrRadius")
  }

  /**
    * Fitness change of the tracked champion between generations.
    * Uses `math.min` so a transient population blip cannot report a worse champion than last time.
    */
  private def computeFitnessDelta(lastBest: ParameterArrayWithFitness,
                                  currentBest: ParameterArrayWithFitness, ct: Int): Double = {
    val effectiveCurrent =
      if (currentBest.fitness <= lastBest.fitness) currentBest
      else lastBest
    val deltaFitness = effectiveCurrent.fitness - lastBest.fitness
    log(ct, effectiveCurrent, nbrRadius, deltaFitness, "---")
    deltaFitness
  }

  /** Remove all but the best candidates. Better candidates have lower values. */
  private def cullPopulation(population: ArrayBuffer[ParameterArrayWithFitness]) = {
    val sortedPopulation = population.sorted
    val keepSize = Math.max(1, (sortedPopulation.size * (1.0 - searchConfig.cullFactor)).toInt)
    val size = sortedPopulation.size
    sortedPopulation.dropRight(size - keepSize)
  }

  /**
    * Replace culled slots until `desiredPopulationSize` is reached.
    * Crossover / single-sample mutation proposals are batched through [[evaluateOffspring]];
    * greedy absolute mutation evaluates inline.
    */
  private def replaceCulledWithKeeperVariants(population: ArrayBuffer[ParameterArrayWithFitness],
                                              keepSize: Int,
                                              generationBaseline: ParameterArray): Unit = {
    val maxAttempts =
      math.max(100 * desiredPopulationSize, (desiredPopulationSize - keepSize) * 50 + 50)
    var attempts = 0
    val pending = ArrayBuffer[ParameterArray]()

    def flushPending(): Unit = {
      if (pending.isEmpty) return
      val scored = evaluateOffspring(pending.toSeq, generationBaseline)
      pending.clear()
      scored.foreach { m =>
        if (population.size < desiredPopulationSize &&
            !GeneticSearchStrategy.containsSolution(population, m.pa)) {
          population += m
          considerChampion(m)
          notifyOfChange(m)
        }
      }
    }

    while (population.size + pending.size < desiredPopulationSize && attempts < maxAttempts) {
      attempts += 1
      val tryCross =
        searchConfig.crossoverEnabled && rnd.nextDouble() < searchConfig.crossoverProbability && keepSize >= 2
      var addedPending = false
      if (tryCross) {
        var i1 = (rnd.nextDouble() * rnd.nextDouble() * keepSize).toInt
        var i2 = (rnd.nextDouble() * rnd.nextDouble() * keepSize).toInt
        if (keepSize > 1) {
          while (i2 == i1) i2 = (i2 + 1) % keepSize
        }
        val p1 = population(i1).pa
        val p2 = population(i2).pa
        if (p1.getClass == p2.getClass) {
          try {
            val child = ParameterCrossover.cross(p1, p2, rnd)
            if (!GeneticSearchStrategy.containsSolution(population, child) &&
                !pending.exists(p => GeneticSearchStrategy.sameSolution(p, child))) {
              pending += child
              addedPending = true
            }
          } catch {
            case _: IllegalArgumentException => ()
          }
        }
      }
      if (!addedPending) {
        val r = rnd.nextDouble()
        val keeperIndex: Int = (r * r * keepSize).toInt
        val p = population(keeperIndex)
        val r2 = effectiveMutationRadius(keeperIndex)
        val useGreedy =
          !optimizee.evaluateByComparison &&
            searchConfig.neighborSelection == NeighborSelection.GreedyImproveAbsolute
        if (useGreedy) {
          flushPending()
          val nbr = getNeighborGreedy(p.pa, r2)
          if (!GeneticSearchStrategy.containsSolution(population, nbr.pa)) {
            population += nbr
            considerChampion(nbr)
            notifyOfChange(p)
          }
        } else {
          val nbrPa = p.pa.getRandomNeighbor(r2)
          if (!GeneticSearchStrategy.containsSolution(population, nbrPa) &&
              !pending.exists(x => GeneticSearchStrategy.sameSolution(x, nbrPa))) {
            pending += nbrPa
          }
        }
      }
    }
    flushPending()
  }

  /**
    * Evaluate a batch of candidate parameter arrays (sequential by default).
    * [[ConcurrentGeneticSearchStrategy]] overrides for parallel fitness evaluation.
    * Parent selection / crossover stay on the calling thread.
    */
  protected def evaluateOffspring(
      candidates: Seq[ParameterArray],
      generationBaseline: ParameterArray
  ): Seq[ParameterArrayWithFitness] =
    candidates.map(pa => evaluateParameterArray(pa, generationBaseline))

  protected def evaluateParameterArray(pa: ParameterArray, generationBaseline: ParameterArray): ParameterArrayWithFitness = {
    val fitness =
      if (optimizee.evaluateByComparison) optimizee.compareFitness(pa, generationBaseline)
      else optimizee.evaluateFitness(pa)
    ParameterArrayWithFitness(pa, fitness)
  }

  /** Absolute-fitness greedy neighbor search (may spend up to maxNeighborRetries+1 evals). */
  private def getNeighborGreedy(p: ParameterArray, rad: Double): ParameterArrayWithFitness = {
    var nbr = p.getRandomNeighbor(rad)
    val curFitness = optimizee.evaluateFitness(p)
    var nbrFitness = optimizee.evaluateFitness(nbr)
    var ct = 0
    while (nbrFitness >= curFitness && ct < searchConfig.maxNeighborRetries) {
      nbr = p.getRandomNeighbor(rad)
      nbrFitness = optimizee.evaluateFitness(nbr)
      ct += 1
    }
    ParameterArrayWithFitness(nbr, nbrFitness)
  }

  /** Refresh per-individual fitness when using relative fitness vs the previous generation best. */
  protected def reevaluatePopulationFitness(population: ArrayBuffer[ParameterArrayWithFitness],
                                            baseline: ParameterArray): Unit = {
    if (!optimizee.evaluateByComparison) return
    var i = 0
    while (i < population.size) {
      val p = population(i).pa
      val f = optimizee.compareFitness(p, baseline)
      population(i) = ParameterArrayWithFitness(p, f)
      i += 1
    }
  }
}
