// Copyright by Barry G. Becker, 2026. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.strategy.annealing

import com.barrybecker4.optimization.benchmark.RandomTourProblem
import com.barrybecker4.optimization.optimizee.{BudgetedOptimizee, DeltaCheckingOptimizee, Optimizee}
import com.barrybecker4.optimization.parameter.{Move, ParameterArray, ParameterArrayWithFitness, PermutedParameterArray, PuzzleLikePath}
import org.scalatest.funsuite.AnyFunSuite

import scala.util.Random

/** Simulated annealing on a permutation can ask for the fitness after a move instead of building each neighbor. */
class SimulatedAnnealingMovesSuite extends AnyFunSuite {

  private class CountingTour(cities: Int, seed: Long) extends RandomTourProblem(cities, seed) {
    var fullEvaluations = 0
    var moveEvaluations = 0
    override def evaluateFitness(tour: ParameterArray): Double = { fullEvaluations += 1; super.evaluateFitness(tour) }
    override def evaluateMove(tour: ParameterArray, length: Double, move: Move): Double = {
      moveEvaluations += 1
      super.evaluateMove(tour, length, move)
    }
  }

  private def valuesOf(pa: ParameterArray): Seq[Int] = (0 until pa.size).map(i => pa.get(i).getValue.toInt)

  /**
    * The tour as a cycle: starting at city 0, in the direction whose second city is lower. Tours that are rotations or
    * reversals of each other have equal length, so which one a search ends on can come down to rounding in the last digit.
    */
  private def asCycle(pa: ParameterArray): Seq[Int] = {
    val order = valuesOf(pa)
    val rotated = order.drop(order.indexOf(0)) ++ order.take(order.indexOf(0))
    val reversed = rotated.head +: rotated.tail.reverse
    if (reversed.lift(1).getOrElse(0) < rotated.lift(1).getOrElse(0)) reversed else rotated
  }

  private def anneal(optimizee: Optimizee, start: ParameterArray, useMoves: Boolean, seed: Long,
                     range: Double): ParameterArrayWithFitness = {
    val strategy = new SimulatedAnnealingStrategy(optimizee, new Random(seed))
    strategy.setConfig(SimulatedAnnealingConfig(useMoves = useMoves, numTempIterations = 8))
    strategy.doOptimization(start, range)
  }

  private def startFor(problem: RandomTourProblem, seed: Long): ParameterArray =
    problem.getInitialGuess.asInstanceOf[PermutedParameterArray].forkWithRnd(new Random(seed))

  test("with moves the search takes the same path and finds the same tour as it does building every neighbor") {
    for (seed <- 1L to 6L; cities <- Seq(8, 25, 60)) {
      val problem = new RandomTourProblem(cities, seed)
      val checked = new DeltaCheckingOptimizee(problem)
      val withMoves = anneal(checked, startFor(problem, seed), useMoves = true, seed, problem.getFitnessRange)
      val without = anneal(problem, startFor(problem, seed), useMoves = false, seed, problem.getFitnessRange)
      assertResult(asCycle(without.pa), s"$cities cities, seed $seed")(asCycle(withMoves.pa))
      assert(Math.abs(withMoves.fitness - without.fitness) < 1e-9)
    }
  }

  test("with moves nearly every proposal is a move evaluation; only the larger ones, several swaps, are evaluated in full") {
    val problem = new CountingTour(40, 3)
    anneal(problem, startFor(problem, 3), useMoves = true, 3, problem.getFitnessRange)
    assert(problem.moveEvaluations > 1000, s"only ${problem.moveEvaluations} proposals")
    assert(problem.fullEvaluations * 20 < problem.moveEvaluations, s"${problem.fullEvaluations} full evaluations")
  }

  test("without moves there is a full evaluation for every proposal and no move evaluations") {
    val problem = new CountingTour(40, 3)
    anneal(problem, startFor(problem, 3), useMoves = false, 3, problem.getFitnessRange)
    assertResult(0)(problem.moveEvaluations)
    assert(problem.fullEvaluations > 1000)
  }

  test("moves count against the evaluation budget the same as full evaluations, and the best so far comes back") {
    val problem = new RandomTourProblem(30, 5)
    val budgeted = new BudgetedOptimizee(problem, maxEvaluations = 500)
    val result = anneal(budgeted, startFor(problem, 5), useMoves = true, 5, problem.getFitnessRange)
    assert(budgeted.wasBudgetExhausted)
    assertResult(500)(budgeted.evaluationsUsed)
    assert(result.fitness <= problem.evaluateFitness(startFor(problem, 5)))
  }

  test("the fitness reported is the fitness of the tour returned") {
    val problem = new RandomTourProblem(50, 2)
    val result = anneal(problem, startFor(problem, 2), useMoves = true, 2, problem.getFitnessRange)
    assert(Math.abs(result.fitness - problem.evaluateFitness(result.pa)) < 1e-9)
  }

  test("an array that picks its own neighbors keeps doing so, and keeps its type") {
    val problem = new RandomTourProblem(20, 4)
    val path = new PuzzleLikePath(problem.getInitialGuess.asInstanceOf[PermutedParameterArray].params, "route", new Random(4))
    val result = anneal(problem, path, useMoves = true, 4, problem.getFitnessRange)
    assert(path.neighborsAsked > 100, "the array's own getRandomNeighbor was used")
    assert(result.pa.isInstanceOf[PuzzleLikePath] && result.pa.asInstanceOf[PuzzleLikePath].label == "route")
  }

  test("moves are on by default") {
    assert(SimulatedAnnealingConfig().useMoves)
  }
}
