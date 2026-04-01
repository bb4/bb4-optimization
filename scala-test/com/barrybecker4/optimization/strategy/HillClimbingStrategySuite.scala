package com.barrybecker4.optimization.strategy

import com.barrybecker4.optimization.Optimizer
import com.barrybecker4.optimization.optimizee.Optimizee
import com.barrybecker4.optimization.parameter.{NumericParameterArray, ParameterArray}
import com.barrybecker4.optimization.parameter.types.{DoubleParameter, IntegerParameter}
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

    val solution = optimizer.doOptimization(HILL_CLIMBING, initial, fitnessRange = 100.0, rnd = rnd)
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

    val solution = optimizer.doOptimization(HILL_CLIMBING, initial, fitnessRange = 100.0, rnd = rnd)
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

    val solution = optimizer.doOptimization(HILL_CLIMBING, initial, fitnessRange = 10.0, rnd = rnd)
    val expectedFitness = optimizee.compareFitness(solution.pa, initial)
    assert(math.abs(solution.fitness - expectedFitness) < 1.0e-9)
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

private final class BaselineRelativeQuadraticCompareOptimizee(target: Double) extends Optimizee {
  override def getName: String = "quadratic-compare"
  override def evaluateByComparison: Boolean = true
  override def evaluateFitness(params: ParameterArray): Double =
    throw new UnsupportedOperationException
  override def compareFitness(sample: ParameterArray, baseline: ParameterArray): Double =
    score(sample) - score(baseline)
  override def getOptimalFitness: Double = 0.0

  private def score(params: ParameterArray): Double = {
    val x = params.get(0).getValue
    val d = x - target
    d * d
  }
}

private final class BaselineRelativePermutationCompareOptimizee extends Optimizee {
  override def getName: String = "perm-compare-baseline-relative"
  override def evaluateByComparison: Boolean = true
  override def evaluateFitness(params: ParameterArray): Double =
    throw new UnsupportedOperationException
  override def compareFitness(sample: ParameterArray, baseline: ParameterArray): Double =
    score(sample) - score(baseline)
  override def getOptimalFitness: Double = 0.0

  private def score(params: ParameterArray): Double = {
    var sum = 0.0
    for (i <- 0 until params.size) {
      sum += math.abs(params.get(i).getValue - i)
    }
    sum
  }
}
