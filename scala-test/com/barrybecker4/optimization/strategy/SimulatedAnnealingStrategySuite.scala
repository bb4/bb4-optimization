package com.barrybecker4.optimization.strategy

import com.barrybecker4.optimization.optimizee.Optimizee
import com.barrybecker4.optimization.parameter.{NumericParameterArray, ParameterArray}
import com.barrybecker4.optimization.parameter.types.DoubleParameter
import org.scalatest.funsuite.AnyFunSuite

import scala.util.Random

class SimulatedAnnealingStrategySuite extends AnyFunSuite {

  test("doOptimization requires positive finite fitnessRange") {
    val rnd = new Random(0)
    val optimizee = new SaTestQuadraticOptimizee(target = 0.0)
    val sa = new SimulatedAnnealingStrategy(optimizee, rnd)
    val initial = NumericParameterArray(
      IndexedSeq(DoubleParameter(value = 2.0, minValue = -10.0, maxValue = 10.0, name = "x")),
      rnd = rnd
    )
    assertThrows[IllegalArgumentException](sa.doOptimization(initial, fitnessRange = 0.0))
    assertThrows[IllegalArgumentException](sa.doOptimization(initial, fitnessRange = -1.0))
  }

  test("fixed step mode improves on simple quadratic") {
    val rnd = new Random(5)
    val optimizee = new SaTestQuadraticOptimizee(target = 1.0)
    val sa = new SimulatedAnnealingStrategy(optimizee, rnd)
    sa.setStepMode(SimulatedAnnealingStepMode.Fixed)
    sa.setMaxTemperature(50.0)
    val initial = NumericParameterArray(
      IndexedSeq(DoubleParameter(value = -5.0, minValue = -10.0, maxValue = 10.0, name = "x")),
      rnd = rnd
    )
    val out = sa.doOptimization(initial, fitnessRange = 100.0)
    assert(out.fitness < 0.05)
    assert(math.abs(out.pa.get(0).getValue - 1.0) < 0.15)
  }

  test("adaptive acceptance completes on flat landscape and respects comparison-mode convention") {
    val rnd = new Random(11)
    val flat = new FlatAbsoluteOptimizee
    val sa = new SimulatedAnnealingStrategy(flat, rnd)
    sa.setStepMode(
      SimulatedAnnealingStepMode.AdaptiveAcceptance(
        targetRate = 0.2,
        windowProposals = 8,
        scaleMin = 0.5,
        scaleMax = 4.0,
        growFactor = 1.2,
        shrinkFactor = 0.85
      )
    )
    sa.setMaxTemperature(20.0)
    val initial = NumericParameterArray(
      IndexedSeq(DoubleParameter(value = 0.0, minValue = -1.0, maxValue = 1.0, name = "x")),
      rnd = rnd
    )
    val out = sa.doOptimization(initial, fitnessRange = 1.0)
    assert(out.fitness == 1.0)

    val rnd2 = new Random(13)
    val compareOpt = new SaTestBaselineRelativeQuadratic(target = 0.0)
    val sa2 = new SimulatedAnnealingStrategy(compareOpt, rnd2)
    sa2.setStepMode(SimulatedAnnealingStepMode.AdaptiveAcceptance(windowProposals = 10))
    sa2.setMaxTemperature(30.0)
    val initial2 = NumericParameterArray(
      IndexedSeq(DoubleParameter(value = 3.0, minValue = -10.0, maxValue = 10.0, name = "x")),
      rnd = rnd2
    )
    val out2 = sa2.doOptimization(initial2, fitnessRange = 100.0)
    val expected = compareOpt.compareFitness(out2.pa, initial2)
    assert(math.abs(out2.fitness - expected) < 1.0e-9)
  }
}

private final class SaTestQuadraticOptimizee(target: Double) extends Optimizee {
  override def getName: String = "quadratic-sa-test"
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

private final class SaTestBaselineRelativeQuadratic(target: Double) extends Optimizee {
  override def getName: String = "quadratic-compare-sa-test"
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

/** Constant positive fitness; Metropolis always accepts equal-fitness moves (exp(0)=1). */
private final class FlatAbsoluteOptimizee extends Optimizee {
  override def getName: String = "flat-sa-test"
  override def evaluateByComparison: Boolean = false
  override def evaluateFitness(params: ParameterArray): Double = 1.0
  override def compareFitness(params1: ParameterArray, params2: ParameterArray): Double =
    throw new UnsupportedOperationException
  override def getOptimalFitness: Double = 0.0
}
