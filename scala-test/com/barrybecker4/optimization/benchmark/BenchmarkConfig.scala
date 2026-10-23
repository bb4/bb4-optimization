// Copyright by Barry G. Becker, 2026. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.benchmark

import com.barrybecker4.optimization.strategy.OptimizationStrategyType
import OptimizationStrategyType.*

object BenchmarkConfig {
  val DEFAULT_TIME_LIMIT_MILLIS = 1000L
  val DEFAULT_MAX_EVALUATIONS = 5_000_000L
  val DEFAULT_NUM_SEEDS = 5

  /** The strategies that make sense on permutations and other discrete problems, and finish on their own. */
  val DEFAULT_STRATEGIES: Seq[OptimizationStrategyType] = Seq(
    SIMULATED_ANNEALING, GLOBAL_SIMULATED_ANNEALING, HILL_CLIMBING, GLOBAL_HILL_CLIMBING,
    GENETIC_SEARCH, CONCURRENT_GENETIC_SEARCH, ITERATED_LOCAL_SEARCH, VARIABLE_NEIGHBORHOOD_SEARCH)

  /**
    * Reads options of the form `--name=value`. The names are time-ms, max-evals, seeds, problems, strategies
    * (the last two are comma separated lists) and out (a file to write the tab separated results to).
    */
  def parse(args: Seq[String]): BenchmarkConfig = {
    val options = args.filter(_.startsWith("--")).map(_.drop(2).split("=", 2)).collect { case Array(k, v) => k -> v }.toMap
    BenchmarkConfig(
      timeLimitMillis = options.get("time-ms").map(_.toLong).getOrElse(DEFAULT_TIME_LIMIT_MILLIS),
      maxEvaluations = options.get("max-evals").map(_.toLong).getOrElse(DEFAULT_MAX_EVALUATIONS),
      numSeeds = options.get("seeds").map(_.toInt).getOrElse(DEFAULT_NUM_SEEDS),
      problemNames = options.get("problems").map(_.split(",").toSeq).getOrElse(Seq.empty),
      strategies = options.get("strategies").map(_.split(",").toSeq.map(OptimizationStrategyType.valueOf))
        .getOrElse(DEFAULT_STRATEGIES),
      outputFile = options.get("out"))
  }
}

/**
  * @param timeLimitMillis each run is stopped after this long
  * @param maxEvaluations each run is stopped after this many evaluations
  * @param numSeeds how many times to repeat each problem and strategy, with the seeds 1 to this number
  * @param problemNames the problems to run, or all of them if empty
  * @param strategies the strategies to run
  * @param outputFile where to write the results as tab separated values, if anywhere
  */
case class BenchmarkConfig(timeLimitMillis: Long, maxEvaluations: Long, numSeeds: Int,
                           problemNames: Seq[String], strategies: Seq[OptimizationStrategyType],
                           outputFile: Option[String])
