// Copyright by Barry G. Becker, 2026. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.benchmark

import com.barrybecker4.optimization.strategy.OptimizationStrategyType.*
import org.scalatest.funsuite.AnyFunSuite

class BenchmarkReportSuite extends AnyFunSuite {

  private def result(problem: String, strategy: com.barrybecker4.optimization.strategy.OptimizationStrategyType,
                     evaluations: Long, millis: Double, fitness: Double, bytes: Option[Long] = Some(1000L),
                     stoppedBy: String = LimitedOptimizee.TIME_LIMIT) =
    RunResult(problem, strategy, 1, evaluations, millis, fitness, bytes, stoppedBy)

  test("the median of an odd number of values is the middle one") {
    assertResult(3.0)(BenchmarkReport.median(Seq(5.0, 1.0, 3.0)))
  }

  test("the median of an even number of values is the mean of the middle two") {
    assertResult(2.5)(BenchmarkReport.median(Seq(4.0, 1.0, 2.0, 3.0)))
  }

  test("the median of nothing is not a number") {
    assert(BenchmarkReport.median(Seq.empty).isNaN)
  }

  test("evaluations per second and bytes per evaluation of a run") {
    val run = result("p", SIMULATED_ANNEALING, evaluations = 500, millis = 250.0, fitness = 1.0, bytes = Some(5000L))
    assertResult(2000.0)(run.evaluationsPerSecond)
    assertResult(Some(10.0))(run.bytesPerEvaluation)
  }

  test("a run with no allocation measurement has no bytes per evaluation") {
    assertResult(None)(result("p", GENETIC_SEARCH, 10, 1.0, 1.0, bytes = None).bytesPerEvaluation)
  }

  test("a summary has one row for each problem and strategy, in the order they first appear") {
    val results = Seq(
      result("b", SIMULATED_ANNEALING, 100, 10.0, 5.0), result("a", GENETIC_SEARCH, 100, 10.0, 5.0),
      result("b", SIMULATED_ANNEALING, 300, 10.0, 3.0), result("b", GENETIC_SEARCH, 100, 10.0, 5.0))
    val summaries = BenchmarkReport.summarize(results)
    assertResult(Seq(("b", "SIMULATED_ANNEALING"), ("a", "GENETIC_SEARCH"), ("b", "GENETIC_SEARCH")))(
      summaries.map(s => (s.problem, s.strategy)))
    assertResult(2)(summaries.head.runs)
  }

  test("a summary has the median and the best fitness") {
    val runs = Seq(1.0, 9.0, 4.0).map(f => result("p", HILL_CLIMBING, 100, 10.0, f))
    val summary = BenchmarkReport.summarize(runs).head
    assertResult((4.0, 1.0))((summary.medianFitness, summary.bestFitness))
  }

  test("the reason for stopping is shown only if every run agrees") {
    val same = Seq(result("p", HILL_CLIMBING, 1, 1.0, 1.0), result("p", HILL_CLIMBING, 1, 1.0, 1.0))
    val mixed = same :+ result("p", HILL_CLIMBING, 1, 1.0, 1.0, stoppedBy = LimitedOptimizee.FINISHED)
    assertResult("time")(BenchmarkReport.summarize(same).head.stoppedBy)
    assertResult("mixed")(BenchmarkReport.summarize(mixed).head.stoppedBy)
  }

  test("the markdown table has a header and a row for every summary") {
    val table = BenchmarkReport.markdown(BenchmarkReport.summarize(Seq(result("tour-50", ITERATED_LOCAL_SEARCH, 1000, 10.0, 7.5))))
    val lines = table.split("\n")
    assertResult(3)(lines.length)
    assert(lines(0).contains("Evals/sec") && lines(2).contains("tour-50") && lines(2).contains("ITERATED_LOCAL_SEARCH"))
  }

  test("tab separated output has a line for every run and a header") {
    val text = BenchmarkReport.tabSeparated(Seq(result("p", HILL_CLIMBING, 100, 10.0, 2.0), result("p", HILL_CLIMBING, 200, 10.0, 1.0)))
    val lines = text.split("\n")
    assertResult(3)(lines.length)
    assertResult(lines(0).split("\t").length)(lines(1).split("\t").length)
  }

  test("the document has the settings and the table, and nothing that changes from one run to the next but the numbers") {
    val summaries = BenchmarkReport.summarize(Seq(result("p", SIMULATED_ANNEALING, 100, 10.0, 5.0)))
    val document = BenchmarkReport.document("Java 21, 2 processors", summaries)
    assert(document.startsWith("# Benchmark results"))
    assert(document.contains("Java 21, 2 processors"))
    assert(document.contains(BenchmarkReport.markdown(summaries)))
    assertResult(document)(BenchmarkReport.document("Java 21, 2 processors", summaries))
  }
}
