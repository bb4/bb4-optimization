// Copyright by Barry G. Becker, 2026. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.strategy.restart

import com.barrybecker4.optimization.benchmark.NQueensProblem
import com.barrybecker4.optimization.optimizee.{BudgetedOptimizee, EvaluationBudgetExceededException, Optimizee}
import com.barrybecker4.optimization.parameter.{ParameterArray, ParameterArrayWithFitness}
import com.barrybecker4.optimization.strategy.OptimizationStrategy
import org.scalatest.funsuite.AnyFunSuite

import scala.collection.mutable.ArrayBuffer
import scala.util.Random

class RestartingStrategySuite extends AnyFunSuite {

  private val Size = 30

  /** Evaluates random samples until it has made `evaluationLimit` of them, or until the budget stops it. */
  private class Sampler(optimizee: Optimizee, evaluationLimit: Int, starts: ArrayBuffer[ParameterArray],
                        results: ArrayBuffer[Double], catchBudget: Boolean = false) extends OptimizationStrategy(optimizee) {
    override def doOptimization(start: ParameterArray, fitnessRange: Double): ParameterArrayWithFitness = {
      starts += start
      var best = ParameterArrayWithFitness(start, optimizee.evaluateFitness(start))
      notifyOfChange(best)
      try {
        for (_ <- 1 until evaluationLimit) {
          val sample = start.getRandomSample
          val scored = ParameterArrayWithFitness(sample, optimizee.evaluateFitness(sample))
          if (scored.fitness < best.fitness) { best = scored; notifyOfChange(best) }
        }
      } catch { case e: EvaluationBudgetExceededException => if (!catchBudget) throw e }
      results += best.fitness
      best
    }
  }

  private def queens(seed: Long = 1) = new NQueensProblem(Size, seed)

  private def strategyOf(problem: NQueensProblem, evaluationLimit: Int, starts: ArrayBuffer[ParameterArray] = ArrayBuffer(),
                         results: ArrayBuffer[Double] = ArrayBuffer(), catchBudget: Boolean = false, optimizee: Optimizee = null) =
    new RestartingStrategy(if (optimizee == null) problem else optimizee,
      limited => new Sampler(limited, evaluationLimit, starts, results, catchBudget))

  test("the Luby sequence") {
    assertResult(List(1, 1, 2, 1, 1, 2, 4, 1, 1, 2, 1, 1, 2, 4, 8, 1))((1 to 16).map(RestartingStrategy.luby))
  }

  test("the Luby sequence starts at 1") {
    intercept[IllegalArgumentException](RestartingStrategy.luby(0))
  }

  test("each run gets the Luby multiple of the base budget, and a run that wants more is cut off") {
    val problem = queens()
    val strategy = strategyOf(problem, evaluationLimit = Int.MaxValue)
    strategy.setConfig(RestartingConfig(maxRestarts = 7, baseBudget = 100, adaptBaseBudget = false))
    strategy.doOptimization(problem.getInitialGuess, problem.getFitnessRange)
    assertResult(7)(strategy.runCount)
    assertResult(1200)(strategy.evaluationsUsed)
  }

  test("the evaluations of all the runs never pass the total budget") {
    val problem = queens()
    val strategy = strategyOf(problem, evaluationLimit = Int.MaxValue)
    strategy.setConfig(RestartingConfig(baseBudget = 400, totalBudget = 1000))
    strategy.doOptimization(problem.getInitialGuess, problem.getFitnessRange)
    assertResult(1000)(strategy.evaluationsUsed)
  }

  test("when a budget that was set outside is used up the search ends and returns the best so far") {
    val problem = queens()
    val outer = new BudgetedOptimizee(problem, 250)
    val strategy = strategyOf(problem, Int.MaxValue, catchBudget = true, optimizee = outer)
    strategy.setConfig(RestartingConfig(baseBudget = 100))
    val result = strategy.doOptimization(problem.getInitialGuess, problem.getFitnessRange)
    assertResult(250)(outer.evaluationsUsed)
    assert(result.fitness < Double.MaxValue)
    assert(strategy.runCount <= 4)
  }

  test("the first run starts from the initial guess and the best of all runs is returned") {
    val problem = queens()
    val starts = ArrayBuffer[ParameterArray]()
    val results = ArrayBuffer[Double]()
    val strategy = strategyOf(problem, evaluationLimit = 20, starts, results)
    strategy.setConfig(RestartingConfig(maxRestarts = 6, baseBudget = 20, adaptBaseBudget = false))
    val initial = problem.getInitialGuess
    val result = strategy.doOptimization(initial, problem.getFitnessRange)
    assert(starts.head eq initial)
    assertResult(6)(results.size)
    assertResult(results.min)(result.fitness)
  }

  test("the search stops when a run reaches the goal fitness") {
    val problem = queens()
    var runs = 0
    val strategy = new RestartingStrategy(problem, _ => new OptimizationStrategy(problem) {
      override def doOptimization(start: ParameterArray, range: Double): ParameterArrayWithFitness = {
        runs += 1
        if (runs == 3) problem.getExactSolution else ParameterArrayWithFitness(start, 5.0)
      }
    })
    val result = strategy.doOptimization(problem.getInitialGuess, problem.getFitnessRange)
    assertResult(0.0)(result.fitness)
    assertResult(3)(strategy.runCount)
  }

  test("with the base budget adapted, a first run that ends by itself sets the length of the runs after it") {
    val problem = queens()
    val strategy = strategyOf(problem, evaluationLimit = 60)
    strategy.setConfig(RestartingConfig(maxRestarts = 3, baseBudget = 1000))
    strategy.doOptimization(problem.getInitialGuess, problem.getFitnessRange)
    assertResult(60 + 60 + 60)(strategy.evaluationsUsed)
    val unadapted = strategyOf(problem, evaluationLimit = Int.MaxValue)
    unadapted.setConfig(RestartingConfig(maxRestarts = 3, baseBudget = 1000, adaptBaseBudget = false))
    unadapted.doOptimization(problem.getInitialGuess, problem.getFitnessRange)
    assertResult(4000)(unadapted.evaluationsUsed)
  }

  test("a restart after failures is a small change of the best when the chance of perturbing is certain") {
    val problem = queens()
    val starts = ArrayBuffer[ParameterArray]()
    val strategy = strategyOf(problem, evaluationLimit = 1, starts)
    strategy.setConfig(RestartingConfig(maxRestarts = 30, baseBudget = 1, perturbationStep = 1.0, maxPerturbationProbability = 1.0))
    strategy.doOptimization(problem.getInitialGuess, problem.getFitnessRange)
    val differing = starts.tail.map(s => (0 until Size).count(i => s.get(i).getValue != starts.head.get(i).getValue))
    // The best may change along the way, but a start is always a few swaps from some best; no start is a fresh shuffle.
    assert(differing.count(_ > Size * 2 / 3) < starts.size / 2)
  }

  test("two seeded runs make the same result") {
    def run() = {
      val problem = queens(4)
      val strategy = strategyOf(problem, evaluationLimit = 30)
      strategy.setConfig(RestartingConfig(maxRestarts = 5, baseBudget = 30))
      (strategy.doOptimization(problem.getInitialGuess, problem.getFitnessRange).fitness, strategy.evaluationsUsed)
    }
    assertResult(run())(run())
  }

  test("comparison of fitness is refused") {
    val problem = new NQueensProblem(Size, 1) { override def evaluateByComparison: Boolean = true }
    intercept[IllegalArgumentException](strategyOf(problem, 10).doOptimization(problem.getInitialGuess, 1.0))
  }

  test("the configuration refuses values that make no sense") {
    intercept[IllegalArgumentException](RestartingConfig(maxRestarts = 0))
    intercept[IllegalArgumentException](RestartingConfig(baseBudget = 0))
    intercept[IllegalArgumentException](RestartingConfig(perturbRadius = 2.0))
    intercept[IllegalArgumentException](RestartingConfig(maxPerturbationProbability = 1.5))
  }
}
