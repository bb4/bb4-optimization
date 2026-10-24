// Copyright by Barry G. Becker, 2026. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.benchmark

import com.barrybecker4.math.MathUtil
import com.barrybecker4.optimization.optimizee.EvaluationBudgetExceededException
import com.barrybecker4.optimization.strategy.OptimizationStrategyType

import scala.util.Random

/**
  * What one run of one strategy on one problem did.
  * @param fitness the best fitness found (lower is better)
  * @param allocatedBytes bytes allocated by the thread that ran the search, or none if not measured
  * @param stoppedBy why it ended: finished, time, evals or solved
  * @param solved true if the optimal fitness was reached (only looked for on constraint problems)
  */
case class RunResult(problem: String, strategy: OptimizationStrategyType, seed: Long, evaluations: Long,
                     millis: Double, fitness: Double, allocatedBytes: Option[Long], stoppedBy: String,
                     solved: Boolean = false) {
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
    else Some(search(problem, optimizee, initialGuess, strategy, seed, config))
  }

  private def search(benchmarkProblem: BenchmarkProblem, problem: com.barrybecker4.optimization.optimizee.optimizees.OptimizeeProblem,
                     initialGuess: com.barrybecker4.optimization.parameter.ParameterArray,
                     strategyType: OptimizationStrategyType, seed: Long, config: BenchmarkConfig): RunResult = {
    val start = System.nanoTime()
    val limited = new LimitedOptimizee(problem, config.maxEvaluations, start + config.timeLimitMillis * NANOS_PER_MILLI,
      stopWhenSolved = benchmarkProblem.isConstraint)
    val strategy = strategyType.getStrategy(limited, problem.getFitnessRange, new Random(seed))
    val allocatedBefore = AllocationMeter.bytesAllocatedSoFar
    val fitness = bestFitness(strategy, initialGuess, problem.getFitnessRange)
    val millis = (System.nanoTime() - start) / NANOS_PER_MILLI.toDouble
    val allocated = for (before <- allocatedBefore; after <- AllocationMeter.bytesAllocatedSoFar) yield after - before
    RunResult(benchmarkProblem.name, strategyType, seed, limited.evaluations, millis, fitness,
      allocated.filter(_ => !MULTI_THREADED.contains(strategyType)), limited.stoppedBy,
      solved = benchmarkProblem.isConstraint && (limited.reachedOptimal || fitness <= problem.getOptimalFitness))
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
