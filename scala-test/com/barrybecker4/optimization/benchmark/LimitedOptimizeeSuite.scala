// Copyright by Barry G. Becker, 2026. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.benchmark

import com.barrybecker4.optimization.optimizee.EvaluationBudgetExceededException
import org.scalatest.funsuite.AnyFunSuite

class LimitedOptimizeeSuite extends AnyFunSuite {

  private val problem = new RandomTourProblem(10, 1)
  private val tour = problem.getInitialGuess
  private val farFuture = System.nanoTime() + 3_600_000_000_000L

  test("evaluations are passed to the problem and counted") {
    val limited = new LimitedOptimizee(problem, 100, farFuture)
    assertResult(problem.evaluateFitness(tour))(limited.evaluateFitness(tour))
    limited.evaluateFitness(tour)
    assertResult(2)(limited.evaluations)
    assertResult(LimitedOptimizee.FINISHED)(limited.stoppedBy)
  }

  test("it stops with the budget exception once the evaluation limit is passed") {
    val limited = new LimitedOptimizee(problem, 3, farFuture)
    (1 to 3).foreach(_ => limited.evaluateFitness(tour))
    intercept[EvaluationBudgetExceededException](limited.evaluateFitness(tour))
    assertResult(LimitedOptimizee.EVALUATION_LIMIT)(limited.stoppedBy)
  }

  test("it stops with the budget exception once the deadline is passed") {
    val limited = new LimitedOptimizee(problem, 100, System.nanoTime() - 1)
    intercept[EvaluationBudgetExceededException](limited.evaluateFitness(tour))
    assertResult(LimitedOptimizee.TIME_LIMIT)(limited.stoppedBy)
  }

  test("it says the same as the problem about how to evaluate and what is optimal") {
    val limited = new LimitedOptimizee(problem, 100, farFuture)
    assertResult((problem.evaluateByComparison, problem.getOptimalFitness, problem.getName))(
      (limited.evaluateByComparison, limited.getOptimalFitness, limited.getName))
  }
}
