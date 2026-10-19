package com.barrybecker4.optimization.strategy.hillclimbing

import com.barrybecker4.optimization.{Optimizer, OptimizationListener}
import com.barrybecker4.optimization.optimizee.BudgetedOptimizee
import com.barrybecker4.optimization.parameter.ParameterArrayWithFitness
import com.barrybecker4.optimization.strategy.hillclimbing.HillClimbingStrategy
import com.barrybecker4.optimization.optimizee.Optimizee
import com.barrybecker4.optimization.parameter.types.{DoubleParameter, IntegerParameter}
import com.barrybecker4.optimization.parameter.{NumericParameterArray, ParameterArray}
import com.barrybecker4.optimization.strategy.OptimizationStrategyType.HILL_CLIMBING
import com.barrybecker4.optimization.strategy.hillclimbing.optimizees.{BaselineRelativePermutationCompareOptimizee, BaselineRelativeQuadraticCompareOptimizee}
import org.scalatest.funsuite.AnyFunSuite

import scala.util.Random

class HillClimbingStrategySuite extends AnyFunSuite {

  test("hill climbing converges on simple numeric absolute objective") {
    val rnd = new Random(2)
    val optimizee = new QuadraticAbsoluteOptimizee(target = 2.0)
    val optimizer = new Optimizer(optimizee)
    val initial = NumericParameterArray(
      IndexedSeq(DoubleParameter(value = -6.0, minValue = -10.0, maxValue = 10.0, name = "x")),
      rnd = rnd
    )

    val solution = optimizer.doOptimization(HILL_CLIMBING, initial, fitnessRange = 100.0, rnd = rnd).best
    assert(solution.fitness < 1.0e-5)
    assert(math.abs(solution.pa.get(0).getValue - 2.0) < 0.02)
  }

  test("comparison mode keeps baseline-referenced fitness in hill climbing") {
    val rnd = new Random(3)
    val optimizee = new BaselineRelativeQuadraticCompareOptimizee(target = 1.5)
    val optimizer = new Optimizer(optimizee)
    val initial = NumericParameterArray(
      IndexedSeq(DoubleParameter(value = -4.0, minValue = -10.0, maxValue = 10.0, name = "x")),
      rnd = rnd
    )

    val solution = optimizer.doOptimization(HILL_CLIMBING, initial, fitnessRange = 100.0, rnd = rnd).best
    val expectedFitness = optimizee.compareFitness(solution.pa, initial)
    assert(math.abs(solution.fitness - expectedFitness) < 1.0e-9)
    assert(solution.fitness < 0.0)
  }

  test("comparison-mode discrete hill climbing exposes comparable final fitness") {
    val rnd = new Random(7)
    val optimizee = new BaselineRelativePermutationCompareOptimizee
    val optimizer = new Optimizer(optimizee)
    val initial = com.barrybecker4.optimization.parameter.PermutedParameterArray(
      IndexedSeq(
        IntegerParameter(3, 0, 3, "a"),
        IntegerParameter(0, 0, 3, "b"),
        IntegerParameter(2, 0, 3, "c"),
        IntegerParameter(1, 0, 3, "d")
      ),
      rnd
    )

    val solution = optimizer.doOptimization(HILL_CLIMBING, initial, fitnessRange = 10.0, rnd = rnd).best
    val expectedFitness = optimizee.compareFitness(solution.pa, initial)
    assert(math.abs(solution.fitness - expectedFitness) < 1.0e-9)
  }
  private def oneDim(x: Double, rnd: Random) = NumericParameterArray(
    IndexedSeq(DoubleParameter(value = x, minValue = -10.0, maxValue = 10.0, name = "x")), rnd = rnd)

  test("comparison mode: a start that cannot be improved scores zero against itself, not Double.MaxValue") {
    val rnd = new Random(5)
    val optimizee = new BaselineRelativeQuadraticCompareOptimizee(target = 2.0)
    val start = oneDim(2.0, rnd)
    val result = new HillClimbingStrategy(optimizee).doOptimization(start, fitnessRange = 100.0)
    assert(result.fitness == 0.0, s"expected 0.0 but got ${result.fitness}")
  }

  test("absolute mode: a start that is already optimal is returned after a single evaluation") {
    val rnd = new Random(5)
    val counting = new CountingQuadratic(target = 2.0)
    val result = new HillClimbingStrategy(counting).doOptimization(oneDim(2.0, rnd), fitnessRange = 100.0)
    assert(result.fitness == 0.0)
    assert(counting.evals == 1, s"expected no gradient probing at the optimum, got ${counting.evals} evaluations")
  }

  test("climb from a known start reports fitness relative to the given baseline in comparison mode") {
    val rnd = new Random(9)
    val optimizee = new BaselineRelativeQuadraticCompareOptimizee(target = 1.5)
    val baseline = oneDim(-4.0, rnd)
    val start = oneDim(0.0, rnd)
    val startFitness = optimizee.compareFitness(start, baseline)
    val result = new HillClimbingStrategy(optimizee).climb(ParameterArrayWithFitness(start, startFitness), baseline)
    assert(math.abs(result.fitness - optimizee.compareFitness(result.pa, baseline)) < 1.0e-9)
    assert(result.fitness < startFitness)
  }

  test("the listener hears about every accepted point including the first improvement") {
    val rnd = new Random(2)
    val seen = scala.collection.mutable.ArrayBuffer[Double]()
    val strategy = new HillClimbingStrategy(new QuadraticAbsoluteOptimizee(target = 2.0))
    strategy.setListener(new OptimizationListener {
      override def optimizerChanged(params: ParameterArrayWithFitness): Unit = seen += params.fitness
    })
    val result = strategy.doOptimization(oneDim(-6.0, rnd), fitnessRange = 100.0)
    assert(seen.size >= 3, s"expected the start plus several improvements, got $seen")
    assert(seen.last == result.fitness)
    assert(seen.zip(seen.tail).forall { case (a, b) => b <= a }, s"fitness should never get worse: $seen")
  }

  test("when the budget runs out mid-climb the best point reached so far is available") {
    val rnd = new Random(2)
    val budgeted = new BudgetedOptimizee(new QuadraticAbsoluteOptimizee(target = 2.0), maxEvaluations = 12)
    val strategy = new HillClimbingStrategy(budgeted)
    val start = oneDim(-6.0, rnd)
    assertThrows[com.barrybecker4.optimization.optimizee.EvaluationBudgetExceededException] {
      strategy.doOptimization(start, fitnessRange = 100.0)
    }
    val best = strategy.bestSoFar.get
    assert(best.fitness < 64.0, s"expected progress beyond the start (fitness 64), got ${best.fitness}")
  }
}

private final class QuadraticAbsoluteOptimizee(target: Double) extends Optimizee {
  override def getName: String = "quadratic-absolute"
  override def evaluateByComparison: Boolean = false
  override def evaluateFitness(params: ParameterArray): Double = {
    val x = params.get(0).getValue
    val d = x - target
    d * d
  }
  override def compareFitness(params1: ParameterArray, params2: ParameterArray): Double =
    throw new UnsupportedOperationException
  override def getOptimalFitness: Double = 0.0
}

private final class CountingQuadratic(target: Double) extends Optimizee {
  var evals = 0
  override def getName: String = "counting-quadratic"
  override def evaluateByComparison: Boolean = false
  override def evaluateFitness(params: ParameterArray): Double = {
    evals += 1
    val d = params.get(0).getValue - target
    d * d
  }
  override def compareFitness(params1: ParameterArray, params2: ParameterArray): Double =
    throw new UnsupportedOperationException
  override def getOptimalFitness: Double = 0.0
}
