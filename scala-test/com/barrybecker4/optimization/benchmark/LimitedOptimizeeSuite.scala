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

  private val queens = new NQueensProblem(10, 1)

  test("it carries on after the problem is solved unless it is asked to stop") {
    val limited = new LimitedOptimizee(queens, 100, farFuture)
    limited.evaluateFitness(queens.getExactSolution.pa)
    limited.evaluateFitness(queens.getInitialGuess)
    assert(limited.reachedOptimal)
    assertResult(LimitedOptimizee.FINISHED)(limited.stoppedBy)
  }

  test("when asked to, it stops at the evaluation after the one that solved the problem") {
    val limited = new LimitedOptimizee(queens, 100, farFuture, stopWhenSolved = true)
    limited.evaluateFitness(queens.getInitialGuess)
    assert(!limited.reachedOptimal)
    limited.evaluateFitness(queens.getExactSolution.pa)
    assert(limited.reachedOptimal)
    intercept[EvaluationBudgetExceededException](limited.evaluateFitness(queens.getInitialGuess))
    assertResult(LimitedOptimizee.SOLVED)(limited.stoppedBy)
  }

  test("a move that reaches the optimal fitness counts as solving it") {
    val limited = new LimitedOptimizee(queens, 100, farFuture)
    val almost = queens.getExactSolution.pa.applyMove(com.barrybecker4.optimization.parameter.Swap(0, 1))
    val broken = queens.evaluateFitness(almost)
    assert(broken > 0)
    limited.evaluateMove(almost, broken, com.barrybecker4.optimization.parameter.Swap(0, 1))
    assert(limited.reachedOptimal)
  }
}
