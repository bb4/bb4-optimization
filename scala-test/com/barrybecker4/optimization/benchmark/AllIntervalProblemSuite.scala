// Copyright by Barry G. Becker, 2026. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.benchmark

import org.scalatest.funsuite.AnyFunSuite

class AllIntervalProblemSuite extends AnyFunSuite {

  private def arrayOf(values: Seq[Int]) = com.barrybecker4.optimization.parameter.PermutedParameterArray(
    values.indices.map(i => new com.barrybecker4.optimization.parameter.types.IntegerParameter(values(i), 0, values.size - 1, "p" + i)),
    new scala.util.Random(1))

  test("the known series has every difference once, for any size") {
    (3 to 40).foreach(size => assertResult(0.0, s"size $size")(new AllIntervalProblem(size, 1).getExactSolution.fitness))
  }

  test("numbers in order have just one difference, so all the others are missing") {
    val problem = new AllIntervalProblem(10, 1)
    assertResult(8.0)(problem.evaluateFitness(arrayOf(0 until 10)))
  }

  test("the initial guess has each number once, and is not already a solution") {
    val problem = new AllIntervalProblem(14, 2)
    val guess = problem.getInitialGuess
    assertResult((0 until 14).toList)((0 until 14).map(guess.get(_).getValue.toInt).sorted.toList)
    assert(problem.evaluateFitness(guess) > 0)
  }

  test("the same seed gives the same initial guess") {
    def values(seed: Long) = { val g = new AllIntervalProblem(14, seed).getInitialGuess; (0 until 14).map(g.get(_).getValue) }
    assertResult(values(3))(values(3))
  }

  test("a series needs at least 3 numbers") {
    intercept[IllegalArgumentException](new AllIntervalProblem(2, 1))
  }
}
