// Copyright by Barry G. Becker, 2000-2026. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.strategy

import com.barrybecker4.common.format.FormatUtil
import com.barrybecker4.math.MathUtil
import com.barrybecker4.optimization.optimizee.Optimizee
import com.barrybecker4.optimization.parameter.{ParameterArray, ParameterArrayWithFitness}

import scala.compiletime.uninitialized
import scala.util.Random

/**
  * How the neighbor proposal radius is scaled on top of the temperature-based base radius.
  */
sealed trait SimulatedAnnealingStepMode

object SimulatedAnnealingStepMode {

  /** Use only the built-in temperature- and iteration-dependent radius (legacy behavior). */
  case object Fixed extends SimulatedAnnealingStepMode

  /**
    * Scale the base radius using observed Metropolis acceptance rate over sliding windows.
    *
    * @param targetRate        desired acceptance fraction (0–1); typical range ~0.3–0.5
    * @param windowProposals   proposals between adjustments; if <= 0, adaptation is disabled
    * @param scaleMin          lower clamp for the multiplicative scale
    * @param scaleMax          upper clamp for the multiplicative scale
    * @param growFactor        multiply scale when acceptance is above target (steps too small)
    * @param shrinkFactor      multiply scale when acceptance is below target (steps too large)
    * @param initialScale      scale at the start of each temperature outer iteration
    */
  final case class AdaptiveAcceptance(
      targetRate: Double = 0.44,
      windowProposals: Int = 50,
      scaleMin: Double = 1e-3,
      scaleMax: Double = 1e3,
      growFactor: Double = 1.1,
      shrinkFactor: Double = 0.9,
      initialScale: Double = 1.0
  ) extends SimulatedAnnealingStepMode
}

/**
  * Tunables for [[SimulatedAnnealingStrategy]] (cooling schedule and base neighbor radius).
  *
  * @param innerIterationsPerDimension inner-loop length is this times parameter count per temperature level
  * @param numTempIterations           used to derive final temperature: `tempMin = tempMax / 2^numTempIterations`
  * @param tempDropFactor              geometric cooling multiplier each outer iteration
  * @param defaultTempMax              initial `tempMax` until [[SimulatedAnnealingStrategy.setMaxTemperature]]
  * @param radiusMultiplier            multiplier in `r_base = radiusMultiplier * temperature / ((N + ct) * tempMax)`
  */
case class SimulatedAnnealingConfig(
    innerIterationsPerDimension: Int = 10,
    numTempIterations: Int = 20,
    tempDropFactor: Double = 0.6,
    defaultTempMax: Double = 1000.0,
    radiusMultiplier: Double = 8.0
)

object SimulatedAnnealingStrategy {
  val DefaultConfig: SimulatedAnnealingConfig = SimulatedAnnealingConfig()
}

/**
  * Simulated annealing optimization strategy.
  * See http://en.wikipedia.org/wiki/Annealing for an explanation of the name.
  *
  * '''Comparison mode (`evaluateByComparison == true`):''' this strategy matches
  * [[com.barrybecker4.optimization.strategy.gradient.DiscreteImprovementFinder]]: a neighbor is an improvement when
  * `compareFitness(newParams, currentParams) < 0`. That matches optimizees where the comparison is a score difference
  * such as `score(new) - score(current)`. It does ''not'' match [[com.barrybecker4.optimization.optimizee.AbsoluteOptimizee.compareFitness]]
  * (positive when the first argument is better); that implementation is intended for `evaluateByComparison == false` only.
  *
  * The [[rnd]] parameter is used for Metropolis coin flips; neighbor sampling uses the [[scala.util.Random]] embedded
  * in the [[com.barrybecker4.optimization.parameter.ParameterArray]] (e.g. [[com.barrybecker4.optimization.parameter.NumericParameterArray]]).
  *
  * @param optimizee the thing to be optimized.
  * @author Barry Becker
  */
class SimulatedAnnealingStrategy(optimizee: Optimizee, rnd: Random = MathUtil.RANDOM)
    extends OptimizationStrategy(optimizee) {

  private var config: SimulatedAnnealingConfig = SimulatedAnnealingStrategy.DefaultConfig
  private var stepMode: SimulatedAnnealingStepMode = SimulatedAnnealingStepMode.Fixed
  private var tempMax: Double = config.defaultTempMax

  /** Initial guess; used to store comparable fitness when `evaluateByComparison` is true. */
  private var initialParams: ParameterArray = uninitialized

  def setConfig(config: SimulatedAnnealingConfig): Unit = {
    this.config = config
  }

  /** @param mode fixed base radius only, or adaptive scaling from acceptance rate (see [[SimulatedAnnealingStepMode]]) */
  def setStepMode(mode: SimulatedAnnealingStepMode): Unit = {
    this.stepMode = mode
  }

  /** @param tempMax the initial temperature at the start of the simulated annealing process (before cooling) */
  def setMaxTemperature(tempMax: Double): Unit = {
    this.tempMax = tempMax
  }

  /** Finds a local minima.
    *
    * The concept is based on the manner in which liquids freeze or metals recrystallize in the process of annealing.
    * In an annealing process, an initially high temperature and disordered liquid, is slowly cooled so that the system
    * is approximately in thermodynamic equilibrium at any point in the process. As cooling proceeds, the system becomes
    * more ordered and approaches a "frozen" ground state at T=0. Hence, the process can be thought of as an adiabatic
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
    * Each outer temperature iteration restarts the chain from the best solution so far ("reheat to best").
    *
    * @param params  the initial value for the parameters to optimize.
    * @param fitnessRange the approximate scale of the objective; must be positive (used in Metropolis acceptance).
    * @return the optimized params.
    */
  override def doOptimization(params: ParameterArray, fitnessRange: Double): ParameterArrayWithFitness = {
    require(
      fitnessRange > 0.0 && fitnessRange.isFinite,
      s"fitnessRange must be finite and > 0 for Metropolis scaling, got $fitnessRange"
    )
    initialParams = params
    val n = config.innerIterationsPerDimension
    val numTempIterations = config.numTempIterations
    val tempDrop = config.tempDropFactor
    val radiusMul = config.radiusMultiplier

    var ct = 0
    var temperature = tempMax
    val tempMin = tempMax / math.pow(2.0, numTempIterations)
    var bestParams =
      if (!optimizee.evaluateByComparison)
        ParameterArrayWithFitness(params, optimizee.evaluateFitness(params))
      else ParameterArrayWithFitness(params, optimizee.compareFitness(params, initialParams))

    var currentParams: ParameterArrayWithFitness = bestParams
    var firstTemperaturePass = true

    while (firstTemperaturePass || (temperature > tempMin && !isOptimalFitnessReached(currentParams))) {
      firstTemperaturePass = false
      currentParams = bestParams
      var stepScale = initialStepScale()
      var windowProposals = 0
      var windowAccepts = 0

      while (ct < n * currentParams.pa.size && !isOptimalFitnessReached(currentParams)) {
        val rBase = radiusMul * temperature / ((n + ct) * tempMax)
        val rEffective = stepScale * rBase
        val (next, accepted) = neighborStep(currentParams, ct, temperature, fitnessRange, rEffective)
        currentParams = next

        stepMode match {
          case SimulatedAnnealingStepMode.Fixed => ()
          case a: SimulatedAnnealingStepMode.AdaptiveAcceptance if a.windowProposals > 0 =>
            windowProposals += 1
            if (accepted) windowAccepts += 1
            if (windowProposals >= a.windowProposals) {
              val rate = windowAccepts.toDouble / windowProposals
              if (rate > a.targetRate + 1e-12)
                stepScale = math.min(a.scaleMax, stepScale * a.growFactor)
              else if (rate < a.targetRate - 1e-12)
                stepScale = math.max(a.scaleMin, stepScale * a.shrinkFactor)
              windowProposals = 0
              windowAccepts = 0
            }
          case _: SimulatedAnnealingStepMode.AdaptiveAcceptance => ()
        }

        if (currentParams.fitness < bestParams.fitness) {
          bestParams = currentParams
          notifyOfChange(bestParams)
        }
        ct += 1
      }
      ct = 0
      temperature *= tempDrop
      trace("temp = " + temperature + " tempMin = " + tempMin + "\n bestParams = " + bestParams)
    }
    log(ct, bestParams, 0, 0, FormatUtil.formatNumber(temperature))
    bestParams
  }

  private def initialStepScale(): Double =
    stepMode match {
      case SimulatedAnnealingStepMode.Fixed                         => 1.0
      case a: SimulatedAnnealingStepMode.AdaptiveAcceptance => a.initialScale
    }

  /** @return (next state, whether the proposal was accepted for adaptive statistics) */
  private def neighborStep(
      params: ParameterArrayWithFitness,
      ct: Int,
      temperature: Double,
      fitnessRange: Double,
      rEffective: Double
  ): (ParameterArrayWithFitness, Boolean) = {
    val curParams = params
    val newParams = params.pa.getRandomNeighbor(rEffective)
    val dist =
      if (isLoggingToFile) curParams.pa.distance(newParams)
      else 0.0
    val (deltaFitness, newFitness) = deltaAndAbsoluteFitness(newParams, curParams)
    val useWorseSolution = metropolisAcceptWorse(deltaFitness, temperature, fitnessRange)
    val accepted = deltaFitness < 0 || useWorseSolution
    val newParamsWithFitness =
      if (accepted) acceptedState(newParams, newFitness)
      else curParams
    log(ct, newParamsWithFitness, rEffective, dist, FormatUtil.formatNumber(temperature))
    (newParamsWithFitness, accepted)
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
    val probability = math.exp(tempMax * -deltaFitness / (fitnessRange * temperature))
    rnd.nextDouble() < probability
  }

  private def acceptedState(newParams: ParameterArray, newFitness: Double): ParameterArrayWithFitness = {
    val storedFitness =
      if (optimizee.evaluateByComparison) optimizee.compareFitness(newParams, initialParams)
      else newFitness
    ParameterArrayWithFitness(newParams, storedFitness)
  }
}
