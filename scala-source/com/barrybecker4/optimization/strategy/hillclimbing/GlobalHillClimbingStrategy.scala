// Copyright by Barry G. Becker, 2000-2026. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.strategy.hillclimbing

import com.barrybecker4.optimization.optimizee.{EvaluationBudgetExceededException, Optimizee}
import com.barrybecker4.optimization.parameter.*
import com.barrybecker4.optimization.strategy.{GlobalSampleStrategy, OptimizationStrategy, SeedForking}


object GlobalHillClimbingStrategy {
  /** Default passed to [[GlobalSampleStrategy.setSamplingRate]] as the global sample budget. */
  val DEFAULT_GLOBAL_SAMPLE_BUDGET: Int = 1000

  /** Number of best global samples from which to run local hill climbing. */
  val DEFAULT_MULTISTART_COUNT: Int = 5
}

/**
  * Hybrid strategy: global exploration (grid-style sampling via [[GlobalSampleStrategy]]), then local
  * [[HillClimbingStrategy]] from each of the best `multistartCount` samples; returns the best local optimum.
  *
  * @param optimizee the thing to be optimized.
  * @author Barry Becker
  */
class GlobalHillClimbingStrategy(optimizee: Optimizee) extends OptimizationStrategy(optimizee) {

  private var config: GlobalHillClimbingConfig = GlobalHillClimbingConfig()

  def setConfig(newConfig: GlobalHillClimbingConfig): Unit = {
    config = newConfig
  }

  def getConfig: GlobalHillClimbingConfig = config

  /** How many top global samples seed local hill climbing (clamped to at least 1 at runtime). */
  def setMultistartCount(k: Int): Unit = {
    assert(k >= 1, "multistart count must be positive")
    config = config.copy(multistartCount = k)
  }

  /** Budget for [[GlobalSampleStrategy]] before local hill climbs (larger = more exploration in high-D spaces). */
  def setGlobalSampleBudget(samples: Int): Unit = {
    assert(samples > 0, "global sample budget must be positive")
    config = config.copy(globalSampleBudget = samples)
  }

  /**
    * Perform the optimization of the optimizee.
    * @param params parameter array
    * @param fitnessRange the approximate absolute value of the fitnessRange.
    * @return optimized params
    */
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

    // Evaluated at most once; if even that exceeds the budget the result is the worst possible fitness.
    lazy val initialGuess: ParameterArrayWithFitness = {
      val fitness =
        if (optimizee.evaluateByComparison) Double.MaxValue
        else runPhase(optimizee.evaluateFitness(params)).getOrElse(Double.MaxValue)
      ParameterArrayWithFitness(params, fitness)
    }

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
      runPhase(runHillClimbingFrom(initialGuess, fitnessRange)).foreach(consider)
    } else {
      seeds.zipWithIndex.foreach { case (seed, idx) =>
        val start = if (idx == 0) seed else SeedForking.fork(seed, idx)
        runPhase(runHillClimbingFrom(start, fitnessRange)).foreach(consider)
      }
    }

    overallBest.getOrElse(initialGuess)
  }

  private def runHillClimbingFrom(seed: ParameterArrayWithFitness, fitnessRange: Double): ParameterArrayWithFitness = {
    val strategy = new HillClimbingStrategy(optimizee)
    copyRunConfigurationTo(strategy)
    try strategy.doOptimization(seed.pa, fitnessRange)
    catch {
      case _: EvaluationBudgetExceededException =>
        strategy.bestSoFar.getOrElse(seed)
    }
  }
}
