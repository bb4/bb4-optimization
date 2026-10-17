package com.barrybecker4.optimization.strategy

import org.scalatest.funsuite.AnyFunSuite

import scala.util.Random

class ParticleSwarmConfigSuite extends AnyFunSuite {

  test("the default config uses the stable constriction coefficients and a global topology") {
    val config = ParticleSwarmConfig.Default

    assert(config == ParticleSwarmConfig())
    assert(config.inertiaWeight == 0.7298)
    assert(config.cognitiveWeight == config.socialWeight)
    assert(config.topology == ParticleSwarmTopology.Global)
    assert(config.swarmSize > 1 && config.maxIterations > 0)
  }

  test("a new strategy starts with the default config and accepts a replacement") {
    val strategy = new ParticleSwarmStrategy(
      new DoubleWellOptimizee, new Random(1))
    assert(strategy.getConfig == ParticleSwarmConfig.Default)

    val custom = ParticleSwarmConfig(swarmSize = 7, topology = ParticleSwarmTopology.Ring)
    strategy.setConfig(custom)

    assert(strategy.getConfig == custom)
  }

  test("copy keeps unchanged tunables") {
    val tuned = ParticleSwarmConfig.Default.copy(swarmSize = 12)

    assert(tuned.swarmSize == 12)
    assert(tuned.maxIterations == ParticleSwarmConfig.Default.maxIterations)
    assert(tuned.topology == ParticleSwarmConfig.Default.topology)
  }

  test("rejects a swarm without particles") {
    intercept[IllegalArgumentException](ParticleSwarmConfig(swarmSize = 0))
    intercept[IllegalArgumentException](ParticleSwarmConfig(swarmSize = -3))
  }

  test("rejects a negative iteration limit but allows zero") {
    intercept[IllegalArgumentException](ParticleSwarmConfig(maxIterations = -1))
    assert(ParticleSwarmConfig(maxIterations = 0).maxIterations == 0)
  }

  test("rejects negative or non-finite weights") {
    intercept[IllegalArgumentException](ParticleSwarmConfig(inertiaWeight = -0.1))
    intercept[IllegalArgumentException](ParticleSwarmConfig(cognitiveWeight = -1.0))
    intercept[IllegalArgumentException](ParticleSwarmConfig(socialWeight = Double.NaN))
    intercept[IllegalArgumentException](ParticleSwarmConfig(inertiaWeight = Double.PositiveInfinity))
  }

  test("rejects velocity fractions that would freeze or invert the swarm") {
    intercept[IllegalArgumentException](ParticleSwarmConfig(maxVelocityFraction = 0.0))
    intercept[IllegalArgumentException](ParticleSwarmConfig(maxVelocityFraction = -0.5))
    intercept[IllegalArgumentException](ParticleSwarmConfig(initialVelocityFraction = -0.1))
    assert(ParticleSwarmConfig(initialVelocityFraction = 0.0).initialVelocityFraction == 0.0)
  }

  test("rejects a negative or non-finite stagnation tolerance") {
    intercept[IllegalArgumentException](ParticleSwarmConfig(stagnationTolerance = -1.0e-3))
    intercept[IllegalArgumentException](ParticleSwarmConfig(stagnationTolerance = Double.NaN))
  }

  test("a non-positive stagnationIterations is allowed and means stagnation detection is off") {
    assert(ParticleSwarmConfig(stagnationIterations = 0).stagnationIterations == 0)
    assert(ParticleSwarmConfig(stagnationIterations = -1).stagnationIterations == -1)
  }
}