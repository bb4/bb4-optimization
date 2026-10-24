// Copyright by Barry G. Becker, 2026. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.strategy.hillclimbing

import com.barrybecker4.optimization.benchmark.RandomTourProblem
import com.barrybecker4.optimization.optimizee.{BudgetedOptimizee, DeltaCheckingOptimizee, EvaluationBudgetExceededException}
import com.barrybecker4.optimization.parameter.{Move, ParameterArray, PermutedParameterArray, PuzzleLikePath}
import org.scalatest.funsuite.AnyFunSuite

import scala.util.Random

/** With `useMoves`, each try is a move, and a neighbor is built only for the move that improves. Tries are not remembered. */
class HillClimbingMovesSuite extends AnyFunSuite {

  private class CountingTour(cities: Int, seed: Long) extends RandomTourProblem(cities, seed) {
    var moveEvaluations = 0
    override def evaluateMove(tour: ParameterArray, length: Double, move: Move): Double = {
      moveEvaluations += 1
      super.evaluateMove(tour, length, move)
    }
  }

  private def start(problem: RandomTourProblem, seed: Long): ParameterArray =
    problem.getInitialGuess.asInstanceOf[PermutedParameterArray].forkWithRnd(new Random(seed))

  private def climb(optimizee: com.barrybecker4.optimization.optimizee.Optimizee, from: ParameterArray, useMoves: Boolean) = {
    val strategy = new HillClimbingStrategy(optimizee)
    strategy.setConfig(HillClimbingConfig(useMoves = useMoves))
    strategy.doOptimization(from, 100.0)
  }

  test("moves are off by default") {
    assert(!HillClimbingConfig().useMoves && !GlobalHillClimbingConfig().useMoves)
    assert(!new HillClimbingStrategy(new RandomTourProblem(10, 1)).getConfig.useMoves)
  }

  test("with moves the fitness returned is the length of the tour returned, with every delta checked") {
    for (seed <- 1L to 4L) {
      val problem = new RandomTourProblem(30, seed)
      val result = climb(new DeltaCheckingOptimizee(problem), start(problem, seed), useMoves = true)
      assert(Math.abs(result.fitness - problem.evaluateFitness(result.pa)) < 1e-9, s"seed $seed")
      assert(result.fitness < problem.evaluateFitness(start(problem, seed)))
    }
  }

  test("with moves the climb asks for move evaluations") {
    val problem = new CountingTour(40, 6)
    climb(problem, start(problem, 6), useMoves = true)
    assert(problem.moveEvaluations > 100)
  }

  test("without moves it asks for none") {
    val problem = new CountingTour(40, 6)
    climb(problem, start(problem, 6), useMoves = false)
    assertResult(0)(problem.moveEvaluations)
  }

  test("with moves the climb ends at a tour about as good as without, over several seeds") {
    def median(useMoves: Boolean) = {
      val ratios = (1L to 7L).map { seed =>
        val problem = new RandomTourProblem(30, seed)
        climb(problem, start(problem, seed), useMoves).fitness / problem.evaluateFitness(start(problem, seed))
      }.sorted
      ratios(ratios.size / 2)
    }
    assert(median(useMoves = true) < median(useMoves = false) * 1.15)
  }

  test("moves count against the evaluation budget") {
    val problem = new RandomTourProblem(30, 2)
    val budgeted = new BudgetedOptimizee(problem, maxEvaluations = 200)
    intercept[EvaluationBudgetExceededException](climb(budgeted, start(problem, 2), useMoves = true))
    assertResult(200)(budgeted.evaluationsUsed)
  }

  test("an array that picks its own neighbors keeps doing so even when moves are asked for") {
    val problem = new RandomTourProblem(20, 4)
    val path = new PuzzleLikePath(problem.getInitialGuess.asInstanceOf[PermutedParameterArray].params, "route", new Random(4))
    val result = climb(problem, path, useMoves = true)
    assert(path.neighborsAsked > 20 && result.pa.isInstanceOf[PuzzleLikePath])
  }

  test("global hill climbing passes the setting to each climb") {
    val problem = new CountingTour(30, 3)
    val strategy = new GlobalHillClimbingStrategy(problem)
    strategy.setConfig(GlobalHillClimbingConfig(multistartCount = 2, globalSampleBudget = 40, useMoves = true))
    strategy.doOptimization(start(problem, 3), 100.0)
    assert(problem.moveEvaluations > 50)
  }
}
