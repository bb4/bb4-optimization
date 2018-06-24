// Copyright by Barry G. Becker, 2000-2018. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.strategy

import com.barrybecker4.common.format.FormatUtil
import com.barrybecker4.common.math.MathUtil
import com.barrybecker4.optimization.optimizee.Optimizee
import com.barrybecker4.optimization.parameter.ParameterArray
import SimulatedAnnealingStrategy._

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
class SimulatedAnnealingStrategy(optimizee: Optimizee) extends OptimizationStrategy(optimizee) {
  private var tempMax = SimulatedAnnealingStrategy.DEFAULT_TEMP_MAX

  /** @param tempMax the initial temperature at the start of the simulated annealing process (before cooling) */
  def setMaxTemperature(tempMax: Double): Unit = {
    this.tempMax = tempMax
  }

  /**
    * Finds a local minima.
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
  override def doOptimization(params: ParameterArray, fitnessRange: Double): ParameterArray = {
    var ct = 0
    var temperature = tempMax
    val tempMin = tempMax / Math.pow(2.0, NUM_TEMP_ITERATIONS)
    if (!optimizee.evaluateByComparison) {
      val currentFitness = optimizee.evaluateFitness(params)
      params.setFitness(currentFitness)
    }
    // store the best solution we found at any given temperature iteration and use that as the initial
    // start of the next temperature iteration.
    var bestParams = params.copy
    var currentParams: ParameterArray = null
    do { // temperature iteration (temperature drops each time through)
      currentParams = bestParams
      do {
        currentParams = findNeighbor(currentParams, ct, temperature)
        if (currentParams.getFitness < bestParams.getFitness) {
          bestParams = currentParams.copy
          notifyOfChange(bestParams)
        }
        ct += 1
      } while (ct < N * currentParams.size && !isOptimalFitnessReached(currentParams))
      ct = 0
      // keep Reducing the temperature until it reaches tempMin
      temperature *= TEMP_DROP_FACTOR
      println("temp = " + temperature + " tempMin = " + tempMin + "\n bestParams = " + bestParams)
    } while (temperature > tempMin && !isOptimalFitnessReached(currentParams))
    //println("T=" + temperature + "  currentFitness = " + bestParams.getFitness());
    log(ct, bestParams.getFitness, 0, 0, bestParams, FormatUtil.formatNumber(temperature))
    bestParams
  }

  /**
    * Select a new point in the neighborhood of our current location
    * The neighborhood we select from has a radius of r.
    *
    * @param params      current location in the parameter space.
    * @param ct          iteration count.
    * @param temperature current temperature. Gets cooler with every successive temperature iteration.
    * @return neighboring point that is hopefully better than params.
    */
  private def findNeighbor(params: ParameterArray, ct: Int, temperature: Double) = {
    //double r = (tempMax/5.0+temperature) / (8.0*(N/5.0+ct)*tempMax);
    var curParams = params
    val r = 2 * temperature / ((N + ct) * tempMax)
    var newParams = curParams.getRandomNeighbor(r)
    val dist = curParams.distance(newParams)
    var deltaFitness = .0
    var newFitness = .0
    if (optimizee.evaluateByComparison) deltaFitness = optimizee.compareFitness(newParams, curParams)
    else {
      newFitness = optimizee.evaluateFitness(newParams)
      newParams.setFitness(newFitness)
      deltaFitness = curParams.getFitness - newFitness
    }
    val probability = Math.pow(Math.E, tempMax * deltaFitness / temperature)
    val useWorseSolution = MathUtil.RANDOM.nextDouble < probability
    if (deltaFitness > 0 || useWorseSolution) { // we always select the solution if it has a better fitness,
      // but we sometimes select a worse solution if the second term evaluates to true.
      if (deltaFitness < 0 && useWorseSolution)
        println("Selected worse solution with prob=" +
          probability + " delta=" + deltaFitness + " / temp=" + temperature)
      curParams = newParams
    }
    //println("T="+temperature+" ct="+ct+" dist="+dist+" deltaFitness="
    //        + deltaFitness+"  currentFitness = "+ curParams.getFitness() );
    log(ct, curParams.getFitness, r, dist, curParams, FormatUtil.formatNumber(temperature))
    curParams
  }
}