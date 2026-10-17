package com.barrybecker4.optimization.strategy

import com.barrybecker4.optimization.optimizee.Optimizee
import com.barrybecker4.optimization.optimizee.optimizees.{DiscreteGrid2DDemoProblem, LineGraphDemoProblem}
import com.barrybecker4.optimization.parameter.{NumericParameterArray, ParameterArray}
import com.barrybecker4.optimization.parameter.types.DoubleParameter
import org.scalatest.funsuite.AnyFunSuite

import scala.util.Random

class GlobalHillClimbingStrategySuite extends AnyFunSuite {

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
