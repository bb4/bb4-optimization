package com.barrybecker4.optimization.strategy.annealing

import com.barrybecker4.optimization.parameter.types.DoubleParameter
import com.barrybecker4.optimization.parameter.{NumericParameterArray, ParameterArray}
import com.barrybecker4.optimization.strategy.OptimizationStrategyType.SIMULATED_ANNEALING
import com.barrybecker4.optimization.strategy.annealing.optimizees.{FlatAbsoluteOptimizee, SaBaselineRelativeQuadratic, SaQuadraticOptimizee}
import com.barrybecker4.optimization.strategy.annealing.{SimulatedAnnealingStepMode, SimulatedAnnealingStrategy}
import org.scalatest.funsuite.AnyFunSuite

import scala.util.Random

class SimulatedAnnealingStrategySuite extends AnyFunSuite {

  test("doOptimization requires positive finite fitnessRange") {
    val rnd = new Random(0)
    val optimizee = new SaQuadraticOptimizee(target = 0.0)
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
    val optimizee = new SaQuadraticOptimizee(target = 1.0)
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

  test("default config uses AdaptiveAcceptance") {
    val sa = new SimulatedAnnealingStrategy(new SaQuadraticOptimizee(0.0), new Random(1))
    assert(sa.getConfig.stepMode.isInstanceOf[SimulatedAnnealingStepMode.AdaptiveAcceptance])
  }

  test("factory default uses AdaptiveAcceptance") {
    val rnd = new Random(3)
    val optimizee = new SaQuadraticOptimizee(target = 0.0)
    val strategy = SIMULATED_ANNEALING.getStrategy(optimizee, fitnessRange = 100.0, rnd)
    val sa = strategy.asInstanceOf[SimulatedAnnealingStrategy]
    assert(sa.getConfig.stepMode.isInstanceOf[SimulatedAnnealingStepMode.AdaptiveAcceptance])
  }

  test("aggressive adaptive scale does not exceed proposal radius clamp") {
    val rnd = new Random(19)
    val optimizee = new SaQuadraticOptimizee(target = 0.0)
    val sa = new SimulatedAnnealingStrategy(optimizee, rnd)
    sa.setStepMode(
      SimulatedAnnealingStepMode.AdaptiveAcceptance(
        targetRate = 0.01,
        windowProposals = 2,
        scaleMin = 1.0,
        scaleMax = 1e6,
        growFactor = 10.0,
        shrinkFactor = 0.5,
        initialScale = 100.0
      )
    )
    sa.setMaxTemperature(100.0)
    val initial = NumericParameterArray(
      IndexedSeq(DoubleParameter(value = 3.0, minValue = -10.0, maxValue = 10.0, name = "x")),
      rnd = rnd
    )
    val out = sa.doOptimization(initial, fitnessRange = 100.0)
    assert(out.pa != null)
    assert(out.fitness.isFinite)
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
    val compareOpt = new SaBaselineRelativeQuadratic(target = 0.0)
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

  test("budget interrupt mid-run keeps compare-fitness consistent with baseline") {
    val rnd = new Random(29)
    val compareOpt = new SaBaselineRelativeQuadratic(target = 0.0)
    val budgeted = new com.barrybecker4.optimization.optimizee.BudgetedOptimizee(compareOpt, maxEvaluations = 25)
    val sa = new SimulatedAnnealingStrategy(budgeted, rnd)
    sa.setStepMode(SimulatedAnnealingStepMode.Fixed)
    sa.setMaxTemperature(40.0)
    val initial = NumericParameterArray(
      IndexedSeq(DoubleParameter(value = 4.0, minValue = -10.0, maxValue = 10.0, name = "x")),
      rnd = rnd
    )
    val out = sa.doOptimization(initial, fitnessRange = 100.0)
    val expected = compareOpt.compareFitness(out.pa, initial)
    assert(math.abs(out.fitness - expected) < 1.0e-9,
      s"stored fitness ${out.fitness} should match compare vs baseline $expected")
  }
}
