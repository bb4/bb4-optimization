// Copyright by Barry G. Becker, 2026. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.benchmark

/** The results of all the runs of one strategy on one problem, summarized. */
case class Summary(problem: String, strategy: String, runs: Int, evaluationsPerSecond: Double,
                   bytesPerEvaluation: Option[Double], medianFitness: Double, bestFitness: Double,
                   medianEvaluations: Double, stoppedBy: String)

/** Turns the results of runs into tables. */
object BenchmarkReport {

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

  private def bytes(value: Option[Double]): String = value.map(v => f"$v%,.0f").getOrElse("n/a")

  /** One line for every run, so results can be compared with a spreadsheet or a script. */
  def tabSeparated(results: Seq[RunResult]): String = {
    val header = Seq("problem", "strategy", "seed", "evaluations", "millis", "fitness", "allocatedBytes", "stoppedBy")
    val rows = results.map { r =>
      Seq(r.problem, r.strategy, r.seed, r.evaluations, f"${r.millis}%.1f", r.fitness,
        r.allocatedBytes.getOrElse(""), r.stoppedBy).mkString("\t")
    }
    (header.mkString("\t") +: rows).mkString("\n")
  }
}
