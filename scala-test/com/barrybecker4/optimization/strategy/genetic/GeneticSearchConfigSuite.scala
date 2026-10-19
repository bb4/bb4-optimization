package com.barrybecker4.optimization.strategy.genetic

import com.barrybecker4.optimization.strategy.genetic.optimizees.RecordingOptimizee
import org.scalatest.funsuite.AnyFunSuite

import scala.util.Random

class GeneticSearchConfigSuite extends AnyFunSuite {

  test("the default config is valid") {
    GeneticSearchConfig()
  }

  test("cull factor and crossover probability must be probabilities") {
    for (bad <- Seq(-0.1, 1.1, Double.NaN)) {
      assertThrows[IllegalArgumentException](GeneticSearchConfig(cullFactor = bad))
      assertThrows[IllegalArgumentException](GeneticSearchConfig(crossoverProbability = bad))
    }
  }

  test("a cull factor of 1 still keeps one member") {
    GeneticSearchConfig(cullFactor = 1.0)
  }

  test("counts and radii are validated") {
    assertThrows[IllegalArgumentException](GeneticSearchConfig(maxIterations = -1))
    assertThrows[IllegalArgumentException](GeneticSearchConfig(maxStagnantGenerations = -1))
    assertThrows[IllegalArgumentException](GeneticSearchConfig(maxNeighborRetries = -1))
    assertThrows[IllegalArgumentException](GeneticSearchConfig(nbrRadius = 0.0))
    assertThrows[IllegalArgumentException](GeneticSearchConfig(initialNeighborRadius = -1.0))
    assertThrows[IllegalArgumentException](GeneticSearchConfig(nbrRadiusShrinkFactor = 0.0))
    assertThrows[IllegalArgumentException](GeneticSearchConfig(nbrRadiusExpandFactor = -1.0))
  }

  test("the radius softener is a divisor and must be positive") {
    assertThrows[IllegalArgumentException](GeneticSearchConfig(nbrRadiusSoftener = 0.0))
  }

  test("improvement epsilon must not be negative") {
    assertThrows[IllegalArgumentException](GeneticSearchConfig(improvementEpsilon = -1e-9))
    val strategy = new GeneticSearchStrategy(new RecordingOptimizee, new Random(1))
    assertThrows[IllegalArgumentException](strategy.setImprovementEpsilon(-1.0))
    strategy.setImprovementEpsilon(0.0)
    assert(strategy.getConfig.improvementEpsilon == 0.0)
  }

  test("setConfig replaces the config") {
    val strategy = new GeneticSearchStrategy(new RecordingOptimizee, new Random(1))
    val cfg = GeneticSearchConfig(maxIterations = 7)
    strategy.setConfig(cfg)
    assert(strategy.getConfig == cfg)
  }
}
