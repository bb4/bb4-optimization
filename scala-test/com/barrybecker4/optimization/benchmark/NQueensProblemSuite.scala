// Copyright by Barry G. Becker, 2026. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.benchmark

import com.barrybecker4.optimization.parameter.{ParameterArray, Swap}
import org.scalatest.funsuite.AnyFunSuite

class NQueensProblemSuite extends AnyFunSuite {

  private def arrayOf(values: Seq[Int]) = com.barrybecker4.optimization.parameter.PermutedParameterArray(
    values.indices.map(i => new com.barrybecker4.optimization.parameter.types.IntegerParameter(values(i), 0, values.size - 1, "p" + i)),
    new scala.util.Random(1))

  private val validSizes = Seq(4, 5, 6, 7, 10, 11, 12, 13, 100, 400)

  test("the known solution has no queens attacking each other, for every size that has one") {
    validSizes.foreach { size =>
      assertResult(0.0, s"$size queens")(new NQueensProblem(size, 1).getExactSolution.fitness)
    }
  }

  test("the sizes without that solution are refused") {
    Seq(2, 3, 8, 9 + 5, 3 * 6 + 2, 3 * 6 + 3).filter(n => n % 6 == 2 || n % 6 == 3 || n < 4).foreach { size =>
      intercept[IllegalArgumentException](new NQueensProblem(size, 1))
    }
  }

  test("the initial guess puts one queen in each column, and a random board has queens attacking") {
    val problem = new NQueensProblem(30, 2)
    val guess = problem.getInitialGuess
    assertResult((0 until 30).toList)((0 until 30).map(guess.get(_).getValue.toInt).sorted.toList)
    assert(problem.evaluateFitness(guess) > 0)
  }

  test("the same seed gives the same initial guess, and another seed another one") {
    def values(seed: Long) = { val g = new NQueensProblem(30, seed).getInitialGuess; (0 until 30).map(g.get(_).getValue) }
    assertResult(values(4))(values(4))
    assert(values(4) != values(5))
  }

  test("queens on the same diagonal are a conflict for each pair") {
    val problem = new NQueensProblem(4, 1)
    val onePair = arrayOf(List(0, 1, 3, 2))
    assertResult(2.0)(problem.evaluateFitness(onePair)) // 0,1 and 3,2 are both on diagonals
    val allOnOneDiagonal = arrayOf(List(0, 1, 2, 3))
    assertResult(6.0)(problem.evaluateFitness(allOnOneDiagonal))
  }

  private def afterSwap(problem: NQueensProblem, board: ParameterArray, i: Int, j: Int): Double =
    problem.evaluateFitness(board.applyMove(Swap(i, j)))

  test("the conflicts after a swap from evaluateMove are those of the swapped board, for every pair") {
    val problem = new NQueensProblem(12, 3)
    val board = problem.getInitialGuess
    val start = problem.evaluateFitness(board)
    for (i <- 0 until 12; j <- 0 until 12)
      assertResult(afterSwap(problem, board, i, j), s"swap $i $j")(problem.evaluateMove(board, start, Swap(i, j)))
  }

  test("evaluateMove stays right over a long walk of swaps") {
    val problem = new NQueensProblem(22, 4)
    var board = problem.getInitialGuess
    var conflicts = problem.evaluateFitness(board)
    val random = new scala.util.Random(1)
    (1 to 300).foreach { _ =>
      val move = Swap(random.nextInt(22), random.nextInt(22))
      conflicts = problem.evaluateMove(board, conflicts, move)
      board = board.applyMove(move)
      assertResult(problem.evaluateFitness(board))(conflicts)
    }
  }
}
