// Copyright by Barry G. Becker, 2026. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.benchmark

/** The results of all the runs of one strategy on one problem, summarized. */
case class Summary(problem: String, strategy: String, runs: Int, evaluationsPerSecond: Double,
                   bytesPerEvaluation: Option[Double], medianFitness: Double, bestFitness: Double,
                   medianEvaluations: Double, stoppedBy: String)

/**
  * How often one strategy solved one constraint problem.
  * @param solved the number of runs that reached the optimal fitness
  * @param medianMillis the median time of the runs that solved it, or NaN if none did
  * @param medianEvaluations the median number of evaluations of the runs that solved it, or NaN if none did
  * @param medianUnsolvedFitness the median best fitness of the runs that did not, or NaN if all did
  */
case class SolveSummary(problem: String, strategy: String, runs: Int, solved: Int, medianMillis: Double,
                        medianEvaluations: Double, medianUnsolvedFitness: Double)

/** Turns the results of runs into tables. */
object BenchmarkReport {

  private val DOCUMENT_TITLE = "# Benchmark results"
  private val DOCUMENT_INTRO =
    "Written by `./gradlew benchmark`; see [improvement-plan.md](improvement-plan.md). Use `git diff` to see what\n" +
      "a change did, and only compare runs made on the same machine."

  def median(values: Seq[Double]): Double = {
    val sorted = values.sorted
    if (sorted.isEmpty) Double.NaN
    else if (sorted.size % 2 == 1) sorted(sorted.size / 2)
    else (sorted(sorted.size / 2 - 1) + sorted(sorted.size / 2)) / 2.0
  }

  /** One summary for each problem and strategy, in the order they first appear. */
  def summarize(results: Seq[RunResult]): Seq[Summary] = {
    val groups = results.groupBy(r => (r.problem, r.strategy.toString))
    results.map(r => (r.problem, r.strategy.toString)).distinct.map { key =>
      val runs = groups(key)
      Summary(key._1, key._2, runs.size, median(runs.map(_.evaluationsPerSecond)),
        bytesPerEvaluation(runs), median(runs.map(_.fitness)), runs.map(_.fitness).min,
        median(runs.map(_.evaluations.toDouble)), stoppedBy(runs))
    }
  }

  /** One summary for each problem and strategy, in the order they first appear. */
  def summarizeSolves(results: Seq[RunResult]): Seq[SolveSummary] = {
    val groups = results.groupBy(r => (r.problem, r.strategy.toString))
    results.map(r => (r.problem, r.strategy.toString)).distinct.map { key =>
      val (solved, unsolved) = groups(key).partition(_.solved)
      SolveSummary(key._1, key._2, solved.size + unsolved.size, solved.size, median(solved.map(_.millis)),
        median(solved.map(_.evaluations.toDouble)), median(unsolved.map(_.fitness)))
    }
  }

  private def bytesPerEvaluation(runs: Seq[RunResult]): Option[Double] = {
    val values = runs.flatMap(_.bytesPerEvaluation)
    Option.when(values.nonEmpty)(median(values))
  }

  /** @return the reason the runs stopped if it was the same for all of them, otherwise a mix. */
  private def stoppedBy(runs: Seq[RunResult]): String = runs.map(_.stoppedBy).distinct match {
    case Seq(single) => single
    case _ => "mixed"
  }

  def markdown(summaries: Seq[Summary]): String = {
    val header = "| Problem | Strategy | Evals/sec | Bytes/eval | Median fitness | Best fitness | Median evals | Stopped by |\n" +
      "| --- | --- | ---: | ---: | ---: | ---: | ---: | --- |"
    val rows = summaries.map { s =>
      f"| ${s.problem} | ${s.strategy} | ${s.evaluationsPerSecond}%,.0f | ${bytes(s.bytesPerEvaluation)} | " +
        f"${s.medianFitness}%.4f | ${s.bestFitness}%.4f | ${s.medianEvaluations}%,.0f | ${s.stoppedBy} |"
    }
    (header +: rows).mkString("\n")
  }

  /**
    * The summary as a document to keep in git. It has no date, so that a run only shows up as a change in the numbers.
    * @param settings a description of the machine and limits, see [[OptimizationBenchmark.machineDescription]]
    */
  def document(settings: String, summaries: Seq[Summary]): String =
    Seq(DOCUMENT_TITLE, DOCUMENT_INTRO, settings, markdown(summaries)).mkString("\n\n") + "\n"

  private val SOLVE_TITLE = "# Solve rate results"
  private val SOLVE_INTRO =
    "Written by `./gradlew benchmark -Pargs=\"--solve-rate=true\"`; see [new-strategies-plan.md](new-strategies-plan.md).\n" +
      "Each run stops when it solves the problem, so the time is the time to a solution. The unsolved fitness is the\n" +
      "median number of constraints still broken in the runs that did not. Only compare runs made on the same machine."

  /** The solve rates as a document to keep in git, with no date, like [[document]]. */
  def solveDocument(settings: String, summaries: Seq[SolveSummary]): String =
    Seq(SOLVE_TITLE, SOLVE_INTRO, settings, solveMarkdown(summaries)).mkString("\n\n") + "\n"

  def solveMarkdown(summaries: Seq[SolveSummary]): String = {
    val header = "| Problem | Strategy | Solved | Median ms (solved) | Median evals (solved) | Median broken (unsolved) |\n" +
      "| --- | --- | ---: | ---: | ---: | ---: |"
    val rows = summaries.map { s =>
      s"| ${s.problem} | ${s.strategy} | ${s.solved}/${s.runs} | ${number(s.medianMillis, "%,.0f")} | " +
        s"${number(s.medianEvaluations, "%,.0f")} | ${number(s.medianUnsolvedFitness, "%.1f")} |"
    }
    (header +: rows).mkString("\n")
  }

  private def number(value: Double, format: String): String = if (value.isNaN) "-" else format.format(value)

  private def bytes(value: Option[Double]): String = value.map(v => f"$v%,.0f").getOrElse("n/a")

  /** One line for every run, so results can be compared with a spreadsheet or a script. */
  def tabSeparated(results: Seq[RunResult]): String = {
    val header = Seq("problem", "strategy", "seed", "evaluations", "millis", "fitness", "allocatedBytes", "stoppedBy", "solved")
    val rows = results.map { r =>
      Seq(r.problem, r.strategy, r.seed, r.evaluations, f"${r.millis}%.1f", r.fitness,
        r.allocatedBytes.getOrElse(""), r.stoppedBy, r.solved).mkString("\t")
    }
    (header.mkString("\t") +: rows).mkString("\n")
  }
}
