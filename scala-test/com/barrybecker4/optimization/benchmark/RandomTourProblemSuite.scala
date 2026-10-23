// Copyright by Barry G. Becker, 2026. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.benchmark

import org.scalatest.funsuite.AnyFunSuite

class RandomTourProblemSuite extends AnyFunSuite {

  test("the same seed gives the same cities and the same initial tour") {
    val a = new RandomTourProblem(20, 7)
    val b = new RandomTourProblem(20, 7)
    assertResult(a.evaluateFitness(a.getInitialGuess))(b.evaluateFitness(b.getInitialGuess))
  }

  test("different seeds give different problems") {
    val a = new RandomTourProblem(20, 1)
    val b = new RandomTourProblem(20, 2)
    assert(a.evaluateFitness(a.getInitialGuess) != b.evaluateFitness(b.getInitialGuess))
  }

  test("the initial guess visits every city once") {
    val guess = new RandomTourProblem(30, 3).getInitialGuess
    assertResult((0 until 30).toList)((0 until guess.size).map(guess.get(_).getValue.toInt).sorted.toList)
  }

  test("a tour is as long as its legs, and the same tour in the other direction has the same length") {
    val problem = new RandomTourProblem(10, 5)
    val tour = problem.getInitialGuess
    val reversed = tour.asInstanceOf[com.barrybecker4.optimization.parameter.PermutedParameterArray].reverse
    assert(Math.abs(problem.evaluateFitness(tour) - problem.evaluateFitness(reversed)) < 1e-9)
    assert(problem.evaluateFitness(tour) > 0)
  }

  test("starting the tour at a different city does not change its length") {
    val problem = new RandomTourProblem(8, 9)
    val tour = problem.getInitialGuess.asInstanceOf[com.barrybecker4.optimization.parameter.PermutedParameterArray]
    val rotated = tour.setPermutation((1 until 8).toList :+ 0)
    assert(Math.abs(problem.evaluateFitness(tour) - problem.evaluateFitness(rotated)) < 1e-9)
  }

  test("a tour needs at least 3 cities") {
    intercept[IllegalArgumentException](new RandomTourProblem(2, 1))
  }
}
