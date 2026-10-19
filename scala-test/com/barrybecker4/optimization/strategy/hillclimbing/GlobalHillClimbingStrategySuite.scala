package com.barrybecker4.optimization.strategy.hillclimbing

import com.barrybecker4.optimization.optimizee.{BudgetedOptimizee, Optimizee}
import com.barrybecker4.optimization.strategy.hillclimbing.optimizees.BaselineRelativeQuadraticCompareOptimizee
import com.barrybecker4.optimization.optimizee.optimizees.{DiscreteGrid2DDemoProblem, LineGraphDemoProblem}
import com.barrybecker4.optimization.parameter.types.DoubleParameter
import com.barrybecker4.optimization.parameter.{NumericParameterArray, ParameterArray}
import com.barrybecker4.optimization.strategy.hillclimbing.{GlobalHillClimbingConfig, GlobalHillClimbingStrategy, SeedSelection, UnimodalQuadraticOptimizee}
import com.barrybecker4.optimization.strategy.optimizees.DoubleWellOptimizee
import org.scalatest.funsuite.AnyFunSuite

import scala.util.Random

class GlobalHillClimbingStrategySuite extends AnyFunSuite {

  private def twoDim(rnd: Random) = NumericParameterArray(
    IndexedSeq(
      DoubleParameter(value = 7.0, minValue = -10.0, maxValue = 10.0, name = "x"),
      DoubleParameter(value = -6.0, minValue = -10.0, maxValue = 10.0, name = "y")),
    rnd = rnd)

  test("multistart is reproducible: the same seed gives the same result every time") {
    def run() = {
      val rnd = new Random(5)
      val strategy = new GlobalHillClimbingStrategy(new com.barrybecker4.optimization.strategy.optimizees.TwoMinima2DOptimizee)
      strategy.setConfig(GlobalHillClimbingConfig(multistartCount = 3, globalSampleBudget = 200))
      strategy.doOptimization(twoDim(rnd), fitnessRange = 100.0).fitness
    }
    val results = (1 to 4).map(_ => run())
    assert(results.distinct.size == 1, s"expected identical results, got $results")
  }

  test("an evaluation budget of zero returns the start with worst fitness instead of throwing") {
    val rnd = new Random(3)
    val budgeted = new com.barrybecker4.optimization.optimizee.BudgetedOptimizee(new DoubleWellOptimizee(), maxEvaluations = 0)
    val out = new GlobalHillClimbingStrategy(budgeted).doOptimization(twoDim(rnd), fitnessRange = 100.0)
    assert(out.fitness == Double.MaxValue)
  }

  test("multistart with K>1 is not worse than K=1 on a unimodal quadratic") {
    val rnd = new Random(11)
    val optimizee = new UnimodalQuadraticOptimizee(target = 0.35)
    val initial = NumericParameterArray(
      IndexedSeq(DoubleParameter(value = -4.0, minValue = -10.0, maxValue = 10.0, name = "x")),
      rnd = rnd
    )
    val fitnessRange = 100.0

    val single = new GlobalHillClimbingStrategy(optimizee)
    single.setMultistartCount(1)
    val r1 = single.doOptimization(initial, fitnessRange)

    val multi = new GlobalHillClimbingStrategy(optimizee)
    multi.setMultistartCount(5)
    val r2 = multi.doOptimization(initial, fitnessRange)

    assert(r2.fitness <= r1.fitness + 1.0e-9,
      s"multistart should not regress vs single-seed; got r1=${r1.fitness} r2=${r2.fitness}")
    assert(r2.fitness < 1.0e-4)
  }

  test("diverse multistart reaches lower fitness than fitness-only seeds on double well") {
    val rnd = new Random(17)
    val optimizee = new DoubleWellOptimizee()
    val initial = NumericParameterArray(
      IndexedSeq(DoubleParameter(value = 8.0, minValue = -10.0, maxValue = 10.0, name = "x")),
      rnd = rnd
    )
    val fitnessRange = 100.0

    val topOnly = new GlobalHillClimbingStrategy(optimizee)
    topOnly.setConfig(
      GlobalHillClimbingConfig(
        multistartCount = 2,
        globalSampleBudget = 100,
        seedSelection = SeedSelection.TopFitness
      )
    )

    val diverse = new GlobalHillClimbingStrategy(optimizee)
    diverse.setConfig(
      GlobalHillClimbingConfig(
        multistartCount = 2,
        globalSampleBudget = 100,
        seedSelection = SeedSelection.DiverseFromTopM(topM = 50, minSeparation = 0.15)
      )
    )

    val rTop = topOnly.doOptimization(initial, fitnessRange)
    val rDiv = diverse.doOptimization(initial, fitnessRange)

    assert(rDiv.fitness <= rTop.fitness + 1.0e-9,
      s"diverse should not lose to top-fitness; top=${rTop.fitness} diverse=${rDiv.fitness}")
    assert(rDiv.fitness < 1.0, s"expected near-global minimum, got ${rDiv.fitness}")
  }

  private def oneDim(x: Double, rnd: Random) = NumericParameterArray(
    IndexedSeq(DoubleParameter(value = x, minValue = -10.0, maxValue = 10.0, name = "x")), rnd = rnd)

  test("comparison mode: the result is scored against the initial guess, whichever seed it came from") {
    for (seed <- 1 to 4) {
      val rnd = new Random(seed)
      val optimizee = new BaselineRelativeQuadraticCompareOptimizee(target = 1.5)
      val initial = oneDim(-4.0, rnd)
      val strategy = new GlobalHillClimbingStrategy(optimizee)
      strategy.setConfig(GlobalHillClimbingConfig(multistartCount = 3, globalSampleBudget = 50))
      val result = strategy.doOptimization(initial, fitnessRange = 100.0)
      val expected = optimizee.compareFitness(result.pa, initial)
      assert(math.abs(result.fitness - expected) < 1.0e-9, s"seed $seed: fitness ${result.fitness} but vs initial it is $expected")
      assert(result.fitness < -1.0, s"seed $seed: expected a big improvement over x=-4, got ${result.fitness}")
    }
  }

  test("comparison mode: the best of several seeds wins using one common baseline") {
    val rnd = new Random(21)
    val optimizee = new BaselineRelativeQuadraticCompareOptimizee(target = 1.5)
    val initial = oneDim(-4.0, rnd)
    val strategy = new GlobalHillClimbingStrategy(optimizee)
    strategy.setConfig(GlobalHillClimbingConfig(multistartCount = 4, globalSampleBudget = 40,
      seedSelection = SeedSelection.TopFitness))
    val result = strategy.doOptimization(initial, fitnessRange = 100.0)
    // The global optimum scores (-4-1.5)^2 = 30.25 better than the start.
    assert(result.fitness < -30.0, s"expected near -30.25, got ${result.fitness}")
  }

  test("comparison mode with a zero budget returns the initial guess scored zero against itself") {
    val rnd = new Random(3)
    val budgeted = new BudgetedOptimizee(new BaselineRelativeQuadraticCompareOptimizee(1.5), maxEvaluations = 0)
    val out = new GlobalHillClimbingStrategy(budgeted).doOptimization(oneDim(-4.0, rnd), fitnessRange = 100.0)
    assert(out.fitness == 0.0, s"expected 0.0, got ${out.fitness}")
  }

  test("a budget that runs out part way through the local climbs still returns the best point evaluated") {
    for (budget <- Seq(30, 60, 90)) {
      val rnd = new Random(budget)
      val inner = new UnimodalQuadraticOptimizee(target = 0.35)
      val budgeted = new BudgetedOptimizee(inner, maxEvaluations = budget)
      val strategy = new GlobalHillClimbingStrategy(budgeted)
      strategy.setConfig(GlobalHillClimbingConfig(multistartCount = 3, globalSampleBudget = 20))
      val result = strategy.doOptimization(oneDim(-4.0, rnd), fitnessRange = 100.0)
      assert(budgeted.wasBudgetExhausted, s"budget $budget should have been exhausted")
      assert(result.fitness < 16.0, s"budget $budget: expected better than the global samples' worst, got ${result.fitness}")
      val check = (result.pa.get(0).getValue - 0.35) * (result.pa.get(0).getValue - 0.35)
      assert(math.abs(result.fitness - check) < 1.0e-9, "returned fitness must belong to the returned point")
    }
  }

  test("integer line and grid searches finish at the optimum") {
    val line = new LineGraphDemoProblem
    val lineResult = new GlobalHillClimbingStrategy(line).doOptimization(line.getInitialGuess, line.getFitnessRange)
    assert(lineResult.fitness == 0.0, s"line fitness ${lineResult.fitness}")

    val grid = new DiscreteGrid2DDemoProblem
    val gridResult = new GlobalHillClimbingStrategy(grid).doOptimization(grid.getInitialGuess, grid.getFitnessRange)
    assert(gridResult.fitness == 0.0, s"grid fitness ${gridResult.fitness}")
  }
}

private final class UnimodalQuadraticOptimizee(target: Double) extends Optimizee {
  override def getName: String = "unimodal-quadratic"
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
