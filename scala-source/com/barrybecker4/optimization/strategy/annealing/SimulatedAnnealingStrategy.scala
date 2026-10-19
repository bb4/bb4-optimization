// Copyright by Barry G. Becker, 2000-2026. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.strategy.annealing

import com.barrybecker4.common.format.FormatUtil
import com.barrybecker4.math.MathUtil
import com.barrybecker4.optimization.optimizee.{EvaluationBudgetExceededException, Optimizee}
import com.barrybecker4.optimization.parameter.*
import com.barrybecker4.optimization.strategy.OptimizationStrategy

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
  ) extends SimulatedAnnealingStepMode {
    require(targetRate > 0.0 && targetRate < 1.0, s"targetRate must be in (0, 1), got $targetRate")
    require(scaleMin > 0.0 && scaleMin <= scaleMax, s"need 0 < scaleMin <= scaleMax, got $scaleMin, $scaleMax")
    require(growFactor > 0.0 && shrinkFactor > 0.0, s"grow/shrink factors must be positive, got $growFactor, $shrinkFactor")
    require(initialScale > 0.0, s"initialScale must be positive, got $initialScale")
  }
}

/**
  * Tunable parameters for [[SimulatedAnnealingStrategy]] (cooling schedule and base neighbor radius).
  *
  * @param innerIterationsPerDimension inner-loop length is this times parameter count per temperature level
  * @param numTempIterations           used to derive final temperature: `tempMin = tempMax / 2^numTempIterations`
  * @param tempDropFactor              geometric cooling multiplier each outer iteration
  * @param defaultTempMax              initial `tempMax` until [[SimulatedAnnealingStrategy.setMaxTemperature]]
  * @param radiusMultiplier            multiplier in `r_base = radiusMultiplier * temperature / ((N + ct) * tempMax)`
  * @param stepMode                    fixed base radius or adaptive acceptance scaling
  */
case class SimulatedAnnealingConfig(
    innerIterationsPerDimension: Int = 10,
    numTempIterations: Int = 20,
    tempDropFactor: Double = 0.6,
    defaultTempMax: Double = 1000.0,
    radiusMultiplier: Double = 8.0,
    stepMode: SimulatedAnnealingStepMode = SimulatedAnnealingStepMode.AdaptiveAcceptance()
) {
  // A tempDropFactor >= 1 never cools, so the outer loop would never terminate.
  require(innerIterationsPerDimension > 0, s"innerIterationsPerDimension must be positive, got $innerIterationsPerDimension")
  require(numTempIterations > 0, s"numTempIterations must be positive, got $numTempIterations")
  require(tempDropFactor > 0.0 && tempDropFactor < 1.0, s"tempDropFactor must be in (0, 1), got $tempDropFactor")
  require(defaultTempMax > 0.0 && defaultTempMax.isFinite, s"defaultTempMax must be finite and > 0, got $defaultTempMax")
  require(radiusMultiplier > 0.0 && radiusMultiplier.isFinite, s"radiusMultiplier must be finite and > 0, got $radiusMultiplier")
}

object SimulatedAnnealingStrategy {
  val DefaultConfig: SimulatedAnnealingConfig = SimulatedAnnealingConfig()

  /** [[com.barrybecker4.optimization.parameter.types.AbstractParameter.tweakNumericValue]] requires |r| <= 1.5. */
  private[strategy] val MAX_PROPOSAL_RADIUS: Double = 1.5

  /** The initial temperature is the objective's fitness range divided by this. */
  private val FITNESS_RANGE_PER_TEMPERATURE: Double = 10.0

  /** Sensible `tempMax` for [[SimulatedAnnealingStrategy.setMaxTemperature]] given the approximate fitness range. */
  def defaultMaxTemperature(fitnessRange: Double): Double = fitnessRange / FITNESS_RANGE_PER_TEMPERATURE
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
  * Budget: when the optimizee is wrapped with an evaluation budget, this strategy catches
  * [[EvaluationBudgetExceededException]] and returns best-so-far (seeded via [[notifyOfChange]] after the initial eval).
  *
  * @param optimizee the thing to be optimized.
  * @author Barry Becker
  */
class SimulatedAnnealingStrategy(optimizee: Optimizee, rnd: Random = MathUtil.RANDOM)
    extends OptimizationStrategy(optimizee) {

  private var config: SimulatedAnnealingConfig = SimulatedAnnealingStrategy.DefaultConfig
  private var tempMax: Double = config.defaultTempMax

  def getConfig: SimulatedAnnealingConfig = config

  def setConfig(config: SimulatedAnnealingConfig): Unit = {
    this.config = config
  }

  /** @param mode fixed base radius only, or adaptive scaling from acceptance rate (see [[SimulatedAnnealingStepMode]]) */
  def setStepMode(mode: SimulatedAnnealingStepMode): Unit = {
    this.config = config.copy(stepMode = mode)
  }

  /** @param tempMax the initial temperature at the start of the simulated annealing process (before cooling) */
  def setMaxTemperature(tempMax: Double): Unit = {
    require(tempMax > 0.0 && tempMax.isFinite, s"max temperature must be finite and > 0, got $tempMax")
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
    * (i.e. trapped in a local minimum energy state).
    *
    * In many ways the algorithm is similar to hill-climbing.
    * The main differences are:
    *  - The next candidate solution is selected randomly within a Gaussian neighborhood that shrinks
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
    evaluateStart(params) match {
      case None => ParameterArrayWithFitness(params, Double.MaxValue) // no budget left for even one evaluation
      case Some(start) =>
        notifyOfChange(start)
        new AnnealingRun(params, fitnessRange, effectiveStepMode(params), start).run()
    }
  }

  /** @return the initial point with its fitness, or None if the evaluation budget was already spent. */
  private def evaluateStart(params: ParameterArray): Option[ParameterArrayWithFitness] =
    try {
      val fitness =
        if (optimizee.evaluateByComparison) optimizee.compareFitness(params, params)
        else optimizee.evaluateFitness(params)
      Some(ParameterArrayWithFitness(params, fitness))
    } catch {
      case _: EvaluationBudgetExceededException => None
    }

  /**
    * Adaptive radius scaling helps continuous landscapes; on discrete spaces it tends to overshoot,
    * so prefer Fixed there even when the config says AdaptiveAcceptance.
    */
  private def effectiveStepMode(params: ParameterArray): SimulatedAnnealingStepMode =
    config.stepMode match {
      case _: SimulatedAnnealingStepMode.AdaptiveAcceptance if isDiscreteProblem(params) =>
        SimulatedAnnealingStepMode.Fixed
      case mode => mode
    }

  private def isDiscreteProblem(params: ParameterArray): Boolean = params match {
    case _: PermutedParameterArray | _: VariableLengthIntSet => true
    case n: NumericParameterArray => (0 until n.size).exists(i => n.get(i).isIntegerOnly)
    case _ => false
  }

  /**
    * State of one annealing run: the geometric cooling loop, with the chain restarted from the best point
    * at every temperature level ("reheat to best").
    *
    * @param baseline the starting point; in comparison mode fitness is always stored relative to it.
    */
  private final class AnnealingRun(
      baseline: ParameterArray,
      fitnessRange: Double,
      stepMode: SimulatedAnnealingStepMode,
      start: ParameterArrayWithFitness
  ) {
    private val innerIterationsPerDimension = config.innerIterationsPerDimension
    private val tempMin = tempMax / math.pow(2.0, config.numTempIterations)
    private var best = start
    private var current = start
    private var step = 0
    private var temperature = tempMax

    /** @return the best point found; if the budget runs out part way, the best found so far. */
    def run(): ParameterArrayWithFitness = {
      try coolUntilFrozen()
      catch {
        case _: EvaluationBudgetExceededException => () // fall through and return best so far
      }
      logFinal()
      best
    }

    /** The first temperature level always runs, even if the starting point already satisfies the stop test. */
    private def coolUntilFrozen(): Unit = {
      runTemperatureLevel()
      coolDown()
      while (shouldKeepCooling) {
        runTemperatureLevel()
        coolDown()
      }
    }

    private def shouldKeepCooling: Boolean = temperature > tempMin && !isOptimalFitnessReached(current)

    private def coolDown(): Unit = {
      temperature *= config.tempDropFactor
      trace("temp = " + temperature + " tempMin = " + tempMin + "\n bestParams = " + best)
    }

    private def runTemperatureLevel(): Unit = {
      current = best
      val levelLength = innerIterationsPerDimension * current.pa.size
      val scaler = new StepScaler(stepMode, levelLength)
      while (step < levelLength && !isOptimalFitnessReached(current)) {
        val (next, accepted) = proposeAndMaybeAccept(scaler)
        current = next
        scaler.record(accepted)
        keepIfBest()
        step += 1
      }
      step = 0
    }

    private def keepIfBest(): Unit =
      if (current.fitness < best.fitness) {
        best = current
        notifyOfChange(best)
      }

    /** Radius shrinks with temperature and with the step count, then is scaled by the step mode. */
    private def proposalRadius(scaler: StepScaler): Double = {
      val base = config.radiusMultiplier * temperature / ((innerIterationsPerDimension + step) * tempMax)
      math.min(SimulatedAnnealingStrategy.MAX_PROPOSAL_RADIUS, scaler.scale * base)
    }

    /** @return (next state, whether the proposal was accepted for adaptive statistics) */
    private def proposeAndMaybeAccept(scaler: StepScaler): (ParameterArrayWithFitness, Boolean) = {
      val radius = proposalRadius(scaler)
      val candidate = current.pa.getRandomNeighbor(radius)
      val dist = if (isLoggingToFile) current.pa.distance(candidate) else 0.0
      val (delta, candidateFitness) = deltaAndAbsoluteFitness(candidate, current)
      // Improving moves are always taken; only worse ones need the Metropolis test (and its random draw).
      val accepted = delta < 0 || metropolisAcceptWorse(delta, temperature, fitnessRange)
      val next = if (accepted) acceptedState(candidate, candidateFitness) else current
      logStep(next, radius, dist)
      (next, accepted)
    }

    /** @return (delta for Metropolis, absolute fitness of candidate when not comparison mode; 0.0 if comparison) */
    private def deltaAndAbsoluteFitness(candidate: ParameterArray,
                                        cur: ParameterArrayWithFitness): (Double, Double) =
      if (optimizee.evaluateByComparison) (optimizee.compareFitness(candidate, cur.pa), 0.0)
      else {
        val fitness = optimizee.evaluateFitness(candidate)
        (fitness - cur.fitness, fitness)
      }

    private def acceptedState(candidate: ParameterArray, candidateFitness: Double): ParameterArrayWithFitness = {
      val stored =
        if (optimizee.evaluateByComparison) optimizee.compareFitness(candidate, baseline)
        else candidateFitness
      ParameterArrayWithFitness(candidate, stored)
    }

    // Formatting the temperature is only worth doing when someone is listening.
    private def logStep(state: ParameterArrayWithFitness, radius: Double, dist: Double): Unit =
      if (isLoggingToFile) log(step, state, radius, dist, FormatUtil.formatNumber(temperature))

    private def logFinal(): Unit =
      if (isLoggingToFile) log(step, best, 0, 0, FormatUtil.formatNumber(temperature))
  }

  private def metropolisAcceptWorse(deltaFitness: Double, temperature: Double, fitnessRange: Double): Boolean = {
    val probability = math.exp(tempMax * -deltaFitness / (fitnessRange * temperature))
    rnd.nextDouble() < probability
  }
}
