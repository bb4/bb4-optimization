// Copyright by Barry G. Becker, 2000-2026. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.strategy

import com.barrybecker4.optimization.optimizee.Optimizee
import com.barrybecker4.common.format.FormatUtil
import com.barrybecker4.math.MathUtil
import com.barrybecker4.optimization.parameter.{ParameterArray, ParameterArrayWithFitness, ParameterCrossover}
import ParameterArrayWithFitness.given
import scala.collection.mutable.ArrayBuffer
import scala.util.Random

object GeneticSearchStrategy {
  // Percent amount to decimate the parent population by on each iteration
  private val CULL_FACTOR = 0.7
  private val NBR_RADIUS = 0.1
  private val NBR_RADIUS_SHRINK_FACTOR = 0.8
  private val NBR_RADIUS_EXPAND_FACTOR = 1.1
  private val NBR_RADIUS_SOFTENER = 20.0
  private val INITIAL_RADIUS = 1.0
  private val MAX_NBRS_TO_EXPLORE = 8

  /** Probability of using crossover vs neighbor mutation when filling the population after culling. */
  private val CROSSOVER_PROBABILITY = 0.65

  /** this prevents us from running forever.  */
  private val MAX_ITERATIONS = 100
  /** If more than this many iterations with no improvement, then stop */
  private val MAX_ITER_NO_IMPRV = 3

  /** stop when the avg population score does not improve by better than this  */
  private[strategy] val DEFAULT_IMPROVEMENT_EPS = 0.000000000001
}

/**
  * Genetic Algorithm (evolutionary) optimization strategy.
  * Many different strategies are possible to alter the population for each successive iteration.
  * The 2 primary ones that I use here are unary mutation and cross-over.
  * See Chapter 6 in "How to Solve it: Modern Heuristics" for more info.
  *
  * Use a hardcoded static data interface to initialize so it can be easily run in an applet without using resources.
  * @param optimizee the thing to be optimized.
  * @param config    per-instance genetic options (defaults preserve prior behavior).
  * @author Barry Becker
  */
class GeneticSearchStrategy(
    optimizee: Optimizee,
    rnd: Random = MathUtil.RANDOM,
    val config: GeneticConfig = GeneticConfig()
) extends OptimizationStrategy(optimizee) {

  /** radius to look for neighbors in  */
  private var nbrRadius = GeneticSearchStrategy.NBR_RADIUS

  /** This is the desired number of members to be maintained in the population at any time.
    * Might not always get this many if there are duplicates or the search space is small.
    */
  private var desiredPopulationSize = 0

  /** If we don't improve by at least this amount between iterations, terminate.  */
  protected var improvementEpsilon: Double = GeneticSearchStrategy.DEFAULT_IMPROVEMENT_EPS

  def setImprovementEpsilon(eps: Double): Unit = {
    improvementEpsilon = eps
  }

  /** Finds a local minima using a genetic algorithm (evolutionary) search.
    * Stop iterating as soon as the average evaluation score of the population
    * does not significantly improve.
    * @param params the initial value for the parameters to optimize.
    * @param fitnessRange the approximate absolute value of the fitnessRange.
    * @return the optimized params.
    */
  override def doOptimization(params: ParameterArray, fitnessRange: Double): ParameterArrayWithFitness = {
    var lastBest: ParameterArrayWithFitness = null
    desiredPopulationSize = params.getSamplePopulationSize

    val population = findInitialPopulation(params)
    //println("The population of this generation is " + population.size()
    //    + " (desired was " + desiredPopulationSize + ")");
    // EVALUATE POPULATION
    lastBest = population.min //evaluatePopulation(population, params)
    evolve(lastBest, population, fitnessRange)
  }

  /** Visible for tests in `com.barrybecker4.optimization`. */
  private[optimization] def findInitialPopulation(params: ParameterArray): ArrayBuffer[ParameterArrayWithFitness] = {
    if (desiredPopulationSize <= 0) desiredPopulationSize = params.getSamplePopulationSize
    // create an initial population based on params and POPULATION_SIZE-1 other random candidate solutions.
    val population = ArrayBuffer[ParameterArrayWithFitness]()
    val fitness =
      if (optimizee.evaluateByComparison) Double.MaxValue
      else optimizee.evaluateFitness(params)
    val p1 = ParameterArrayWithFitness(params, fitness)
    population += p1
    var i = 0
    val max = 100 * desiredPopulationSize
    while (population.size < desiredPopulationSize && i < max) {
      val nbr = params.getRandomNeighbor(GeneticSearchStrategy.INITIAL_RADIUS)
      if (!population.contains(nbr)) {
        val fitness =
          if (optimizee.evaluateByComparison) optimizee.compareFitness(nbr, params)
          else optimizee.evaluateFitness(nbr)
        population += ParameterArrayWithFitness(nbr, fitness)
      }
      i += 1
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

  /** Find the new best candidate.
    * @return the new best candidate.
    */
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
    while (!shouldStopEvolution(deltaFitness, numWithNoImprovement, currentBest, ct)) {
      val generationBaseline = recentBest.pa
      reevaluatePopulationFitness(pop, generationBaseline)
      recentBest = syncBestAfterReevaluate(pop, generationBaseline)
      pop = cullPopulation(pop)
      replaceCulledWithKeeperVariants(pop, pop.size, generationBaseline)
      currentBest = pop.min
      trace("currBest = " + currentBest + " \nrecBest = " + recentBest + "        ct=" + ct)
      deltaFitness = computeFitnessDelta(recentBest, currentBest, ct)
      trace("delta fitness =" +
        deltaFitness + "      rbrRadius = " + nbrRadius + "  improvementEpsilon = " + improvementEpsilon)
      scaleNeighborRadius(deltaFitness, lowThresh, highThresh)
      recentBest = currentBest
      notifyOfChange(currentBest)
      ct += 1
      assert(deltaFitness <= 0, "The fitness should never get worse.")
      if (deltaFitness > -improvementEpsilon) {
        numWithNoImprovement += 1
      }
    }

    logEvolveTermination(currentBest, deltaFitness, numWithNoImprovement, ct)
    trace("----------------------- done -------------------")
    log(ct, currentBest, 0, 0, FormatUtil.formatNumber(ct))
    currentBest
  }

  /** After re-scoring vs `baselinePa`, refresh the tracked champion so its `fitness` matches the buffer. */
  private def syncBestAfterReevaluate(
      pop: ArrayBuffer[ParameterArrayWithFitness],
      baselinePa: ParameterArray
  ): ParameterArrayWithFitness =
    pop.find(_.pa eq baselinePa).getOrElse(pop.min)

  private def shouldStopEvolution(deltaFitness: Double, numWithNoImprovement: Int,
                                  currentBest: ParameterArrayWithFitness, ct: Int): Boolean =
    (deltaFitness >= -improvementEpsilon && numWithNoImprovement > GeneticSearchStrategy.MAX_ITER_NO_IMPRV) ||
      isOptimalFitnessReached(currentBest) ||
      ct > GeneticSearchStrategy.MAX_ITERATIONS

  private def scaleNeighborRadius(deltaFitness: Double, lowThresh: Double, highThresh: Double): Unit = {
    val factor =
      if (deltaFitness < -highThresh) GeneticSearchStrategy.NBR_RADIUS_EXPAND_FACTOR
      else if (deltaFitness > -lowThresh) GeneticSearchStrategy.NBR_RADIUS_SHRINK_FACTOR
      else 1.0
    nbrRadius *= factor
  }

  private def logEvolveTermination(currentBest: ParameterArrayWithFitness, deltaFitness: Double,
                                   numWithNoImprovement: Int, ct: Int): Unit = {
    if (isOptimalFitnessReached(currentBest))
      trace("stopped because we found the optimal fitness.")
    else if (deltaFitness >= -improvementEpsilon && numWithNoImprovement > GeneticSearchStrategy.MAX_ITER_NO_IMPRV) {
      trace("stopped because we made no IMPROVEMENT. The delta, " +
        deltaFitness + " was >= " + -improvementEpsilon)
    }
    else if (ct > GeneticSearchStrategy.MAX_ITERATIONS)
      trace(s"Stopped because we exceeded the MAX ITERATIONS(${GeneticSearchStrategy.MAX_ITERATIONS}): num iterations = $ct")
    else
      throw new IllegalStateException(s"stopped for unexpected cause. ct = $ct deltaFit=$deltaFitness " +
        s"currBest=$currentBest nbrRad=$nbrRadius")
  }

  /** Computes the fitness delta, but also logs and asserts that it is not 0.
    * @return the different in fitness between current best and last best.
    */
  private def computeFitnessDelta(lastBest: ParameterArrayWithFitness,
                                  currentBest: ParameterArrayWithFitness, ct: Int): Double = {
    val deltaFitness = currentBest.fitness - lastBest.fitness
    assert(deltaFitness <= 0, "We must never get worse in a new generation. Old fitness=" +
      lastBest.fitness + " New Fitness = " + currentBest.fitness + ".")
    log(ct, currentBest, nbrRadius, deltaFitness, "---")
    deltaFitness
  }

  /** Remove all but the best candidates. Better candidates have lower values.
    * @param population the whole population. It will be reduced in size.
    * @return the culled population
    */
  private def cullPopulation(population: ArrayBuffer[ParameterArrayWithFitness]) = {
    // sort the population according to the fitness of members.
    val sortedPopulation = population.sorted
    // throw out the bottom CULL_FACTOR*desiredPopulationSize members - keeping the cream of the crop.
    // then replace those culled with crossover and/or neighbor mutation of the parents that remain.
    val keepSize = Math.max(1, (sortedPopulation.size * (1.0 - GeneticSearchStrategy.CULL_FACTOR)).toInt)
    val size = sortedPopulation.size
    val culledPop = sortedPopulation.dropRight(size - keepSize)
    //println("new pop size = " + culledPop.size)
    //println("pop after culling: first = "  +  culledPop.get(0) + " last(" + culledPop.size() + ")");
    //printPopulation(sortedPopulation, 5);
    culledPop
  }

  /** Replace culled slots until `desiredPopulationSize` is reached (at most one new individual per attempt).
    *
    * @param generationBaseline compare-mode fitness for all new evaluations this generation uses this baseline
    */
  private def replaceCulledWithKeeperVariants(population: ArrayBuffer[ParameterArrayWithFitness],
                                              keepSize: Int,
                                              generationBaseline: ParameterArray): Unit = {
    val maxAttempts =
      math.max(100 * desiredPopulationSize, (desiredPopulationSize - keepSize) * 50 + 50)
    var attempts = 0
    while (population.size < desiredPopulationSize && attempts < maxAttempts) {
      attempts += 1
      val tryCross =
        config.crossoverEnabled && rnd.nextDouble() < GeneticSearchStrategy.CROSSOVER_PROBABILITY && keepSize >= 2
      var added = false
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
            if (!population.exists(_.pa == child)) {
              population += evaluateParameterArray(child, generationBaseline)
              notifyOfChange(population(i1))
              added = true
            }
          } catch {
            case _: IllegalArgumentException => ()
          }
        }
      }
      if (!added) {
        val r = rnd.nextDouble()
        val keeperIndex: Int = (r * r * keepSize).toInt
        val p = population(keeperIndex)
        val r2 = (keeperIndex + GeneticSearchStrategy.NBR_RADIUS_SOFTENER) / GeneticSearchStrategy.NBR_RADIUS_SOFTENER * nbrRadius
        val nbr = getNeighbor(p.pa, r2, generationBaseline)
        if (!population.exists(_.pa == nbr.pa)) {
          population += nbr
          notifyOfChange(p)
        }
      }
    }
  }

  private def evaluateParameterArray(pa: ParameterArray, generationBaseline: ParameterArray): ParameterArrayWithFitness = {
    val fitness =
      if (optimizee.evaluateByComparison) optimizee.compareFitness(pa, generationBaseline)
      else optimizee.evaluateFitness(pa)
    ParameterArrayWithFitness(pa, fitness)
  }

  /** @param p parameter array to get neighbor for
    * @param rad larger radius means more distant neighbor
    * @param generationBaseline compare-mode baseline for this generation (same as reevaluate/crossover)
    * @return a neighbor of p. If using absolute fitness, try to find a neighbor that has better fitnesss.
    */
  private def getNeighbor(p: ParameterArray, rad: Double, generationBaseline: ParameterArray): ParameterArrayWithFitness = {
    var nbr = p.getRandomNeighbor(rad)
    var nbrFitness = Double.MaxValue
    if (!optimizee.evaluateByComparison) { // try to find a nbr with fitness that is better
      val curFitness = optimizee.evaluateFitness(p)
      var ct = 0
      nbrFitness = optimizee.evaluateFitness(nbr)
      while (nbrFitness >= curFitness && ct < GeneticSearchStrategy.MAX_NBRS_TO_EXPLORE) {
        nbr = p.getRandomNeighbor(rad)
        nbrFitness = optimizee.evaluateFitness(nbr)
        ct += 1
      }
    } else {
      nbrFitness = optimizee.compareFitness(nbr, generationBaseline)
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

/**
  * Per-instance options for [[GeneticSearchStrategy]] (thread-safe).
  *
  * @param crossoverEnabled when false, refill uses neighbor mutation only
  */
case class GeneticConfig(crossoverEnabled: Boolean = true)
