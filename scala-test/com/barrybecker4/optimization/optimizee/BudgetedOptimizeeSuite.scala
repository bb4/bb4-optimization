package com.barrybecker4.optimization.optimizee

import com.barrybecker4.optimization.{OptimizationStatus, Optimizer}
import com.barrybecker4.optimization.parameter.NumericParameterArray
import com.barrybecker4.optimization.parameter.types.{DoubleParameter, IntegerParameter}
import com.barrybecker4.optimization.strategy.{DoubleWellOptimizee, GENETIC_SEARCH, GLOBAL_HILL_CLIMBING, HILL_CLIMBING}
import org.scalatest.funsuite.AnyFunSuite

import scala.util.Random

class BudgetedOptimizeeSuite extends AnyFunSuite {

  test("BudgetExhausted returns best-so-far after max evaluations") {
    val rnd = new Random(1)
    val problem = new LineGraphTestProblem(target = 7, lo = 0, hi = 20, rnd = rnd)
    val opt = new Optimizer(problem)
    val guess = new NumericParameterArray(IndexedSeq(new IntegerParameter(0, 0, 20, "x")), rnd = rnd)
    val result = opt.doOptimization(HILL_CLIMBING, guess, 100.0, rnd, maxEvaluations = Some(1))
    assert(result.status == OptimizationStatus.BudgetExhausted)
    assert(result.evaluationsUsed.contains(1))
    assert(result.best.pa.get(0).getValue.toInt == 0)
    assert(result.best.fitness == 7.0)
  }

  test("global hill climbing returns best-so-far when budget exhausted") {
    val rnd = new Random(3)
    val optimizee = new DoubleWellOptimizee()
    val guess = NumericParameterArray(
      IndexedSeq(DoubleParameter(value = 8.0, minValue = -10.0, maxValue = 10.0, name = "x")),
      rnd = rnd
    )
    val initialFitness = optimizee.evaluateFitness(guess)
    val opt = new Optimizer(optimizee)
    val result = opt.doOptimization(GLOBAL_HILL_CLIMBING, guess, 100.0, rnd, maxEvaluations = Some(120))
    assert(result.status == OptimizationStatus.BudgetExhausted)
    assert(result.best.fitness < initialFitness,
      s"expected best-so-far to improve on initial guess; got ${result.best.fitness} vs initial $initialFitness")
  }

  test("genetic search returns best-so-far when budget exhausted") {
    val rnd = new Random(5)
    val optimizee = new DoubleWellOptimizee()
    val guess = NumericParameterArray(
      IndexedSeq(DoubleParameter(value = 8.0, minValue = -10.0, maxValue = 10.0, name = "x")),
      rnd = rnd
    )
    val initialFitness = optimizee.evaluateFitness(guess)
    val opt = new Optimizer(optimizee)
    val result = opt.doOptimization(GENETIC_SEARCH, guess, 100.0, rnd, maxEvaluations = Some(80))
    assert(result.status == OptimizationStatus.BudgetExhausted)
    assert(result.best.fitness <= initialFitness,
      s"expected best-so-far not worse than initial; got ${result.best.fitness} vs initial $initialFitness")
  }
}
