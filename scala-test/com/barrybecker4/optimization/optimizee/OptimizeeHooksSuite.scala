// Copyright by Barry G. Becker, 2026. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.optimizee

import com.barrybecker4.optimization.benchmark.{LimitedOptimizee, RandomTourProblem}
import com.barrybecker4.optimization.optimizee.optimizees.problems.TravelingSalesmanProblem
import com.barrybecker4.optimization.parameter.{Move, ParameterArray, PermutedParameterArray, Swap}
import org.scalatest.funsuite.AnyFunSuite

import scala.util.Random

/** The hooks a problem can use to say something about its own structure. By default they say nothing. */
class OptimizeeHooksSuite extends AnyFunSuite {

  /** Repairs by putting city 0 first, and always suggests swapping the first two positions. */
  private class Opinionated extends RandomTourProblem(8, 1) {
    var repairs = 0
    var guided = 0
    var conflicts = 0
    override def conflictedPositions(current: ParameterArray): Seq[Int] = { conflicts += 1; Seq(3, 4) }
    override def repair(candidate: ParameterArray): ParameterArray = { repairs += 1; candidate }
    override def proposeGuidedMove(current: ParameterArray, radius: Double, rnd: Random): Option[Move] = {
      guided += 1
      Some(Swap(0, 1))
    }
  }

  private val tour = new RandomTourProblem(8, 1).getInitialGuess

  // a problem that does not override either hook
  private val plain = new TravelingSalesmanProblem()
  private val plainTour = plain.getInitialGuess

  test("by default a candidate needs no repair, and the same array comes back") {
    assert(plain.repair(plainTour) eq plainTour)
  }

  test("by default a problem has no guided move to suggest") {
    assertResult(None)(plain.proposeGuidedMove(plainTour, 0.5, new Random(1)))
  }

  test("a budget passes repair and guided moves to the optimizee it wraps, and they do not use up the budget") {
    val inner = new Opinionated()
    val budgeted = new BudgetedOptimizee(inner, maxEvaluations = 1)
    budgeted.repair(tour)
    assertResult(Some(Swap(0, 1)))(budgeted.proposeGuidedMove(tour, 0.5, new Random(1)))
    assertResult((1, 1))((inner.repairs, inner.guided))
    assertResult(0)(budgeted.evaluationsUsed)
  }

  test("the benchmark limit passes them on too") {
    val inner = new Opinionated()
    val limited = new LimitedOptimizee(inner, maxEvaluations = 1, deadlineNanos = Long.MaxValue)
    limited.repair(tour)
    limited.proposeGuidedMove(tour, 0.5, new Random(1))
    assertResult((1, 1))((inner.repairs, inner.guided))
    assertResult(0L)(limited.evaluations)
  }

  test("the delta check passes them on too") {
    val inner = new Opinionated()
    val checked = new DeltaCheckingOptimizee(inner)
    checked.repair(tour)
    checked.proposeGuidedMove(tour, 0.5, new Random(1))
    assertResult((1, 1))((inner.repairs, inner.guided))
  }

  test("by default no position is reported as conflicted, which means the problem does not know") {
    assertResult(Seq.empty)(plain.conflictedPositions(plainTour))
  }

  test("the wrappers pass the conflicted positions on, without counting an evaluation") {
    val budgeted = new BudgetedOptimizee(new Opinionated(), maxEvaluations = 1)
    assertResult(Seq(3, 4))(budgeted.conflictedPositions(tour))
    assertResult(0)(budgeted.evaluationsUsed)
    assertResult(Seq(3, 4))(new LimitedOptimizee(new Opinionated(), 1, Long.MaxValue).conflictedPositions(tour))
    assertResult(Seq(3, 4))(new DeltaCheckingOptimizee(new Opinionated()).conflictedPositions(tour))
  }
}
