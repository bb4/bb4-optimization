// Copyright by Barry G. Becker, 2000-2026. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.strategy.annealing

import com.barrybecker4.math.MathUtil
import com.barrybecker4.optimization.optimizee.{EvaluationBudgetExceededException, Optimizee}
import com.barrybecker4.optimization.parameter.*
import com.barrybecker4.optimization.strategy.{GlobalSampleStrategy, OptimizationStrategy}
import com.barrybecker4.optimization.strategy.annealing.SimulatedAnnealingStrategy

import scala.util.Random

object GlobalSimulatedAnnealingStrategy {
  /** Default passed to [[GlobalSampleStrategy.setSamplingRate]] as the global sample budget. */
  val DEFAULT_GLOBAL_SAMPLE_BUDGET: Int = 1000

  /** Number of the best global samples from which to run local simulated annealing. */
  val DEFAULT_MULTISTART_COUNT: Int = 5
}

/**
  * Hybrid strategy: global exploration (grid-style sampling via [[GlobalSampleStrategy]]), then local
  * [[SimulatedAnnealingStrategy]] from each of the best `multistartCount` samples; returns the best result.
  *
  * Prefer this over [[GLOBAL_HILL_CLIMBING]] when local optima are deep and annealing can escape them;
  * prefer [[SIMULATED_ANNEALING]] alone when a single chain with the evaluation budget is enough.
  *
  * Extra multistart seeds often share the same [[Random]] as the initial guess; annealing mutates that RNG.
  * Fork per seed (after the primary) so the first local run matches single-start behavior and later starts
  * do not depend on how many draws earlier seeds consumed.
  *
  * @param optimizee the thing to be optimized.
  * @param rnd       random source for Metropolis flips in each local SA run
  * @author Barry Becker
  */
class GlobalSimulatedAnnealingStrategy(optimizee: Optimizee, rnd: Random = MathUtil.RANDOM)
    extends OptimizationStrategy(optimizee) {

  private var config: GlobalSimulatedAnnealingConfig = GlobalSimulatedAnnealingConfig()

  def setConfig(newConfig: GlobalSimulatedAnnealingConfig): Unit = {
    config = newConfig
  }

  def getConfig: GlobalSimulatedAnnealingConfig = config

  /** How many top global samples seed local SA (clamped to at least 1 at runtime). */
  def setMultistartCount(k: Int): Unit = {
    assert(k >= 1, "multistart count must be positive")
    config = config.copy(multistartCount = k)
  }

  /** Budget for [[GlobalSampleStrategy]] before local SA runs (larger = more exploration in high-D spaces). */
  def setGlobalSampleBudget(samples: Int): Unit = {
    assert(samples > 0, "global sample budget must be positive")
    config = config.copy(globalSampleBudget = samples)
  }

  override def doOptimization(params: ParameterArray,
                              fitnessRange: Double): ParameterArrayWithFitness = {
    var overallBest: Option[ParameterArrayWithFitness] = None

    def consider(candidate: ParameterArrayWithFitness): Unit = {
      updateRunBest(candidate)
      if (overallBest.forall(_.fitness > candidate.fitness)) overallBest = Some(candidate)
    }

    def runPhase[T](f: => T): Option[T] =
      try Some(f)
      catch {
        case _: EvaluationBudgetExceededException => None
      }

    def initialGuess: ParameterArrayWithFitness =
      if (optimizee.evaluateByComparison) ParameterArrayWithFitness(params, Double.MaxValue)
      else ParameterArrayWithFitness(params, optimizee.evaluateFitness(params))

    val gsStrategy = new GlobalSampleStrategy(optimizee)
    copyRunConfigurationTo(gsStrategy)
    gsStrategy.setSamplingRate(config.globalSampleBudget)

    val seeds = runPhase {
      gsStrategy.doOptimizationTopK(
        params,
        fitnessRange,
        config.multistartCount,
        config.effectiveSeedSelection
      )
    } match {
      case Some(s) => s
      case None =>
        gsStrategy.bestSoFar.foreach(consider)
        IndexedSeq.empty
    }
    seeds.foreach(consider)

    if (seeds.isEmpty) {
      runPhase(runSimulatedAnnealingFrom(initialGuess, fitnessRange)).foreach(consider)
    } else {
      seeds.zipWithIndex.foreach { case (seed, idx) =>
        val start = if (idx == 0) seed else parameterArrayWithForkedRnd(seed, idx)
        runPhase(runSimulatedAnnealingFrom(start, fitnessRange)).foreach(consider)
      }
    }

    overallBest.getOrElse(initialGuess)
  }

  private def parameterArrayWithForkedRnd(s: ParameterArrayWithFitness, forkIdx: Int): ParameterArrayWithFitness = {
    val salt = s.pa.## * 31L + forkIdx.toLong * 1103515245L
    val forked = new Random(salt)
    val newPa = s.pa match {
      case v: VariableLengthIntSet =>
        val paramsSeq = (0 until v.size).map(v.get)
        VariableLengthIntSet.createInstance(paramsSeq, v.fullSeq, forked)
      case n: NumericParameterArray =>
        NumericParameterArray(n.params, n.numSteps, forked)
      case p: PermutedParameterArray =>
        p.forkWithRnd(forked)
      case other => other
    }
    ParameterArrayWithFitness(newPa, s.fitness)
  }

  private def runSimulatedAnnealingFrom(
      seed: ParameterArrayWithFitness,
      fitnessRange: Double
  ): ParameterArrayWithFitness = {
    val strategy = new SimulatedAnnealingStrategy(optimizee, rnd)
    copyRunConfigurationTo(strategy)
    strategy.setConfig(config.annealingConfig)
    strategy.setMaxTemperature(fitnessRange / 10.0)
    try strategy.doOptimization(seed.pa, fitnessRange)
    catch {
      case _: EvaluationBudgetExceededException =>
        strategy.bestSoFar.getOrElse(seed)
    }
  }
}
