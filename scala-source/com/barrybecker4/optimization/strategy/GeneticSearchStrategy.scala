// Copyright by Barry G. Becker, 2000-2018. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.strategy

import com.barrybecker4.optimization.optimizee.Optimizee
import GeneticSearchStrategy._
import com.barrybecker4.common.concurrency.ThreadUtil
import com.barrybecker4.common.format.FormatUtil
import com.barrybecker4.math.MathUtil
import com.barrybecker4.optimization.parameter.{ParameterArray, ParameterArrayWithFitness}
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

  /** this prevents us from running forever.  */
  private val MAX_ITERATIONS = 100
  /** If more than this many iterations with no improvement, then stop */
  private val MAX_ITER_NO_IMPRV = 3

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
class GeneticSearchStrategy(optimizee: Optimizee, rnd: Random = MathUtil.RANDOM)
  extends OptimizationStrategy(optimizee) {

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
    evolve(params, lastBest, population, fitnessRange)
  }

  private def findInitialPopulation(params: ParameterArray): ArrayBuffer[ParameterArrayWithFitness] = {
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
      val nbr = params.getRandomNeighbor(INITIAL_RADIUS)
      if (!population.contains(nbr)) {
        val fitness =
          if (optimizee.evaluateByComparison) optimizee.compareFitness(nbr, params)
          else optimizee.evaluateFitness(params)
        population += ParameterArrayWithFitness(nbr, fitness)
      }
      i += 1
    }
    if (population.size <= 1) throw new IllegalStateException("No random neighbors found for " + params)
    population
  }

  /** Find the new best candidate.
    * @return the new best candidate.
    */
  private def evolve(params: ParameterArray, lastBest: ParameterArrayWithFitness,
                          population: ArrayBuffer[ParameterArrayWithFitness],
                          fitnessRange: Double): ParameterArrayWithFitness = {
    var currentBest = lastBest
    var ct = 0
    var deltaFitness = .0
    var numWithNoImprovement = 0

    var lowThresh = 0.005* fitnessRange  // shrink if less than this
    var highThresh = 0.05 * fitnessRange // grow if more than this

    var recentBest = lastBest
    //println("findNewBest: recent best =" + recentBest);
    // each iteration represents a new generation of the population.
    var pop = population
    do {
      pop = cullPopulation(pop)
      replaceCulledWithKeeperVariants(pop, pop.size)
      currentBest = pop.min //evaluatePopulation(pop, recentBest.pa)
      println("currBest = " + currentBest + " \nrecBest = " + recentBest + "        ct=" + ct)
      deltaFitness = computeFitnessDelta(params, recentBest, currentBest, ct)
      println("delta fitness =" +
        deltaFitness + "      rbrRadius = " + nbrRadius + "  improvementEpsilon = " + improvementEpsilon)
      val factor =
        if (deltaFitness < -highThresh) NBR_RADIUS_EXPAND_FACTOR
        else if (deltaFitness > -lowThresh) NBR_RADIUS_SHRINK_FACTOR
        else 1.0
      nbrRadius *= factor
      recentBest = currentBest
      notifyOfChange(currentBest)
      ct += 1
      assert(deltaFitness <= 0, "The fitness should never get worse.")
      if (deltaFitness > -improvementEpsilon) {
        numWithNoImprovement += 1
      }
    } while (!(deltaFitness >= -improvementEpsilon && numWithNoImprovement > MAX_ITER_NO_IMPRV)
        && !isOptimalFitnessReached(currentBest) && (ct <= MAX_ITERATIONS))

    if (isOptimalFitnessReached(currentBest))
      println("stopped because we found the optimal fitness.")
    else if (deltaFitness >= -improvementEpsilon && numWithNoImprovement > MAX_ITER_NO_IMPRV) {
      println("stopped because we made no IMPROVEMENT. The delta, " +
        deltaFitness + " was >= " + -improvementEpsilon)
    }
    else if (ct > MAX_ITERATIONS)
      println(s"Stopped because we exceeded the MAX ITERATIONS($MAX_ITERATIONS): num iterations = $ct")
    else
      throw new IllegalStateException(s"stopped for unexpected cause. ct = $ct deltaFit=$deltaFitness " +
        s"currBest=$currentBest nbrRad=$nbrRadius")

    println("----------------------- done -------------------")
    log(ct, currentBest, 0, 0, FormatUtil.formatNumber(ct))
    currentBest
  }

  /** Computes the fitness delta, but also logs and asserts that it is not 0.
    * @return the different in fitness between current best and last best.
    */
  private def computeFitnessDelta(params: ParameterArray,
                                  lastBest: ParameterArrayWithFitness,
                                  currentBest: ParameterArrayWithFitness, ct: Int) = {
    var deltaFitness = .0
    deltaFitness = currentBest.fitness - lastBest.fitness
    assert(deltaFitness <= 0, "We must never get worse in a new generation. Old fitness=" +
      lastBest.fitness + " New Fitness = " + currentBest.fitness + ".")
    //println(" ct="+ct+"  nbrRadius = " + nbrRadius + "  population size =" + desiredPopulationSize
    //                   +"  deltaFitness = " + deltaFitness+"  currentBest = " + currentBest.getFitness()
    //                   +"  lastBest = " + lastBest.getFitness());
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
  private def replaceCulledWithKeeperVariants(population: ArrayBuffer[ParameterArrayWithFitness],
                                              keepSize: Int): Unit = {
    var k = keepSize
    //println("keepSize = " + keepSize + " grow current popSize of "
    //    + population.size() + " to " + desiredPopulationSize);
    while (k < desiredPopulationSize) {
      // loop over the keepers until all replacements found.
      // Select randomly, but skewed toward the better ones
      val r = rnd.nextDouble
      val keeperIndex: Int = (r * r * keepSize).toInt
      //k % keepSize;
      val p = population(keeperIndex)
      // Add a permutation of one of the keepers.
      // Multiply the radius by m because we want the worse ones to have higher variability.
      val r2 = (keeperIndex + NBR_RADIUS_SOFTENER) / NBR_RADIUS_SOFTENER * nbrRadius
      val nbr = getNeighbor(p.pa, r2)
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
  private def getNeighbor(p: ParameterArray, rad: Double): ParameterArrayWithFitness = {
    var nbr = p.getRandomNeighbor(rad)
    var nbrFitness = Double.MaxValue
    if (!optimizee.evaluateByComparison) { // try to find a nbr with fitness that is better
      val curFitness = optimizee.evaluateFitness(p)
      var ct = 0
      nbrFitness = optimizee.evaluateFitness(nbr)
      while (nbrFitness >= curFitness && ct < MAX_NBRS_TO_EXPLORE) {
        nbr = p.getRandomNeighbor(rad)
        nbrFitness = optimizee.evaluateFitness(nbr)
        ct += 1
      }
    }
    ParameterArrayWithFitness(nbr, nbrFitness)
  }

  /** Evaluate the members of the population - either directly, or by
    * comparing them against the initial params value passed in (including params).
    * Note: this method assigns a fitness value to each member of the population.
    * @param population   the population to evaluate
    * @param previousBest the best solution from the previous iteration
    * @return the new best solution.
    */
  protected def evaluatePopulation(population: ArrayBuffer[ParameterArray],
                                   previousBest: ParameterArray): ParameterArrayWithFitness = {
    var bestFitness = ParameterArrayWithFitness(previousBest, Double.MaxValue)

    for (p <- population) {
      val fitness =
        if (optimizee.evaluateByComparison) optimizee.compareFitness(p, previousBest)
        else optimizee.evaluateFitness(p)
      if (fitness < bestFitness.fitness) {
        bestFitness = ParameterArrayWithFitness(p, fitness)
        // show it if better than what we had before
        notifyOfChange(bestFitness)
        //ThreadUtil.sleep(500)
      }
    }
    bestFitness
  }

  private def printPopulation(population: List[_]): Unit = printPopulation(population, population.size)

  private def printPopulation(population: List[_], limit: Int): Unit = {
    var i = 0
    while (i < population.size && i < limit) {
      println("$i: ${population(i)}")
      i += 1
    }
    println("")
  }
}
