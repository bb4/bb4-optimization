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
class SimulatedAnnealingStrategy(optimizee: Optimizee, rnd: Random = MathUtil.RANDOM)
  extends OptimizationStrategy(optimizee) {

  private var tempMax = SimulatedAnnealingStrategy.DEFAULT_TEMP_MAX

  /** Initial guess; used to store comparable fitness when `evaluateByComparison` is true. */
  private var initialParams: ParameterArray = _

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
  override def doOptimization(params: ParameterArray, fitnessRange: Double): ParameterArrayWithFitness = {
    initialParams = params
    var ct = 0
    var temperature = tempMax
    val tempMin = tempMax / Math.pow(2.0, NUM_TEMP_ITERATIONS)
    var bestParams =
      if (!optimizee.evaluateByComparison)
        ParameterArrayWithFitness(params, optimizee.evaluateFitness(params))
      else ParameterArrayWithFitness(params, optimizee.compareFitness(params, initialParams))

    // store the best solution we found at any given temperature iteration and use that as the initial
    // start of the next temperature iteration.
    var currentParams: ParameterArrayWithFitness = null

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
      trace("temp = " + temperature + " tempMin = " + tempMin + "\n bestParams = " + bestParams)
    }
    //println("T=" + temperature + "  currentFitness = " + bestParams.getFitness());
    log(ct, bestParams, 0, 0, FormatUtil.formatNumber(temperature))
    bestParams
  }

  /** Select a new point in the neighborhood of our current location.
    * The neighborhood we select from has a radius of r.
    * @param params      current location in the parameter space.
    * @param ct          iteration count.
    * @param temperature current temperature. Gets cooler with every successive temperature iteration.
    * @return neighboring point that is hopefully better than params.
    */
  private def findNeighbor(params: ParameterArrayWithFitness,
                           ct: Int, temperature: Double, fitnessRange: Double): ParameterArrayWithFitness = {
    val curParams = params
    val r = 8 * temperature / ((N + ct) * tempMax)
    val newParams = params.pa.getRandomNeighbor(r)
    val dist = curParams.pa.distance(newParams)
    val (deltaFitness, newFitness) = deltaAndAbsoluteFitness(newParams, curParams)
    val useWorseSolution = metropolisAcceptWorse(deltaFitness, temperature, fitnessRange)
    val newParamsWithFitness =
      if (deltaFitness < 0 || useWorseSolution) acceptedState(newParams, newFitness)
      else curParams
    log(ct, newParamsWithFitness, r, dist, FormatUtil.formatNumber(temperature))
    newParamsWithFitness
  }

  /** @return (delta for Metropolis, absolute fitness of newParams when not comparison mode; 0.0 if comparison) */
  private def deltaAndAbsoluteFitness(newParams: ParameterArray, curParams: ParameterArrayWithFitness): (Double, Double) =
    if (optimizee.evaluateByComparison) {
      val d = optimizee.compareFitness(newParams, curParams.pa)
      (d, 0.0)
    } else {
      val nf = optimizee.evaluateFitness(newParams)
      (nf - curParams.fitness, nf)
    }

  private def metropolisAcceptWorse(deltaFitness: Double, temperature: Double, fitnessRange: Double): Boolean = {
    val probability = Math.pow(Math.E, tempMax * -deltaFitness / (fitnessRange * temperature))
    rnd.nextDouble() < probability
  }

  private def acceptedState(newParams: ParameterArray, newFitness: Double): ParameterArrayWithFitness = {
    val storedFitness =
      if (optimizee.evaluateByComparison) optimizee.compareFitness(newParams, initialParams)
      else newFitness
    ParameterArrayWithFitness(newParams, storedFitness)
  }
}