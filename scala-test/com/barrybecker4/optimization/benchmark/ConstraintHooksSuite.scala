// Copyright by Barry G. Becker, 2026. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.benchmark

import com.barrybecker4.optimization.optimizee.DeltaCheckingOptimizee
import com.barrybecker4.optimization.parameter.{ParameterArray, SetValue}
import org.scalatest.funsuite.AnyFunSuite

import scala.util.Random

/** The constraint problems tell which positions are in conflict, and score a change of one position by its delta. */
class ConstraintHooksSuite extends AnyFunSuite {

  private def valuesOf(pa: ParameterArray): Seq[Int] = (0 until pa.size).map(i => pa.get(i).getValue.toInt)

  test("the conflicted queens are exactly the ones that another queen attacks") {
    val problem = new NQueensProblem(12, 3)
    val board = problem.getInitialGuess
    val columns = valuesOf(board)
    val attacked = (0 until 12).filter(r => (0 until 12).exists(o => o != r && Math.abs(o - r) == Math.abs(columns(o) - columns(r))))
    assertResult(attacked)(problem.conflictedPositions(board))
    assert(attacked.nonEmpty)
  }

  test("a solved board has no conflicted queens") {
    assertResult(Seq.empty)(new NQueensProblem(10, 1).conflictedPositions(new NQueensProblem(10, 1).getExactSolution.pa))
  }

  test("the conflicted nodes are the ones with a neighbor of their color, and there are none in the solution") {
    val problem = new GraphColoringProblem(40, 3, 5.0, 1)
    val guess = problem.getInitialGuess
    val conflicted = problem.conflictedPositions(guess)
    assert(conflicted.nonEmpty && conflicted.size < 40)
    assertResult(Seq.empty)(problem.conflictedPositions(problem.getExactSolution.pa))
    assertResult(0.0)(problem.evaluateFitness(problem.getExactSolution.pa))
  }

  test("a node with an edge that clashes is reported, and recoloring all the reported nodes can fix the clash count to zero") {
    val problem = new GraphColoringProblem(30, 3, 4.0, 2)
    val guess = problem.getInitialGuess
    assert(problem.evaluateFitness(guess) > 0 && problem.conflictedPositions(guess).nonEmpty)
  }

  test("the clashes after recoloring a node from evaluateMove are those of the recolored graph, for every node and color") {
    val problem = new GraphColoringProblem(30, 4, 5.0, 3)
    val coloring = problem.getInitialGuess
    val clashes = problem.evaluateFitness(coloring)
    for (node <- 0 until 30; color <- 0 until 4) {
      val move = SetValue(node, color)
      assertResult(problem.evaluateFitness(coloring.applyMove(move)), s"node $node color $color")(problem.evaluateMove(coloring, clashes, move))
    }
  }

  test("a delta checking wrapper accepts every recoloring that coloring scores by delta") {
    val problem = new DeltaCheckingOptimizee(new GraphColoringProblem(30, 4, 5.0, 4))
    val coloring = new GraphColoringProblem(30, 4, 5.0, 4).getInitialGuess
    val start = problem.evaluateFitness(coloring)
    val random = new Random(1)
    (1 to 100).foreach(_ => problem.evaluateMove(coloring, start, coloring.proposeLocalMove(random)))
  }

  test("the conflicted positions of the all-interval series are the ends of repeated differences") {
    val problem = new AllIntervalProblem(8, 1)
    val series = problem.getInitialGuess
    val values = valuesOf(series)
    val differences = (1 until 8).map(i => Math.abs(values(i) - values(i - 1)))
    val expected = (1 until 8).filter(i => differences.count(_ == differences(i - 1)) > 1).flatMap(i => Seq(i - 1, i)).distinct
    assertResult(expected)(problem.conflictedPositions(series))
  }

  test("a solved series has no conflicted positions") {
    val problem = new AllIntervalProblem(14, 1)
    assertResult(Seq.empty)(problem.conflictedPositions(problem.getExactSolution.pa))
  }
}
