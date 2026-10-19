package com.barrybecker4.optimization.strategy.annealing

import com.barrybecker4.optimization.strategy.annealing.SimulatedAnnealingStepMode.AdaptiveAcceptance
import com.barrybecker4.optimization.strategy.annealing.optimizees.SaQuadraticOptimizee
import org.scalatest.funsuite.AnyFunSuite

import scala.util.Random

class SimulatedAnnealingConfigSuite extends AnyFunSuite {

  test("default configs are valid") {
    SimulatedAnnealingConfig()
    GlobalSimulatedAnnealingConfig()
    AdaptiveAcceptance()
  }

  test("tempDropFactor must be strictly between 0 and 1 (>= 1 would never cool and loop forever)") {
    for (bad <- Seq(1.0, 1.5, 0.0, -0.5, Double.NaN))
      assertThrows[IllegalArgumentException](SimulatedAnnealingConfig(tempDropFactor = bad))
  }

  test("other cooling-schedule values must be positive") {
    assertThrows[IllegalArgumentException](SimulatedAnnealingConfig(innerIterationsPerDimension = 0))
    assertThrows[IllegalArgumentException](SimulatedAnnealingConfig(numTempIterations = 0))
    assertThrows[IllegalArgumentException](SimulatedAnnealingConfig(defaultTempMax = 0.0))
    assertThrows[IllegalArgumentException](SimulatedAnnealingConfig(radiusMultiplier = -1.0))
  }

  test("AdaptiveAcceptance rejects inconsistent values") {
    assertThrows[IllegalArgumentException](AdaptiveAcceptance(targetRate = 0.0))
    assertThrows[IllegalArgumentException](AdaptiveAcceptance(targetRate = 1.0))
    assertThrows[IllegalArgumentException](AdaptiveAcceptance(scaleMin = 2.0, scaleMax = 1.0))
    assertThrows[IllegalArgumentException](AdaptiveAcceptance(scaleMin = 0.0))
    assertThrows[IllegalArgumentException](AdaptiveAcceptance(growFactor = 0.0))
    assertThrows[IllegalArgumentException](AdaptiveAcceptance(initialScale = 0.0))
  }

  test("setMaxTemperature rejects non-positive and non-finite values") {
    val sa = new SimulatedAnnealingStrategy(new SaQuadraticOptimizee(0.0), new Random(1))
    for (bad <- Seq(0.0, -1.0, Double.NaN, Double.PositiveInfinity))
      assertThrows[IllegalArgumentException](sa.setMaxTemperature(bad))
  }

  test("global config rejects non-positive multistart count and sample budget") {
    assertThrows[IllegalArgumentException](GlobalSimulatedAnnealingConfig(multistartCount = 0))
    assertThrows[IllegalArgumentException](GlobalSimulatedAnnealingConfig(globalSampleBudget = 0))
  }

  test("defaultMaxTemperature is a tenth of the fitness range") {
    assert(SimulatedAnnealingStrategy.defaultMaxTemperature(250.0) == 25.0)
  }
}
