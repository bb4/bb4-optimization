// Copyright by Barry G. Becker, 2026. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.benchmark

import com.barrybecker4.optimization.strategy.OptimizationStrategyType
import OptimizationStrategyType.*

object BenchmarkConfig {
  val DEFAULT_TIME_LIMIT_MILLIS = 5000L
  val DEFAULT_MAX_EVALUATIONS = 5_000_000L
  val DEFAULT_NUM_SEEDS = 5

  /** Where a full run writes its summary, so that git shows what changed since the last run. */
  val DEFAULT_REPORT_FILE = "docs/benchmark-results.md"
  private val NO_REPORT = "none"

  /** The solve rate run is separate, with more seeds and a shorter limit, since a failed run takes the whole limit. */
  val DEFAULT_SOLVE_RATE_REPORT_FILE = "docs/solve-rate-results.md"
  val DEFAULT_SOLVE_RATE_SEEDS = 20
  val DEFAULT_SOLVE_RATE_TIME_LIMIT_MILLIS = 2000L

  /** The strategies that make sense on permutations and other discrete problems, and finish on their own. */
  val DEFAULT_STRATEGIES: Seq[OptimizationStrategyType] = Seq(
    SIMULATED_ANNEALING, GLOBAL_SIMULATED_ANNEALING, HILL_CLIMBING, GLOBAL_HILL_CLIMBING,
    GENETIC_SEARCH, CONCURRENT_GENETIC_SEARCH, ITERATED_LOCAL_SEARCH, VARIABLE_NEIGHBORHOOD_SEARCH)

  /**
    * Reads options of the form `--name=value`. The names are time-ms, max-evals, seeds, problems, strategies
    * (the last two are comma separated lists), report (the Markdown summary file, or `none`) and out (a file to
    * write the tab separated results of every run to).
    *
    * The option `solve-rate=true` runs the constraint problems instead (see [[BenchmarkProblems.CONSTRAINT]]), with
    * 20 seeds and 2 seconds as the defaults, and reports how many runs solved each, and how fast. Its full run writes
    * [[DEFAULT_SOLVE_RATE_REPORT_FILE]].
    *
    * A full run (all problems and the default strategies) writes the summary to [[DEFAULT_REPORT_FILE]]. A run
    * restricted to some problems or strategies writes no summary unless `report` is given, so it cannot replace
    * the full one with a partial one.
    */
  def parse(args: Seq[String]): BenchmarkConfig = {
    val options = args.filter(_.startsWith("--")).map(_.drop(2).split("=", 2)).collect { case Array(k, v) => k -> v }.toMap
    val solveRate = options.get("solve-rate").contains("true")
    val problemNames = options.get("problems").map(_.split(",").toSeq).getOrElse(Seq.empty)
    val strategies = options.get("strategies").map(_.split(",").toSeq.map(OptimizationStrategyType.valueOf))
      .getOrElse(DEFAULT_STRATEGIES)
    val isFullRun = problemNames.isEmpty && strategies == DEFAULT_STRATEGIES
    BenchmarkConfig(
      timeLimitMillis = options.get("time-ms").map(_.toLong).getOrElse(defaultTimeLimit(solveRate)),
      maxEvaluations = options.get("max-evals").map(_.toLong).getOrElse(DEFAULT_MAX_EVALUATIONS),
      numSeeds = options.get("seeds").map(_.toInt).getOrElse(defaultSeeds(solveRate)),
      problemNames = if (solveRate && problemNames.isEmpty) BenchmarkProblems.CONSTRAINT.map(_.name) else problemNames,
      strategies = strategies,
      outputFile = options.get("out"),
      reportFile = reportFile(options.get("report"), isFullRun, solveRate),
      solveRate = solveRate)
  }

  private def defaultTimeLimit(solveRate: Boolean): Long =
    if (solveRate) DEFAULT_SOLVE_RATE_TIME_LIMIT_MILLIS else DEFAULT_TIME_LIMIT_MILLIS

  private def defaultSeeds(solveRate: Boolean): Int = if (solveRate) DEFAULT_SOLVE_RATE_SEEDS else DEFAULT_NUM_SEEDS

  private def reportFile(requested: Option[String], isFullRun: Boolean, solveRate: Boolean): Option[String] = requested match {
    case Some(NO_REPORT) => None
    case Some(file) => Some(file)
    case None => Option.when(isFullRun)(if (solveRate) DEFAULT_SOLVE_RATE_REPORT_FILE else DEFAULT_REPORT_FILE)
  }
}

/**
  * @param timeLimitMillis each run is stopped after this long
  * @param maxEvaluations each run is stopped after this many evaluations
  * @param numSeeds how many times to repeat each problem and strategy, with the seeds 1 to this number
  * @param problemNames the problems to run, or all of them if empty
  * @param strategies the strategies to run
  * @param outputFile where to write the results of every run as tab separated values, if anywhere
  * @param reportFile where to write the summary as a Markdown document, if anywhere
  * @param solveRate true to report how many runs reached the optimal fitness, for the constraint problems
  */
case class BenchmarkConfig(timeLimitMillis: Long, maxEvaluations: Long, numSeeds: Int,
                           problemNames: Seq[String], strategies: Seq[OptimizationStrategyType],
                           outputFile: Option[String], reportFile: Option[String], solveRate: Boolean = false)
