// Copyright by Barry G. Becker, 2026. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.benchmark

import com.barrybecker4.optimization.parameter.{ParameterArray, Swap, SwapSequence}
import org.scalatest.funsuite.AnyFunSuite

import scala.util.Random

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

  private def lengthAfter(problem: RandomTourProblem, tour: ParameterArray, i: Int, j: Int): Double =
    problem.evaluateFitness(tour.applyMove(Swap(i, j)))

  test("the length after a swap from evaluateMove is the length of the swapped tour, for every pair on small tours") {
    for (cities <- 3 to 7; seed <- 1 to 5) {
      val problem = new RandomTourProblem(cities, seed)
      val tour = problem.getInitialGuess
      val length = problem.evaluateFitness(tour)
      for (i <- 0 until cities; j <- 0 until cities)
        assert(Math.abs(problem.evaluateMove(tour, length, Swap(i, j)) - lengthAfter(problem, tour, i, j)) < 1e-9,
          s"$cities cities, seed $seed, swap $i and $j")
    }
  }

  test("a swap of a position with itself changes nothing") {
    val problem = new RandomTourProblem(10, 4)
    val tour = problem.getInitialGuess
    val length = problem.evaluateFitness(tour)
    assertResult(length)(problem.evaluateMove(tour, length, Swap(3, 3)))
  }

  test("random swaps on a longer tour, chained so that each starts from the last result, agree with full evaluation") {
    val problem = new DeltaCheckingTour(new RandomTourProblem(50, 11))
    var tour = problem.getInitialGuess
    var length = problem.evaluateFitness(tour)
    val rnd = new Random(2)
    for (_ <- 1 to 3000) {
      val move = Swap(rnd.nextInt(50), rnd.nextInt(50))
      length = problem.evaluateMove(tour, length, move)
      tour = tour.applyMove(move)
    }
    assert(Math.abs(length - problem.evaluateFitness(tour)) < 1e-9, "rounding error did not pile up")
  }

  test("a sequence of swaps is evaluated in full, which gives the same answer") {
    val problem = new RandomTourProblem(12, 6)
    val tour = problem.getInitialGuess
    val move = SwapSequence(Vector(Swap(0, 5), Swap(5, 9), Swap(2, 3)))
    assertResult(problem.evaluateFitness(tour.applyMove(move)))(problem.evaluateMove(tour, problem.evaluateFitness(tour), move))
  }

  test("a move evaluation does not look at every city of the tour") {
    val problem = new RandomTourProblem(2000, 1)
    val tour = problem.getInitialGuess
    val length = problem.evaluateFitness(tour)
    val start = System.nanoTime()
    for (k <- 1 to 20000) problem.evaluateMove(tour, length, Swap(k % 2000, (k * 7 + 1) % 2000))
    assert((System.nanoTime() - start) / 1e9 < 1.0, "20000 deltas on 2000 cities should take milliseconds")
  }

  /** Checks the deltas of the problem it wraps while the test uses it. */
  private class DeltaCheckingTour(inner: RandomTourProblem)
    extends com.barrybecker4.optimization.optimizee.DeltaCheckingOptimizee(inner) {
    def getInitialGuess: ParameterArray = inner.getInitialGuess
  }
}
