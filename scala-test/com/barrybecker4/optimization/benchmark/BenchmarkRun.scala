// Copyright by Barry G. Becker, 2026. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.benchmark

import com.barrybecker4.math.MathUtil
import com.barrybecker4.optimization.optimizee.EvaluationBudgetExceededException
import com.barrybecker4.optimization.strategy.{OptimizationStrategy, OptimizationStrategyType}
import com.barrybecker4.optimization.strategy.hillclimbing.{GlobalHillClimbingStrategy, HillClimbingConfig, HillClimbingStrategy}
import com.barrybecker4.optimization.strategy.iteratedlocal.IteratedLocalSearchStrategy

import scala.util.Random

/**
  * What one run of one strategy on one problem did.
  * @param fitness the best fitness found (lower is better)
  * @param allocatedBytes bytes allocated by the thread that ran the search, or none if not measured
  * @param stoppedBy why it ended: finished, time or evals
  */
case class RunResult(problem: String, strategy: OptimizationStrategyType, seed: Long, evaluations: Long,
                     millis: Double, fitness: Double, allocatedBytes: Option[Long], stoppedBy: String) {
  def evaluationsPerSecond: Double = if (millis > 0) evaluations * 1000.0 / millis else 0.0
  def bytesPerEvaluation: Option[Double] = allocatedBytes.filter(_ => evaluations > 0).map(_.toDouble / evaluations)
}

object BenchmarkRun {

  private val NANOS_PER_MILLI = 1_000_000L

  /** Strategies that use several threads, where the allocations of the calling thread are not the whole story. */
  private val MULTI_THREADED = Set(OptimizationStrategyType.CONCURRENT_GENETIC_SEARCH)

  /** @return the result, or none if the strategy cannot be used on the problem. */
  def run(problem: BenchmarkProblem, strategy: OptimizationStrategyType, seed: Long, config: BenchmarkConfig): Option[RunResult] = {
    MathUtil.RANDOM.setSeed(seed)
    val optimizee = problem.create(seed)
    val initialGuess = optimizee.getInitialGuess
    if (!strategy.appliesTo(optimizee, initialGuess)) None
    else Some(search(problem.name, optimizee, initialGuess, strategy, seed, config))
  }

  private def search(name: String, problem: com.barrybecker4.optimization.optimizee.optimizees.OptimizeeProblem,
                     initialGuess: com.barrybecker4.optimization.parameter.ParameterArray,
                     strategyType: OptimizationStrategyType, seed: Long, config: BenchmarkConfig): RunResult = {
    val start = System.nanoTime()
    val limited = new LimitedOptimizee(problem, config.maxEvaluations, start + config.timeLimitMillis * NANOS_PER_MILLI)
    val strategy = withMovesIfAsked(strategyType.getStrategy(limited, problem.getFitnessRange, new Random(seed)), config)
    val allocatedBefore = AllocationMeter.bytesAllocatedSoFar
    val fitness = bestFitness(strategy, initialGuess, problem.getFitnessRange)
    val millis = (System.nanoTime() - start) / NANOS_PER_MILLI.toDouble
    val allocated = for (before <- allocatedBefore; after <- AllocationMeter.bytesAllocatedSoFar) yield after - before
    RunResult(name, strategyType, seed, limited.evaluations, millis, fitness,
      allocated.filter(_ => !MULTI_THREADED.contains(strategyType)), limited.stoppedBy)
  }

  /** Turns on the opt-in moves of the strategies that have that setting, when the benchmark is asked to. */
  private def withMovesIfAsked(strategy: OptimizationStrategy, config: BenchmarkConfig): OptimizationStrategy = {
    if (config.useMoves) strategy match {
      case s: IteratedLocalSearchStrategy => s.setConfig(s.getConfig.copy(useMoves = true))
      case s: GlobalHillClimbingStrategy => s.setConfig(s.getConfig.copy(useMoves = true))
      case s: HillClimbingStrategy => s.setConfig(HillClimbingConfig(useMoves = true))
      case _ => ()
    }
    strategy
  }

  /** The best fitness found, even when the search was stopped before it finished. */
  private def bestFitness(strategy: com.barrybecker4.optimization.strategy.OptimizationStrategy,
                          initialGuess: com.barrybecker4.optimization.parameter.ParameterArray, range: Double): Double =
    try {
      val result = strategy.doOptimization(initialGuess, range)
      strategy.bestSoFar.map(_.fitness).filter(_ < result.fitness).getOrElse(result.fitness)
    } catch {
      case _: EvaluationBudgetExceededException => strategy.bestSoFar.map(_.fitness).getOrElse(Double.NaN)
    }
}
