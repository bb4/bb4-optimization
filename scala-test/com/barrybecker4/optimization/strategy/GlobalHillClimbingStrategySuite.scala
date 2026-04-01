package com.barrybecker4.optimization.strategy

import com.barrybecker4.optimization.optimizee.Optimizee
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
