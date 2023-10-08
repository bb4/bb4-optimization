// Copyright by Barry G. Becker, 2000-2018. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.strategy

import com.barrybecker4.common.format.FormatUtil
import com.barrybecker4.math.MathUtil
import com.barrybecker4.optimization.optimizee.Optimizee
import com.barrybecker4.optimization.parameter.{ParameterArray, ParameterArrayWithFitness}
import SimulatedAnnealingStrategy._
import scala.util.Random


/**
  * Simulated annealing optimization strategy.
  * See http://en.wikipedia.org/wiki/Annealing for an explanation of the name.
  *
  * @author Barry Becker
  */
object SimulatedAnnealingStrategy {
  /** The number of iterations in the inner loop divided by the number of dimensions in the search space  */
  private val N = 10
  private val NUM_TEMP_ITERATIONS = 20

  /** the amount to drop the temperature on each temperature iteration.   */
  private val TEMP_DROP_FACTOR = 0.6

  /** the client should really set the tempMax using setTemperatureMax before running. */
  private val DEFAULT_TEMP_MAX = 1000.0
}

/**
  * Constructor.
  * use a hardcoded static data interface to initialize.
  * so it can be easily run in an applet without using resources.
  * @param optimizee the thing to be optimized.
  */
class SimulatedAnnealingStrategy[P <: ParameterArray](optimizee: Optimizee[P], rnd: Random = MathUtil.RANDOM)
  extends OptimizationStrategy(optimizee) {

  private var tempMax = SimulatedAnnealingStrategy.DEFAULT_TEMP_MAX

  /** keep track of points that were searched */
  private val cache: Set[P] = Set()

  /** @param tempMax the initial temperature at the start of the simulated annealing process (before cooling) */
  def setMaxTemperature(tempMax: Double): Unit = {
    this.tempMax = tempMax
  }

  /** Finds a local minima.
    *
    * The concept is based on the manner in which liquids freeze or metals recrystallize in the process of annealing.
    * In an annealing process, an initially at high temperature and disordered liquid, is slowly cooled so that the system
    * is approximately in thermodynamic equilibrium at any point in the process. As cooling proceeds, the system becomes
    * more ordered and approaches a "frozen" ground state at T=0. Hence the process can be thought of as an adiabatic
    * approach to the lowest energy state. If the initial temperature of the system is too low, or cooling is too fast,
    * the system may become quenched, forming defects or freezing out in metastable states
    * (ie. trapped in a local minimum energy state).
    *
    * In many ways the algorithm is similar to hill-climbing.
    * The main differences are:
    *  - The next candidate solution is selected randomly within a gaussian neighborhood that shrinks
    * with the temperature and within the current iteration.
    *  - You can actually make a move toward a solution that is worse. This allows the algorithm to
    * move out of local optima.
    *
    * @param params  the initial value for the parameters to optimize.
    * @param fitnessRange the approximate absolute value of the fitnessRange.
    * @return the optimized params.
    */
  override def doOptimization(params: P, fitnessRange: Double): ParameterArrayWithFitness[P] = {
    var ct = 0
    var temperature = tempMax
    val tempMin = tempMax / Math.pow(2.0, NUM_TEMP_ITERATIONS)
    var bestParams =
      if (!optimizee.evaluateByComparison)
        ParameterArrayWithFitness(params, optimizee.evaluateFitness(params))
      else ParameterArrayWithFitness(params, Double.MaxValue)

    // store the best solution we found at any given temperature iteration and use that as the initial
    // start of the next temperature iteration.
    var currentParams: ParameterArrayWithFitness[P] = null

    while (currentParams == null || (temperature > tempMin && !isOptimalFitnessReached(currentParams))) {
      // temperature iteration (temperature drops each time through)
      currentParams = bestParams
      while (ct < N * currentParams.pa.size && !isOptimalFitnessReached(currentParams)) {
        currentParams = findNeighbor(currentParams, ct, temperature, fitnessRange)
        if (currentParams.fitness < bestParams.fitness) {
          bestParams = currentParams
          notifyOfChange(bestParams)
        }
        ct += 1
      }
      ct = 0
      // keep Reducing the temperature until it reaches tempMin
      temperature *= TEMP_DROP_FACTOR
      println("temp = " + temperature + " tempMin = " + tempMin + "\n bestParams = " + bestParams)
    }
    //println("T=" + temperature + "  currentFitness = " + bestParams.getFitness());
    log(ct, bestParams, 0, 0, FormatUtil.formatNumber(temperature))
    bestParams
  }

  /** Select a new point in the neighborhood of our current location.
    * The neighborhood we select from has a radius of r.
    * Uses cache to avoid finding candidates that wre previously searched.
    * @param params      current location in the parameter space.
    * @param ct          iteration count.
    * @param temperature current temperature. Gets cooler with every successive temperature iteration.
    * @return neighboring point that is hopefully better than params.
    */
  private def findNeighbor(params: ParameterArrayWithFitness[P],
                           ct: Int, temperature: Double, fitnessRange: Double): ParameterArrayWithFitness[P] = {
    //double r = (tempMax/5.0+temperature) / (8.0*(N/5.0+ct)*tempMax);
    val curParams = params
    val r = 8 * temperature / ((N + ct) * tempMax)
    var newParams: P = params.pa.getRandomNeighbor(r).asInstanceOf[P]

    // Try to avoid getting the same point as one we have seen before
    var tempRad = r
    var i = 0
    while (cache.contains(newParams) && i < 10) {
      newParams = curParams.pa.getRandomNeighbor(tempRad).asInstanceOf[P]
      tempRad *= 1.05
      i += 1
    }

    val dist = curParams.pa.distance(newParams)
    var newFitness = .0
    val deltaFitness =
      if (optimizee.evaluateByComparison)
        optimizee.compareFitness(newParams, curParams.pa)
      else {
        newFitness = optimizee.evaluateFitness(newParams)
        newFitness - curParams.fitness
      }
    val probability = Math.pow(Math.E, tempMax * -deltaFitness / (fitnessRange * temperature))
    val useWorseSolution = rnd.nextDouble() < probability

    val newParamsWithFitness =
      if (deltaFitness < 0 || useWorseSolution) {
        // Always select the solution if it has a better fitness,
        // but sometimes select a worse solution if the second term evaluates to true.
        ParameterArrayWithFitness(newParams, newFitness)
      } else curParams

    log(ct, newParamsWithFitness, r, dist, FormatUtil.formatNumber(temperature))
    newParamsWithFitness
  }
}