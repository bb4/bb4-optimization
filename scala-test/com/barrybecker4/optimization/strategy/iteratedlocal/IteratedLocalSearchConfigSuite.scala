package com.barrybecker4.optimization.strategy.iteratedlocal

import com.barrybecker4.optimization.strategy.DoubleWellOptimizee
import com.barrybecker4.optimization.strategy.iteratedlocal.{IteratedLocalSearchConfig, IteratedLocalSearchStrategy, LocalSearchAcceptance}
import org.scalatest.funsuite.AnyFunSuite

import scala.util.Random

class IteratedLocalSearchConfigSuite extends AnyFunSuite {

  test("the iterated local search preset shakes with a fixed strength") {
    val config = IteratedLocalSearchConfig.IteratedLocalSearch

    assert(config.minShakeLevel == config.maxShakeLevel)
    assert(config.acceptance == LocalSearchAcceptance.BetterOrEqual)
    assert(IteratedLocalSearchConfig.Default == config)
    assert(IteratedLocalSearchConfig() == config)
  }

  test("the variable neighborhood search preset escalates the shake and only accepts improvements") {
    val config = IteratedLocalSearchConfig.VariableNeighborhoodSearch

    assert(config.minShakeLevel == 1)
    assert(config.maxShakeLevel > config.minShakeLevel)
    assert(config.acceptance == LocalSearchAcceptance.Better)
  }

  test("a new strategy starts with the default config and accepts a replacement") {
    val strategy = new IteratedLocalSearchStrategy(new DoubleWellOptimizee, new Random(1))
    assert(strategy.getConfig == IteratedLocalSearchConfig.Default)

    strategy.setConfig(IteratedLocalSearchConfig.VariableNeighborhoodSearch)

    assert(strategy.getConfig == IteratedLocalSearchConfig.VariableNeighborhoodSearch)
  }

  test("rejects a negative iteration count but allows zero") {
    intercept[IllegalArgumentException](IteratedLocalSearchConfig(maxIterations = -1))
    assert(IteratedLocalSearchConfig(maxIterations = 0).maxIterations == 0)
  }

  test("rejects a local radius that is not in (0, 1.5]") {
    intercept[IllegalArgumentException](IteratedLocalSearchConfig(localRadius = 0.0))
    intercept[IllegalArgumentException](IteratedLocalSearchConfig(localRadius = -0.5))
    intercept[IllegalArgumentException](IteratedLocalSearchConfig(localRadius = 1.6))
    intercept[IllegalArgumentException](IteratedLocalSearchConfig(localRadius = Double.NaN))
    assert(IteratedLocalSearchConfig(localRadius = 1.5).localRadius == 1.5)
  }

  test("requires at least one stale neighbor before a local optimum is declared") {
    intercept[IllegalArgumentException](IteratedLocalSearchConfig(maxStaleNeighbors = 0))
    assert(IteratedLocalSearchConfig(maxStaleNeighbors = 1).maxStaleNeighbors == 1)
  }

  test("shake levels start at 1 and must not be reversed") {
    intercept[IllegalArgumentException](IteratedLocalSearchConfig(minShakeLevel = 0))
    intercept[IllegalArgumentException](IteratedLocalSearchConfig(minShakeLevel = 4, maxShakeLevel = 3))
    assert(IteratedLocalSearchConfig(minShakeLevel = 2, maxShakeLevel = 2).maxShakeLevel == 2)
  }

  test("a restart threshold of zero disables restarts but a negative one is rejected") {
    assert(IteratedLocalSearchConfig(restartAfterStagnantIterations = 0).restartAfterStagnantIterations == 0)
    intercept[IllegalArgumentException](IteratedLocalSearchConfig(restartAfterStagnantIterations = -1))
  }

  test("Metropolis acceptance needs a positive finite temperature") {
    intercept[IllegalArgumentException](
      IteratedLocalSearchConfig(acceptance = LocalSearchAcceptance.Metropolis(0.0)))
    intercept[IllegalArgumentException](
      IteratedLocalSearchConfig(acceptance = LocalSearchAcceptance.Metropolis(-1.0)))
    intercept[IllegalArgumentException](
      IteratedLocalSearchConfig(acceptance = LocalSearchAcceptance.Metropolis(Double.PositiveInfinity)))
    val config = IteratedLocalSearchConfig(acceptance = LocalSearchAcceptance.Metropolis(0.01))
    assert(config.acceptance == LocalSearchAcceptance.Metropolis(0.01))
  }

  test("copy keeps unchanged tunables") {
    val tuned = IteratedLocalSearchConfig.VariableNeighborhoodSearch.copy(maxIterations = 10)

    assert(tuned.maxIterations == 10)
    assert(tuned.maxShakeLevel == IteratedLocalSearchConfig.VariableNeighborhoodSearch.maxShakeLevel)
    assert(tuned.acceptance == LocalSearchAcceptance.Better)
  }
}
