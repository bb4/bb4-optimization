// Copyright by Barry G. Becker, 2000-2026. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.strategy.annealing

import com.barrybecker4.math.MathUtil
import com.barrybecker4.optimization.optimizee.{EvaluationBudgetExceededException, Optimizee}
import com.barrybecker4.optimization.parameter.*
import com.barrybecker4.optimization.strategy.{GlobalSampleStrategy, OptimizationStrategy, SeedForking}

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
    config = config.copy(multistartCount = k)
  }

  /** Budget for [[GlobalSampleStrategy]] before local SA runs (larger = more exploration in high-D spaces). */
  def setGlobalSampleBudget(samples: Int): Unit = {
    config = config.copy(globalSampleBudget = samples)
  }

  override def doOptimization(params: ParameterArray,
                              fitnessRange: Double): ParameterArrayWithFitness = {
    val champion = new Champion
    lazy val initialGuess = evaluateInitialGuess(params)

    val seeds = sampleSeeds(params, fitnessRange, champion)
    if (seeds.isEmpty) annealFrom(initialGuess, fitnessRange, champion)
    else seeds.zipWithIndex.foreach { case (seed, idx) =>
      annealFrom(startPoint(seed, idx), fitnessRange, champion)
    }

    champion.best.getOrElse(initialGuess)
  }

  /** Best result of this call (the run-wide best is tracked separately by the base class). */
  private final class Champion {
    var best: Option[ParameterArrayWithFitness] = None

    def offer(candidate: ParameterArrayWithFitness): Unit = {
      updateRunBest(candidate)
      if (best.forall(_.fitness > candidate.fitness)) best = Some(candidate)
    }
  }

  private def withinBudget[T](f: => T): Option[T] =
    try Some(f)
    catch {
      case _: EvaluationBudgetExceededException => None
    }

  /** Evaluated at most once; if even that exceeds the budget the result is the worst possible fitness. */
  private def evaluateInitialGuess(params: ParameterArray): ParameterArrayWithFitness = {
    val fitness =
      if (optimizee.evaluateByComparison) Double.MaxValue
      else withinBudget(optimizee.evaluateFitness(params)).getOrElse(Double.MaxValue)
    ParameterArrayWithFitness(params, fitness)
  }

  /** Global exploration phase: returns the seeds for local annealing (empty if the budget ran out first). */
  private def sampleSeeds(params: ParameterArray,
                          fitnessRange: Double,
                          champion: Champion): Seq[ParameterArrayWithFitness] = {
    val sampler = new GlobalSampleStrategy(optimizee)
    copyRunConfigurationTo(sampler)
    sampler.setSamplingRate(config.globalSampleBudget)

    val seeds = withinBudget {
      sampler.doOptimizationTopK(params, fitnessRange, config.multistartCount, config.effectiveSeedSelection)
    }.getOrElse {
      sampler.bestSoFar.foreach(champion.offer)
      Seq.empty
    }
    seeds.foreach(champion.offer)
    seeds
  }

  /** The first seed shares the caller's RNG (single-start behavior); later seeds get their own. */
  private def startPoint(seed: ParameterArrayWithFitness, idx: Int): ParameterArrayWithFitness =
    if (idx == 0) seed else SeedForking.fork(seed, idx)

  private def annealFrom(start: ParameterArrayWithFitness,
                         fitnessRange: Double,
                         champion: Champion): Unit =
    withinBudget(runSimulatedAnnealingFrom(start, fitnessRange)).foreach(champion.offer)

  private def runSimulatedAnnealingFrom(seed: ParameterArrayWithFitness,
                                        fitnessRange: Double): ParameterArrayWithFitness = {
    val strategy = new SimulatedAnnealingStrategy(optimizee, rnd)
    copyRunConfigurationTo(strategy)
    strategy.setConfig(config.annealingConfig)
    strategy.setMaxTemperature(SimulatedAnnealingStrategy.defaultMaxTemperature(fitnessRange))
    strategy.doOptimization(seed.pa, fitnessRange)
  }
}
