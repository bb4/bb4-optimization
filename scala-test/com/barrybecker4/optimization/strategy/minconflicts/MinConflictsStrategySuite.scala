// Copyright by Barry G. Becker, 2026. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.strategy.minconflicts

import com.barrybecker4.optimization.benchmark.{GraphColoringProblem, NQueensProblem}
import com.barrybecker4.optimization.optimizee.{BudgetedOptimizee, Optimizee}
import com.barrybecker4.optimization.parameter.{Move, ParameterArray, PuzzleLikePath, PermutedParameterArray, Swap}
import org.scalatest.funsuite.AnyFunSuite

import scala.collection.mutable.ArrayBuffer
import scala.util.Random

class MinConflictsStrategySuite extends AnyFunSuite {

  private def strategyFor(problem: Optimizee, config: MinConflictsConfig, seed: Long = 1) = {
    val strategy = new MinConflictsStrategy(problem, new Random(seed))
    strategy.setConfig(config)
    strategy
  }

  test("it solves a larger N-queens problem quickly") {
    val problem = new NQueensProblem(100, 1)
    val budget = new BudgetedOptimizee(problem, 2000000)
    val result = strategyFor(budget, MinConflictsConfig()).doOptimization(problem.getInitialGuess, 1.0)
    assertResult(0.0)(result.fitness)
    assert(!budget.wasBudgetExhausted)
  }

  test("it solves a small graph coloring problem") {
    val problem = new GraphColoringProblem(60, 4, 5.0, 2)
    val result = strategyFor(problem, MinConflictsConfig()).doOptimization(problem.getInitialGuess, 1.0)
    assertResult(0.0)(result.fitness)
  }

  test("every move that is scored belongs to a queen that is in conflict") {
    var scoredMoves = 0
    var outsideConflicts = 0
    val recording = new NQueensProblem(40, 3) {
      override def evaluateMove(c: ParameterArray, f: Double, m: Move): Double = {
        m match {
          case Swap(position, _) =>
            scoredMoves += 1
            if (f > 0 && !conflictedPositions(c).contains(position)) outsideConflicts += 1
          case _ =>
        }
        super.evaluateMove(c, f, m)
      }
    }
    strategyFor(recording, MinConflictsConfig(maxSteps = 30, noiseProbability = 0.0, restartAfterStagnantSteps = 0))
      .doOptimization(recording.getInitialGuess, 1.0)
    assert(scoredMoves > 0)
    assertResult(0)(outsideConflicts)
  }

  test("a run with no noise repeats for the same seed") {
    def run() = {
      val problem = new NQueensProblem(52, 3)
      strategyFor(problem, MinConflictsConfig(maxSteps = 25, noiseProbability = 0.0), seed = 4)
        .doOptimization(problem.getInitialGuess, 1.0).fitness
    }
    assertResult(run())(run())
  }

  test("each step scores every move of the position, which is the size less one for a permutation") {
    val problem = new NQueensProblem(40, 3)
    val budget = new BudgetedOptimizee(problem, Int.MaxValue)
    strategyFor(budget, MinConflictsConfig(maxSteps = 5, restartAfterStagnantSteps = 0)).doOptimization(problem.getInitialGuess, 1.0)
    assertResult(1 + 5 * 39)(budget.evaluationsUsed)
  }

  test("when the problem reports no conflicts it still runs, with a random position") {
    val problem = new NQueensProblem(30, 3) { override def conflictedPositions(c: ParameterArray): Seq[Int] = Seq.empty }
    val start = problem.getInitialGuess
    val result = strategyFor(problem, MinConflictsConfig(maxSteps = 200)).doOptimization(start, 1.0)
    assert(result.fitness <= problem.evaluateFitness(start))
  }

  test("when the budget is used up the best so far is returned") {
    val problem = new NQueensProblem(60, 3)
    val budget = new BudgetedOptimizee(problem, 300)
    val result = strategyFor(budget, MinConflictsConfig()).doOptimization(problem.getInitialGuess, 1.0)
    assert(budget.wasBudgetExhausted)
    assertResult(result.fitness)(problem.evaluateFitness(result.pa))
  }

  test("an array without local moves is refused") {
    val problem = new NQueensProblem(30, 3)
    val plain = new PuzzleLikePath(problem.getInitialGuess.asInstanceOf[PermutedParameterArray].params, "path")
    intercept[IllegalArgumentException](strategyFor(problem, MinConflictsConfig()).doOptimization(plain, 1.0))
  }

  test("comparison of fitness is refused") {
    val problem = new NQueensProblem(30, 1) { override def evaluateByComparison: Boolean = true }
    intercept[IllegalArgumentException](strategyFor(problem, MinConflictsConfig()).doOptimization(problem.getInitialGuess, 1.0))
  }

  test("the configuration refuses values that make no sense") {
    intercept[IllegalArgumentException](MinConflictsConfig(noiseProbability = 1.5))
    intercept[IllegalArgumentException](MinConflictsConfig(maxSteps = -1))
    intercept[IllegalArgumentException](MinConflictsConfig(restartAfterStagnantSteps = -1))
  }
}
