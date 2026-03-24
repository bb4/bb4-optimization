package com.barrybecker4.optimization.strategy

import com.barrybecker4.math.MathUtil
import com.barrybecker4.optimization.optimizee.Optimizee
import com.barrybecker4.optimization.parameter.{NumericParameterArray, ParameterArray}
import org.scalatest.funsuite.AnyFunSuite

import scala.collection.mutable

/**
  * Regression: initial population must assign fitness via `evaluateFitness(nbr)` for each neighbor,
  * not repeatedly via `evaluateFitness(seedParams)`.
  */
class GeneticSearchStrategySuite extends AnyFunSuite {

  test("findInitialPopulation evaluates fitness of each neighbor candidate") {
    MathUtil.RANDOM.setSeed(42)
    val seed = NumericParameterArraySuiteLike.twoDoubleParams(0.1, 0.2)
    val optimizee = new RecordingOptimizee
    val strategy = new GeneticSearchStrategy(optimizee, MathUtil.RANDOM)

    val pop = strategy.findInitialPopulation(seed)
    assert(pop.nonEmpty)

    val callsForSeed = optimizee.evaluateFitnessCalls.count(_ eq seed)
    assert(callsForSeed == 1,
      s"expected exactly one evaluateFitness(seed); got $callsForSeed")

    val neighborCalls = optimizee.evaluateFitnessCalls.filterNot(_ eq seed)
    assert(neighborCalls.nonEmpty, "expected neighbor evaluations")
    assert(neighborCalls.forall(_ ne seed), "each neighbor fitness must use the neighbor array, not the seed")
  }
}

private[strategy] object NumericParameterArraySuiteLike {
  def twoDoubleParams(a: Double, b: Double): NumericParameterArray = {
    val min = Array(-20.0, -20.0)
    val max = Array(20.0, 20.0)
    new NumericParameterArray(
      Array[Double](a, b),
      min,
      max,
      Array[String]("A", "B"),
      MathUtil.RANDOM)
  }
}

private[strategy] final class RecordingOptimizee extends Optimizee {
  val evaluateFitnessCalls: mutable.Buffer[ParameterArray] = mutable.Buffer.empty

  override def getName: String = "recording"

  override def evaluateByComparison: Boolean = false

  override def evaluateFitness(params: ParameterArray): Double = {
    evaluateFitnessCalls += params
    params.get(0).getValue + params.get(1).getValue
  }

  override def compareFitness(params1: ParameterArray, params2: ParameterArray): Double =
    throw new UnsupportedOperationException

  override def getOptimalFitness: Double = 0.0
}
