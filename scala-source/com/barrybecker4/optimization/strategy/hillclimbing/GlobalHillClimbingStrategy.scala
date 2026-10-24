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

  /** How many top global samples seed local hill climbing. */
  def setMultistartCount(k: Int): Unit = {
    config = config.copy(multistartCount = k)
  }

  /** Budget for [[GlobalSampleStrategy]] before local hill climbs (larger = more exploration in high-D spaces). */
  def setGlobalSampleBudget(samples: Int): Unit = {
    config = config.copy(globalSampleBudget = samples)
  }

  /**
    * Perform the optimization of the optimizee.
    * @param params parameter array
    * @param fitnessRange the approximate absolute value of the fitnessRange.
    * @return optimized params
    */
  override def doOptimization(params: ParameterArray,
                              fitnessRange: Double): ParameterArrayWithFitness =
    new MultistartRun(params, fitnessRange).execute()

  /**
    * One multistart run: sample globally, then climb from each seed, keeping the best result.
    *
    * In comparison mode every fitness handled here (seeds, climbs and the result) is measured against `params`, the
    * initial guess, so results from different seeds can be compared with each other.
    * If the evaluation budget runs out, the best point evaluated so far is returned.
    */
  private final class MultistartRun(params: ParameterArray, fitnessRange: Double) {

    private var best: Option[ParameterArrayWithFitness] = None

    /** Evaluated at most once; if even that exceeds the budget the result is the worst possible fitness. */
    private lazy val initialGuess: ParameterArrayWithFitness = {
      val fitness =
        if (optimizee.evaluateByComparison) 0.0 // it is the baseline, so no better or worse than itself
        else withinBudget(optimizee.evaluateFitness(params)).getOrElse(Double.MaxValue)
      ParameterArrayWithFitness(params, fitness)
    }

    def execute(): ParameterArrayWithFitness = {
      val seeds = sampleSeeds()
      seeds.foreach(consider)
      // lazily forked, so that each fork happens after the previous climb, as it always has
      startingPoints(seeds).foreach(start => withinBudget(climbFrom(start)).foreach(consider))
      best.getOrElse(initialGuess)
    }

    private def consider(candidate: ParameterArrayWithFitness): Unit = {
      updateRunBest(candidate)
      if (best.forall(_.fitness > candidate.fitness)) best = Some(candidate)
    }

    private def withinBudget[T](f: => T): Option[T] =
      try Some(f)
      catch {
        case _: EvaluationBudgetExceededException => None
      }

    private def sampleSeeds(): Seq[ParameterArrayWithFitness] = {
      val sampler = new GlobalSampleStrategy(optimizee)
      copyRunConfigurationTo(sampler)
      sampler.setSamplingRate(config.globalSampleBudget)
      withinBudget(
        sampler.doOptimizationTopK(params, fitnessRange, config.multistartCount, config.effectiveSeedSelection)
      ).getOrElse {
        sampler.bestSoFar.foreach(consider)
        Seq.empty
      }
    }

    /** The first seed is used as is; later ones get their own random number generator. */
    private def startingPoints(seeds: Seq[ParameterArrayWithFitness]): Iterator[ParameterArrayWithFitness] =
      if (seeds.isEmpty) Iterator.single(initialGuess)
      else seeds.iterator.zipWithIndex.map { case (seed, idx) => if (idx == 0) seed else SeedForking.fork(seed, idx) }

    private def climbFrom(start: ParameterArrayWithFitness): ParameterArrayWithFitness = {
      val climber = new HillClimbingStrategy(optimizee)
      climber.setConfig(HillClimbingConfig(useMoves = config.useMoves))
      copyRunConfigurationTo(climber)
      try climber.climb(start, params)
      catch {
        case _: EvaluationBudgetExceededException => climber.bestSoFar.getOrElse(start)
      }
    }
  }
}
