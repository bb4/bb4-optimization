package com.barrybecker4.optimization.strategy.hillclimbing

import org.scalatest.funsuite.AnyFunSuite

class GlobalHillClimbingConfigSuite extends AnyFunSuite {

  test("the defaults are valid") {
    val cfg = GlobalHillClimbingConfig()
    assert(cfg.multistartCount == GlobalHillClimbingStrategy.DEFAULT_MULTISTART_COUNT)
    assert(cfg.globalSampleBudget == GlobalHillClimbingStrategy.DEFAULT_GLOBAL_SAMPLE_BUDGET)
  }

  test("a multistart count below one is rejected") {
    assertThrows[IllegalArgumentException](GlobalHillClimbingConfig(multistartCount = 0))
    assertThrows[IllegalArgumentException](GlobalHillClimbingConfig(multistartCount = -2))
  }

  test("a non-positive global sample budget is rejected") {
    assertThrows[IllegalArgumentException](GlobalHillClimbingConfig(globalSampleBudget = 0))
  }

  test("a negative minimum seed separation is rejected") {
    assertThrows[IllegalArgumentException](
      GlobalHillClimbingConfig(seedSelection = SeedSelection.DiverseFromTopM(10, -0.1)))
  }

  test("the strategy setters validate through the config") {
    val strategy = new GlobalHillClimbingStrategy(new UnimodalQuadraticOptimizee(0.0))
    strategy.setMultistartCount(7)
    strategy.setGlobalSampleBudget(33)
    assert(strategy.getConfig.multistartCount == 7)
    assert(strategy.getConfig.globalSampleBudget == 33)
    assertThrows[IllegalArgumentException](strategy.setMultistartCount(0))
    assertThrows[IllegalArgumentException](strategy.setGlobalSampleBudget(0))
    assert(strategy.getConfig.multistartCount == 7, "a rejected value must leave the config unchanged")
  }

  test("effectiveSeedSelection fills in topM from the multistart count when topM <= 0") {
    val cfg = GlobalHillClimbingConfig(multistartCount = 8, seedSelection = SeedSelection.DiverseFromTopM(0, 0.2))
    assert(cfg.effectiveSeedSelection == SeedSelection.DiverseFromTopM(80, 0.2))
    assert(cfg.copy(multistartCount = 2).effectiveSeedSelection == SeedSelection.DiverseFromTopM(50, 0.2))
    assert(cfg.copy(seedSelection = SeedSelection.TopFitness).effectiveSeedSelection == SeedSelection.TopFitness)
  }

  test("an explicit positive topM is kept") {
    val cfg = GlobalHillClimbingConfig(seedSelection = SeedSelection.DiverseFromTopM(12, 0.3))
    assert(cfg.effectiveSeedSelection == SeedSelection.DiverseFromTopM(12, 0.3))
  }

  test("the default diverse selection scales with the multistart count but never drops below 50") {
    assert(SeedSelection.defaultDiverseForMultistart(2) == SeedSelection.DiverseFromTopM(50, 0.15))
    assert(SeedSelection.defaultDiverseForMultistart(20) == SeedSelection.DiverseFromTopM(200, 0.15))
  }
}
