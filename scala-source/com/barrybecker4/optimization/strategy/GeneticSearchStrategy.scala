// Copyright by Barry G. Becker, 2000-2018. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.strategy

import com.barrybecker4.optimization.optimizee.Optimizee
import GeneticSearchStrategy._
import com.barrybecker4.common.concurrency.ThreadUtil
import com.barrybecker4.common.format.FormatUtil
import com.barrybecker4.common.math.MathUtil
import com.barrybecker4.optimization.parameter.ParameterArray

import scala.collection.mutable.ArrayBuffer

object GeneticSearchStrategy {
  // Percent amount to decimate the parent population by on each iteration
  private val CULL_FACTOR = 0.8
  private val NBR_RADIUS = 0.08
  private val NBR_RADIUS_SHRINK_FACTOR = 0.8
  private val NBR_RADIUS_EXPAND_FACTOR = 1.1
  private val NBR_RADIUS_SOFTENER = 10.0
  private val INITIAL_RADIUS = 1.0
  private val MAX_NBRS_TO_EXPLORE = 8

  /** this prevents us from running forever.  */
  private val MAX_ITERATIONS = 100

  /** stop when the avg population score does not improve by better than this  */
  private val DEFAULT_IMPROVEMENT_EPS = 0.000000000001
}

/**
  * Genetic Algorithm (evolutionary) optimization strategy.
  * Many different strategies are possible to alter the population for each successive iteration.
  * The 2 primary ones that I use here are unary mutation and cross-over.
  * See Chapter 6 in "How to Solve it: Modern Heuristics" for more info.
  *
  * Use a hardcoded static data interface to initialize so it can be easily run in an applet without using resources.
  * @param optimizee the thing to be optimized.
  * @author Barry Becker
  */
class GeneticSearchStrategy(optimizee: Optimizee) extends OptimizationStrategy(optimizee) {

  /** radius to look for neighbors in  */
  private var nbrRadius = NBR_RADIUS

  /** This is the desired number of members to be maintained in the population at any time.
    * Might not always get this many if there are duplicates or the search space is small.
    */
  private var desiredPopulationSize = 0

  /** If we don't improve by at least this amount between iterations, terminate.  */
  protected var improvementEpsilon: Double = DEFAULT_IMPROVEMENT_EPS

  def setImprovementEpsilon(eps: Double): Unit = {
    improvementEpsilon = eps
  }

  /** Finds a local minima using a genetic algorithm (evolutionary) search.
    * Stop iterating as soon as the average evaluation score of the population
    * does not significantly improve.
    * @param params   the initial value for the parameters to optimize.
    * @param fitnessRange the approximate absolute value of the fitnessRange.
    * @return the optimized params.
    */
  override def doOptimization(params: ParameterArray, fitnessRange: Double): ParameterArray = {
    var lastBest: ParameterArray = null
    desiredPopulationSize = params.getSamplePopulationSize

    val population = findInitialPopulation(params)
    //println("The population of this generation is " + population.size()
    //    + " (desired was " + desiredPopulationSize + ")");
    // EVALUATE POPULATION
    lastBest = evaluatePopulation(population, params)
    findNewBest(params, lastBest, population)
  }

  private def findInitialPopulation(params: ParameterArray) = {
    // create an initial population based on params and POPULATION_SIZE-1 other random candidate solutions.
    val population = ArrayBuffer[ParameterArray]()
    population += params
    var i = 0
    val max = 100 * desiredPopulationSize
    while (population.size < desiredPopulationSize && i < max) {
      val nbr = params.getRandomNeighbor(INITIAL_RADIUS)
      if (!population.contains(nbr))
        population += nbr
      i += 1
    }
    if (population.size <= 1) throw new IllegalStateException("No random neighbors found for " + params)
    population
  }

  /** Find the new best candidate.
    * @return the new best candidate.
    */
  private def findNewBest(params: ParameterArray, lastBest: ParameterArray, population: ArrayBuffer[ParameterArray]) = {
    var currentBest = lastBest
    var ct = 0
    var deltaFitness = .0
    var recentBest = lastBest
    //println("findNewBest: recent best =" + recentBest);
    // each iteration represents a new generation of the population.
    var pop = population
    do {
      pop = cullPopulation(pop)
      replaceCulledWithKeeperVariants(pop, pop.size)
      currentBest = evaluatePopulation(pop, recentBest)
      println("currBest = " + currentBest + " \nrecBest = " + recentBest + "        ct=" + ct)
      deltaFitness = computeFitnessDelta(params, recentBest, currentBest, ct)
      println("delta fitness =" +
        deltaFitness + "      rbrRadius = " + nbrRadius + "  improvementEpsilon = " + improvementEpsilon)
      val factor = if (deltaFitness < -3.0) NBR_RADIUS_EXPAND_FACTOR
      else NBR_RADIUS_SHRINK_FACTOR
      nbrRadius *= factor
      recentBest = currentBest.copy
      notifyOfChange(currentBest)
      ct += 1
    } while ((deltaFitness < -improvementEpsilon) && !isOptimalFitnessReached(currentBest) && (ct < MAX_ITERATIONS))

    if (isOptimalFitnessReached(currentBest))
      println("stopped because we found the optimal fitness.")
    else if (deltaFitness >= -improvementEpsilon)
      println("stopped because we made no IMPROVEMENT. The delta, " +
        deltaFitness + " was >= " + -improvementEpsilon)
    else println("Stopped because we exceeded the MAX ITERATIONS: " + ct)

    println("----------------------- done -------------------")
    log(ct, currentBest.getFitness, 0, 0, currentBest, FormatUtil.formatNumber(ct))
    currentBest
  }

  /** Computes the fitness delta, but also logs and asserts that it is not 0.
    * @return the different in fitness between current best and last best.
    */
  private def computeFitnessDelta(params: ParameterArray, lastBest: ParameterArray, currentBest: ParameterArray, ct: Int) = {
    var deltaFitness = .0
    deltaFitness = currentBest.getFitness - lastBest.getFitness
    assert(deltaFitness <= 0, "We must never get worse in a new generation. Old fitness=" +
      lastBest.getFitness + " New Fitness = " + currentBest.getFitness + ".")
    //println(" ct="+ct+"  nbrRadius = " + nbrRadius + "  population size =" + desiredPopulationSize
    //                   +"  deltaFitness = " + deltaFitness+"  currentBest = " + currentBest.getFitness()
    //                   +"  lastBest = " + lastBest.getFitness());
    log(ct, currentBest.getFitness, nbrRadius, deltaFitness, params, "---")
    deltaFitness
  }

  /** Remove all but the best candidates. Better candidates have lower values.
    * @param population the whole population. It will be reduced in size.
    * @return the culled population
    */
  private def cullPopulation(population: ArrayBuffer[ParameterArray]) = {
    // sort the population according to the fitness of members.
    val sortedPopulation = population.sorted
    // throw out the bottom CULL_FACTOR*desiredPopulationSize members - keeping the cream of the crop.
    // then replace those culled with unary variations of those (now parents) that remain.
    // @@ add option to do cross-over variations too.
    val keepSize = Math.max(1, (sortedPopulation.size * (1.0 - CULL_FACTOR)).toInt)
    val size = sortedPopulation.size
    val culledPop = sortedPopulation.dropRight(size - keepSize)
    //println("new pop size = " + culledPop.size)
    //println("pop after culling: first = "  +  culledPop.get(0) + " last(" + culledPop.size() + ")");
    //printPopulation(sortedPopulation, 5);
    culledPop
  }

  /** Replace the members of the population that were removed with variations of the ones that we kept.
    * @param population population
    * @param keepSize the number that were kept
    */
  private def replaceCulledWithKeeperVariants(population: ArrayBuffer[ParameterArray], keepSize: Int): Unit = {
    var k = keepSize
    //println("keepSize = " + keepSize + " grow current popSize of "
    //    + population.size() + " to " + desiredPopulationSize);
    while (k < desiredPopulationSize) { // loop over the keepers until all replacements found. Select randomly, but skewed toward the better ones
      val rnd = MathUtil.RANDOM.nextDouble
      val keeperIndex = (rnd * rnd * keepSize).toInt
      //k % keepSize;
      val p = population(keeperIndex)
      // Add a permutation of one of the keepers.
      // Multiply the radius by m because we want the worse ones to have higher variability.
      val r = (keeperIndex + NBR_RADIUS_SOFTENER) / NBR_RADIUS_SOFTENER * nbrRadius
      val nbr = getNeighbor(p, r) //p.getRandomNeighbor(r);
      if (!population.contains(nbr)) {
        population += nbr
        notifyOfChange(p)
      }
      k += 1
    }
    //printPopulation(population, 20);
  }

  /** @param p parameter array to get neighbor for
    * @param rad larger radius means more distant neighbor
    * @return a neighbor of p. If using absolute fitness, try to find a neighbor that has better fitnesss.
    */
  private def getNeighbor(p: ParameterArray, rad: Double) = {
    var nbr = p.getRandomNeighbor(rad)
    if (!optimizee.evaluateByComparison) { // try to find a nbr with fitness that is better
      val curFitness = optimizee.evaluateFitness(p)
      var ct = 0
      while (optimizee.evaluateFitness(nbr) > curFitness && ct < MAX_NBRS_TO_EXPLORE) {
        nbr = p.getRandomNeighbor(rad)
        ct += 1
      }
    }
    nbr
  }

  /** Evaluate the members of the population - either directly, or by
    * comparing them against the initial params value passed in (including params).
    * Note: this method assigns a fitness value to each member of the population.
    * @param population   the population to evaluate
    * @param previousBest the best solution from the previous iteration
    * @return the new best solution.
    */
  protected def evaluatePopulation(population: ArrayBuffer[ParameterArray],
                                   previousBest: ParameterArray): ParameterArray = {
    var bestFitness = previousBest

    for (p <- population) {
      var fitness = .0
      if (optimizee.evaluateByComparison) fitness = optimizee.compareFitness(p, previousBest)
      else fitness = optimizee.evaluateFitness(p)
      p.setFitness(fitness)
      if (fitness < bestFitness.getFitness) {
        bestFitness = p
        // show it if better than what we had before
        notifyOfChange(p)
        ThreadUtil.sleep(500)
      }
    }
    bestFitness.copy
  }

  private def printPopulation(population: List[_]): Unit = printPopulation(population, population.size)

  private def printPopulation(population: List[_], limit: Int): Unit = {
    var i = 0
    while (i < population.size && i < limit) {
      println(i + ": " + population(i))
      i += 1
    }
    println("")
  }
}