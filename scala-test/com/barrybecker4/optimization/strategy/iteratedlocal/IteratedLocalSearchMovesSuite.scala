// Copyright by Barry G. Becker, 2026. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.strategy.iteratedlocal

import com.barrybecker4.optimization.benchmark.RandomTourProblem
import com.barrybecker4.optimization.optimizee.{BudgetedOptimizee, DeltaCheckingOptimizee, Optimizee}
import com.barrybecker4.optimization.parameter.{Move, ParameterArray, ParameterArrayWithFitness, PermutedParameterArray, PuzzleLikePath}
import org.scalatest.funsuite.AnyFunSuite

import scala.util.Random

/**
  * With `useMoves`, a local search asks for the fitness after a move and builds a neighbor only if it is better. The
  * price is that it no longer remembers every neighbor it rejected, so it may evaluate one again.
  */
class IteratedLocalSearchMovesSuite extends AnyFunSuite {

  private class CountingTour(cities: Int, seed: Long) extends RandomTourProblem(cities, seed) {
    var moveEvaluations = 0
    val evaluated = scala.collection.mutable.ArrayBuffer[Seq[Int]]()
    override def evaluateFitness(tour: ParameterArray): Double = {
      evaluated += (0 until tour.size).map(tour.get(_).getValue.toInt)
      super.evaluateFitness(tour)
    }
    override def evaluateMove(tour: ParameterArray, length: Double, move: Move): Double = {
      moveEvaluations += 1
      super.evaluateMove(tour, length, move)
    }
  }

  private def start(problem: RandomTourProblem, seed: Long): ParameterArray =
    problem.getInitialGuess.asInstanceOf[PermutedParameterArray].forkWithRnd(new Random(seed))

  private def search(optimizee: Optimizee, from: ParameterArray, range: Double, config: IteratedLocalSearchConfig,
                     seed: Long = 1): ParameterArrayWithFitness =
    IteratedLocalSearchStrategy.strategyFor(optimizee, config, new Random(seed)).doOptimization(from, range)

  private val quick = IteratedLocalSearchConfig(maxIterations = 30, maxStaleNeighbors = 40)

  test("moves are off by default, so a distinct solution is still evaluated at most once") {
    assert(!IteratedLocalSearchConfig.Default.useMoves && !IteratedLocalSearchConfig.VariableNeighborhoodSearch.useMoves)
    val problem = new CountingTour(12, 3)
    search(problem, start(problem, 3), problem.getFitnessRange, quick)
    assertResult(0)(problem.moveEvaluations)
    assertResult(problem.evaluated.size)(problem.evaluated.distinct.size)
  }

  test("with moves the fitness returned is the length of the tour returned, with every delta checked") {
    for (seed <- 1L to 4L) {
      val problem = new RandomTourProblem(25, seed)
      val result = search(new DeltaCheckingOptimizee(problem), start(problem, seed), problem.getFitnessRange,
        quick.copy(useMoves = true), seed)
      assert(Math.abs(result.fitness - problem.evaluateFitness(result.pa)) < 1e-9, s"seed $seed")
      assert(result.fitness < problem.evaluateFitness(start(problem, seed)))
    }
  }

  test("with moves the search asks for move evaluations rather than evaluating each neighbor") {
    val problem = new CountingTour(40, 5)
    search(problem, start(problem, 5), problem.getFitnessRange, quick.copy(useMoves = true), 5)
    assert(problem.moveEvaluations > 200, s"${problem.moveEvaluations} move evaluations")
  }

  test("with moves the quality is about what it is without, over several seeds") {
    def median(useMoves: Boolean) = {
      val lengths = (1L to 7L).map { seed =>
        val problem = new RandomTourProblem(30, seed)
        search(problem, start(problem, seed), problem.getFitnessRange, quick.copy(useMoves = useMoves), seed).fitness / problem.evaluateFitness(start(problem, seed))
      }.sorted
      lengths(lengths.size / 2)
    }
    assert(median(useMoves = true) < median(useMoves = false) * 1.15)
  }

  test("moves count against the evaluation budget and the best so far comes back") {
    val problem = new RandomTourProblem(30, 2)
    val budgeted = new BudgetedOptimizee(problem, maxEvaluations = 300)
    val result = search(budgeted, start(problem, 2), problem.getFitnessRange, quick.copy(useMoves = true), 2)
    assert(budgeted.wasBudgetExhausted && budgeted.evaluationsUsed == 300)
    assert(result.fitness <= problem.evaluateFitness(start(problem, 2)))
  }

  test("an array that picks its own neighbors keeps doing so even when moves are asked for") {
    val problem = new RandomTourProblem(20, 4)
    val path = new PuzzleLikePath(problem.getInitialGuess.asInstanceOf[PermutedParameterArray].params, "route", new Random(4))
    val result = search(problem, path, problem.getFitnessRange, quick.copy(useMoves = true), 4)
    assert(path.neighborsAsked > 50)
    assert(result.pa.isInstanceOf[PuzzleLikePath])
  }

  test("an unvisited start and shakes are still not repeated: an accepted solution is remembered") {
    val problem = new CountingTour(10, 8)
    search(problem, start(problem, 8), problem.getFitnessRange, quick.copy(useMoves = true), 8)
    assert(problem.evaluated.size < 50, "only the starting point, shakes and restarts are evaluated in full")
  }
}
