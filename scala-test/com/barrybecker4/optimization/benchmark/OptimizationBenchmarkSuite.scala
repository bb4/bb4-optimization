// Copyright by Barry G. Becker, 2026. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.benchmark

import com.barrybecker4.optimization.strategy.OptimizationStrategyType.*
import org.scalatest.funsuite.AnyFunSuite

/** Short runs only, to check that the benchmark works. The real numbers come from running the benchmark itself. */
class OptimizationBenchmarkSuite extends AnyFunSuite {

  private val quick = BenchmarkConfig(timeLimitMillis = 150, maxEvaluations = 200_000, numSeeds = 2,
    problemNames = Seq("tour-50"), strategies = Seq(SIMULATED_ANNEALING, GENETIC_SEARCH), outputFile = None, reportFile = None)

  test("every problem is a different name") {
    assertResult(BenchmarkProblems.ALL.size)(BenchmarkProblems.ALL.map(_.name).distinct.size)
  }

  test("there is a result for each seed of each strategy, and they did some work") {
    val results = OptimizationBenchmark.runAll(quick)
    assertResult(4)(results.size)
    results.foreach(r => assert(r.evaluations > 0 && r.millis > 0 && !r.fitness.isNaN, r.toString))
  }

  test("a search ends by the time limit or by finishing, and allocation is measured when it has one thread") {
    val results = OptimizationBenchmark.runAll(quick)
    results.foreach(r => assert(Set("time", "finished", "evals").contains(r.stoppedBy)))
    assert(results.filter(_.strategy == SIMULATED_ANNEALING).forall(_.allocatedBytes.exists(_ > 0)))
  }

  test("a search finds a shorter tour than the one it started with") {
    val problem = BenchmarkProblems.named(Seq("tour-50")).head.create(1)
    val start = problem.evaluateFitness(problem.getInitialGuess)
    val results = OptimizationBenchmark.runAll(quick.copy(strategies = Seq(SIMULATED_ANNEALING)))
    results.foreach(r => assert(r.fitness < start, s"${r.fitness} is not below $start"))
  }

  test("a strategy that cannot be used on a problem is left out") {
    val results = OptimizationBenchmark.runAll(quick.copy(problemNames = Seq("tour-50"), strategies = Seq(DIFFERENTIAL_EVOLUTION)))
    assert(results.isEmpty)
  }

  test("the same seed gives the same starting point, so a shorter time limit never starts from somewhere else") {
    val a = BenchmarkProblems.named(Seq("tour-50")).head.create(3)
    val b = BenchmarkProblems.named(Seq("tour-50")).head.create(3)
    assertResult(a.evaluateFitness(a.getInitialGuess))(b.evaluateFitness(b.getInitialGuess))
  }

  test("the constraint problems have their own names, are marked, and are not in the ordinary list") {
    assertResult(BenchmarkProblems.CONSTRAINT.size)(BenchmarkProblems.CONSTRAINT.map(_.name).distinct.size)
    assert(BenchmarkProblems.CONSTRAINT.forall(_.isConstraint) && BenchmarkProblems.ALL.forall(!_.isConstraint))
    assert(BenchmarkProblems.CONSTRAINT.forall(p => !BenchmarkProblems.ALL.exists(_.name == p.name)))
  }

  test("every constraint problem has a known solution that has the optimal fitness") {
    BenchmarkProblems.CONSTRAINT.foreach { p =>
      val problem = p.create(1)
      assertResult(problem.getOptimalFitness, p.name)(problem.getExactSolution.fitness)
    }
  }

  test("a run on a constraint problem that finds a solution is solved and stops there") {
    val config = quick.copy(problemNames = Seq("queens-100"), strategies = Seq(SIMULATED_ANNEALING), timeLimitMillis = 3000,
      numSeeds = 3, solveRate = true)
    val results = OptimizationBenchmark.runAll(config)
    assertResult(3)(results.size)
    results.filter(_.solved).foreach(r => assert(r.stoppedBy == LimitedOptimizee.SOLVED || r.fitness == 0.0, r.toString))
  }

  test("an ordinary problem is never marked solved") {
    assert(OptimizationBenchmark.runAll(quick).forall(!_.solved))
  }
}
