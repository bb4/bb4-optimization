// Copyright by Barry G. Becker, 2026. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.benchmark

import java.nio.file.{Files, Paths}

/**
  * Measures how fast each strategy works on permutation and other discrete problems, and how good a solution it finds
  * in a fixed time. It is for comparing versions of this library on the same machine: record the output before a
  * change and again after.
  *
  * For every problem and strategy it reports, as the median of several seeds, the evaluations per second, the
  * bytes allocated per evaluation (single threaded strategies only), and the fitness reached when the time or
  * evaluation limit ended the search.
  *
  * Run it with `./gradlew benchmark`. Options are given as `-Pargs="--time-ms=2000 --seeds=3 --problems=tour-50"`.
  * See [[BenchmarkConfig]] for the options. Each problem and strategy is run once first, and not counted, to warm up the JIT.
  * @author Barry Becker
  */
object OptimizationBenchmark {

  private val WARM_UP_SEED = 0L
  private val WARM_UP_MILLIS = 300L

  def main(args: Array[String]): Unit = {
    val config = BenchmarkConfig.parse(args.toSeq)
    val results = runAll(config)
    println(machineDescription(config))
    println(BenchmarkReport.markdown(BenchmarkReport.summarize(results)))
    config.outputFile.foreach(file => Files.writeString(Paths.get(file), BenchmarkReport.tabSeparated(results)))
  }

  def runAll(config: BenchmarkConfig): Seq[RunResult] =
    for {
      problem <- BenchmarkProblems.named(config.problemNames)
      strategy <- config.strategies
      result <- runStrategy(problem, strategy, config)
    } yield result

  private def runStrategy(problem: BenchmarkProblem, strategy: com.barrybecker4.optimization.strategy.OptimizationStrategyType,
                          config: BenchmarkConfig): Seq[RunResult] = {
    BenchmarkRun.run(problem, strategy, WARM_UP_SEED, config.copy(timeLimitMillis = WARM_UP_MILLIS))
    (1 to config.numSeeds).flatMap(seed => BenchmarkRun.run(problem, strategy, seed.toLong, config))
  }

  def machineDescription(config: BenchmarkConfig): String =
    s"Java ${System.getProperty("java.version")}, ${Runtime.getRuntime.availableProcessors} processors, " +
      s"${config.numSeeds} seeds, ${config.timeLimitMillis} ms or ${config.maxEvaluations} evaluations per run\n"
}
