// Copyright by Barry G. Becker, 2026. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.optimizee

import com.barrybecker4.optimization.parameter.types.{IntegerParameter, Parameter}
import com.barrybecker4.optimization.parameter.{Move, ParameterArray, PermutedParameterArray, Swap}
import org.scalatest.funsuite.AnyFunSuite

import scala.util.Random

class OptimizeeMovesSuite extends AnyFunSuite {

  private def permutation(order: Int*): PermutedParameterArray =
    PermutedParameterArray(order.indices.map(i => IntegerParameter(order(i), 0, order.size - 1, "p" + i): Parameter), new Random(1))

  /** Fitness is how far each value is from its own position. */
  private class Displacement extends AbsoluteOptimizee {
    var fullEvaluations = 0
    override def getName = "displacement"
    override def evaluateFitness(params: ParameterArray): Double = {
      fullEvaluations += 1
      (0 until params.size).map(i => math.abs(params.get(i).getValue - i)).sum
    }
  }

  /** Works out the change of a swap without looking at the array, and can be told to get it wrong. */
  private class FastDisplacement(error: Double = 0.0) extends Displacement {
    var moveEvaluations = 0
    override def evaluateMove(current: ParameterArray, currentFitness: Double, move: Move): Double = move match {
      case Swap(i, j) =>
        moveEvaluations += 1
        val (vi, vj) = (current.get(i).getValue, current.get(j).getValue)
        def cost(v: Double, at: Int) = math.abs(v - at)
        currentFitness - cost(vi, i) - cost(vj, j) + cost(vj, i) + cost(vi, j) + error
      case _ => super.evaluateMove(current, currentFitness, move)
    }
  }

  test("by default the fitness after a move is a full evaluation of the array after the move") {
    val optimizee = new Displacement()
    val current = permutation(2, 0, 1, 3)
    val expected = optimizee.evaluateFitness(current.applyMove(Swap(0, 3)))
    assertResult(expected)(optimizee.evaluateMove(current, optimizee.evaluateFitness(current), Swap(0, 3)))
  }

  test("an optimizee can work out the fitness after a move without evaluating the array") {
    val optimizee = new FastDisplacement()
    val current = permutation(3, 1, 0, 2)
    val before = optimizee.evaluateFitness(current)
    optimizee.fullEvaluations = 0
    val after = optimizee.evaluateMove(current, before, Swap(0, 2))
    assertResult(0, "no full evaluation")(optimizee.fullEvaluations)
    assertResult(new Displacement().evaluateFitness(current.applyMove(Swap(0, 2))))(after)
  }

  test("a budget counts a move evaluation as one evaluation, and passes it to the optimizee") {
    val inner = new FastDisplacement()
    val budgeted = new BudgetedOptimizee(inner, maxEvaluations = 2)
    val current = permutation(1, 0, 3, 2)
    budgeted.evaluateMove(current, 4.0, Swap(0, 1))
    budgeted.evaluateMove(current, 4.0, Swap(2, 3))
    assertResult(2)(budgeted.evaluationsUsed)
    assertResult(2, "the optimizee's own delta was used")(inner.moveEvaluations)
    assertResult(0)(inner.fullEvaluations)
    intercept[EvaluationBudgetExceededException](budgeted.evaluateMove(current, 4.0, Swap(0, 3)))
    assert(budgeted.wasBudgetExhausted)
  }

  test("move evaluations and full evaluations share one budget") {
    val budgeted = new BudgetedOptimizee(new Displacement(), maxEvaluations = 2)
    val current = permutation(1, 0, 2)
    budgeted.evaluateFitness(current)
    budgeted.evaluateMove(current, 2.0, Swap(0, 1))
    intercept[EvaluationBudgetExceededException](budgeted.evaluateFitness(current))
  }

  test("the delta check lets a correct delta through") {
    val checked = new DeltaCheckingOptimizee(new FastDisplacement())
    val current = permutation(3, 1, 0, 2)
    val fitness = checked.evaluateFitness(current)
    assertResult(new Displacement().evaluateFitness(current.applyMove(Swap(1, 3))))(checked.evaluateMove(current, fitness, Swap(1, 3)))
  }

  test("the delta check stops a wrong delta and says what was wrong") {
    val checked = new DeltaCheckingOptimizee(new FastDisplacement(error = 0.5))
    val current = permutation(3, 1, 0, 2)
    val error = intercept[IllegalStateException](checked.evaluateMove(current, checked.evaluateFitness(current), Swap(1, 3)))
    assert(error.getMessage.contains("Swap(1,3)") && error.getMessage.contains("0.5"), error.getMessage)
  }

  test("the delta check stops a wrong fitness for the current array") {
    val checked = new DeltaCheckingOptimizee(new FastDisplacement())
    val current = permutation(3, 1, 0, 2)
    val error = intercept[IllegalStateException](checked.evaluateMove(current, 99.0, Swap(1, 3)))
    assert(error.getMessage.contains("current"), error.getMessage)
  }

  test("the delta check changes nothing else about the optimizee") {
    val inner = new FastDisplacement()
    val checked = new DeltaCheckingOptimizee(inner)
    assertResult(inner.getName)(checked.getName)
    assertResult(inner.evaluateByComparison)(checked.evaluateByComparison)
    assertResult(inner.getOptimalFitness)(checked.getOptimalFitness)
    val current = permutation(1, 0, 2)
    assertResult(inner.evaluateFitness(current))(checked.evaluateFitness(current))
  }
}
