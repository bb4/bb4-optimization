// Copyright by Barry G. Becker, 2026. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.strategy.tabu

import com.barrybecker4.optimization.benchmark.{GraphColoringProblem, NQueensProblem}
import com.barrybecker4.optimization.optimizee.BudgetedOptimizee
import com.barrybecker4.optimization.parameter.{Move, ParameterArray}
import com.barrybecker4.optimization.strategy.statespace.StateSpaceSearchStrategy
import org.scalatest.funsuite.AnyFunSuite

import scala.util.Random

class TabuSearchStrategySuite extends AnyFunSuite {

  private def strategyFor(problem: com.barrybecker4.optimization.optimizee.Optimizee, config: TabuSearchConfig, seed: Long = 1) = {
    val strategy = new TabuSearchStrategy(problem, new Random(seed))
    strategy.setConfig(config)
    strategy
  }

  test("it solves a small N-queens problem") {
    val problem = new NQueensProblem(30, 1)
    val result = strategyFor(problem, TabuSearchConfig()).doOptimization(problem.getInitialGuess, problem.getFitnessRange)
    assertResult(0.0)(result.fitness)
    assertResult(0.0)(problem.evaluateFitness(result.pa))
  }

  test("it solves a small graph coloring problem, which needs moves of one position") {
    val problem = new GraphColoringProblem(40, 4, 5.0, 2)
    val result = strategyFor(problem, TabuSearchConfig()).doOptimization(problem.getInitialGuess, problem.getFitnessRange)
    assertResult(0.0)(result.fitness)
  }

  test("a seeded run repeats") {
    def run() = {
      val problem = new NQueensProblem(40, 3)
      val config = TabuSearchConfig(maxIterations = 30)
      strategyFor(problem, config, seed = 9).doOptimization(problem.getInitialGuess, 1.0).fitness
    }
    assertResult(run())(run())
  }

  test("the result is never worse than the starting solution") {
    val problem = new NQueensProblem(40, 3)
    val start = problem.getInitialGuess
    val result = strategyFor(problem, TabuSearchConfig(maxIterations = 20)).doOptimization(start, 1.0)
    assert(result.fitness <= problem.evaluateFitness(start))
  }

  test("a solved problem ends early, using few evaluations") {
    val problem = new NQueensProblem(12, 1)
    val budget = new BudgetedOptimizee(problem, Int.MaxValue)
    strategyFor(budget, TabuSearchConfig(maxIterations = 100000)).doOptimization(problem.getInitialGuess, 1.0)
    assert(budget.evaluationsUsed < 100000L * 50)
  }

  test("when the budget is used up the best so far is returned") {
    val problem = new NQueensProblem(40, 3)
    val budget = new BudgetedOptimizee(problem, 500)
    val result = strategyFor(budget, TabuSearchConfig()).doOptimization(problem.getInitialGuess, 1.0)
    assert(budget.wasBudgetExhausted)
    assert(result.fitness < Double.PositiveInfinity)
    assertResult(result.fitness)(problem.evaluateFitness(result.pa))
  }

  test("each iteration scores as many moves as the config asks for") {
    val problem = new NQueensProblem(40, 3)
    val budget = new BudgetedOptimizee(problem, Int.MaxValue)
    strategyFor(budget, TabuSearchConfig(maxIterations = 10, candidateMoves = 7, restartAfterStagnantIterations = 0))
      .doOptimization(problem.getInitialGuess, 1.0)
    assertResult(1 + 10 * 7)(budget.evaluationsUsed)
  }

  test("with a tenure longer than the run and no aspiration, the search never returns to a solution it has left") {
    val problem = new NQueensProblem(40, 5)
    val currents = scala.collection.mutable.ArrayBuffer[Any]()
    val recording = new NQueensProblem(40, 5) {
      override def evaluateMove(c: ParameterArray, f: Double, m: Move): Double = {
        currents += StateSpaceSearchStrategy.stateKey(c)
        super.evaluateMove(c, f, m)
      }
    }
    val config = TabuSearchConfig(maxIterations = 50, candidateMoves = 7, tenureMin = 1000, tenureMax = 1000,
      aspiration = false, restartAfterStagnantIterations = 0)
    strategyFor(recording, config).doOptimization(problem.getInitialGuess, 1.0)
    assertResult(50)(currents.distinct.size)
  }

  test("an array without local moves is searched through random neighbors") {
    val problem = new NQueensProblem(30, 5)
    val plain = new com.barrybecker4.optimization.parameter.PuzzleLikePath(problem.getInitialGuess.asInstanceOf[com.barrybecker4.optimization.parameter.PermutedParameterArray].params, "path")
    assert(!plain.supportsLocalMoves)
    val result = strategyFor(problem, TabuSearchConfig(maxIterations = 20, candidateMoves = 5)).doOptimization(plain, 1.0)
    assert(result.fitness <= problem.evaluateFitness(plain))
  }

  test("comparison of fitness is refused") {
    val problem = new NQueensProblem(30, 1) { override def evaluateByComparison: Boolean = true }
    intercept[IllegalArgumentException](strategyFor(problem, TabuSearchConfig()).doOptimization(problem.getInitialGuess, 1.0))
  }

  test("an array with a real number is refused") {
    val problem = new com.barrybecker4.optimization.optimizee.optimizees.SphereProblem(2)
    intercept[IllegalArgumentException](strategyFor(problem, TabuSearchConfig()).doOptimization(problem.getInitialGuess, 1.0))
  }

  test("the configuration refuses values that make no sense") {
    intercept[IllegalArgumentException](TabuSearchConfig(candidateMoves = 0))
    intercept[IllegalArgumentException](TabuSearchConfig(tenureMin = 5, tenureMax = 4))
    intercept[IllegalArgumentException](TabuSearchConfig(maxIterations = -1))
    intercept[IllegalArgumentException](TabuSearchConfig(fallbackRadius = 0.0))
  }
}
