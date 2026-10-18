package com.barrybecker4.optimization.strategy.annealing

import com.barrybecker4.optimization.optimizee.BudgetedOptimizee
import com.barrybecker4.optimization.parameter.types.DoubleParameter
import com.barrybecker4.optimization.parameter.NumericParameterArray
import com.barrybecker4.optimization.strategy.annealing.{GlobalSimulatedAnnealingConfig, GlobalSimulatedAnnealingStrategy}
import com.barrybecker4.optimization.strategy.hillclimbing.SeedSelection
import com.barrybecker4.optimization.strategy.annealing.optimizees.SaGlobalUnimodalOptimizee
import com.barrybecker4.optimization.strategy.optimizees.DoubleWellOptimizee
import org.scalatest.funsuite.AnyFunSuite

import scala.util.Random

class GlobalSimulatedAnnealingStrategySuite extends AnyFunSuite {

  test("multistart with K>1 is not worse than K=1 on a unimodal quadratic") {
    val rnd = new Random(11)
    val unimodal = new SaGlobalUnimodalOptimizee(target = 0.35)
    val initial = NumericParameterArray(
      IndexedSeq(DoubleParameter(value = -4.0, minValue = -10.0, maxValue = 10.0, name = "x")),
      rnd = rnd
    )
    val fitnessRange = 100.0

    val single = new GlobalSimulatedAnnealingStrategy(unimodal, rnd)
    single.setMultistartCount(1)
    val r1 = single.doOptimization(initial, fitnessRange)

    val multi = new GlobalSimulatedAnnealingStrategy(unimodal, new Random(11))
    multi.setMultistartCount(5)
    val r2 = multi.doOptimization(initial, fitnessRange)

    assert(r2.fitness <= r1.fitness + 1.0e-6,
      s"multistart should not regress vs single-seed; got r1=${r1.fitness} r2=${r2.fitness}")
  }

  test("budget exhausted during global sample still returns best-so-far") {
    val rnd = new Random(7)
    val optimizee = new DoubleWellOptimizee()
    val budgeted = new BudgetedOptimizee(optimizee, maxEvaluations = 15)
    val strategy = new GlobalSimulatedAnnealingStrategy(budgeted, rnd)
    strategy.setConfig(
      GlobalSimulatedAnnealingConfig(
        multistartCount = 3,
        globalSampleBudget = 100,
        seedSelection = SeedSelection.TopFitness
      )
    )
    val initial = NumericParameterArray(
      IndexedSeq(DoubleParameter(value = 8.0, minValue = -10.0, maxValue = 10.0, name = "x")),
      rnd = rnd
    )
    val initialFitness = optimizee.evaluateFitness(initial)
    val out = strategy.doOptimization(initial, fitnessRange = 100.0)
    assert(out.fitness.isFinite)
    assert(out.fitness <= initialFitness + 1.0e-9,
      s"expected best-so-far not worse than initial; got ${out.fitness} vs $initialFitness")
  }

  test("diverse multistart reaches lower fitness than fitness-only seeds on double well") {
    val rnd = new Random(17)
    val optimizee = new DoubleWellOptimizee()
    val initial = NumericParameterArray(
      IndexedSeq(DoubleParameter(value = 8.0, minValue = -10.0, maxValue = 10.0, name = "x")),
      rnd = rnd
    )
    val fitnessRange = 100.0

    val topOnly = new GlobalSimulatedAnnealingStrategy(optimizee, rnd)
    topOnly.setConfig(
      GlobalSimulatedAnnealingConfig(
        multistartCount = 2,
        globalSampleBudget = 100,
        seedSelection = SeedSelection.TopFitness
      )
    )

    val diverse = new GlobalSimulatedAnnealingStrategy(optimizee, new Random(17))
    diverse.setConfig(
      GlobalSimulatedAnnealingConfig(
        multistartCount = 2,
        globalSampleBudget = 100,
        seedSelection = SeedSelection.DiverseFromTopM(topM = 50, minSeparation = 0.15)
      )
    )

    val rTop = topOnly.doOptimization(initial, fitnessRange)
    val rDiv = diverse.doOptimization(initial, fitnessRange)

    assert(rDiv.fitness <= rTop.fitness + 1.0e-6,
      s"diverse should not lose to top-fitness; top=${rTop.fitness} diverse=${rDiv.fitness}")
    assert(rDiv.fitness < 1.0, s"expected near-global minimum, got ${rDiv.fitness}")
  }
}
