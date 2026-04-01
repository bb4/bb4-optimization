package com.barrybecker4.optimization.strategy

import com.barrybecker4.math.MathUtil
import com.barrybecker4.optimization.parameter.PermutedParameterArray
import com.barrybecker4.optimization.parameter.types.IntegerParameter
import org.scalatest.funsuite.AnyFunSuite

import scala.util.Random

class ConcurrentGeneticSearchStrategySuite extends AnyFunSuite {

  test("concurrent genetic completes in compare mode on tiny permutation space") {
    MathUtil.RANDOM.setSeed(42)
    val rnd = new Random(42)
    val optimizee = new PermutationIdentityCompareOptimizee
    val seed = PermutedParameterArray(IndexedSeq(
      new IntegerParameter(2, 0, 2, "a"),
      new IntegerParameter(1, 0, 2, "b"),
      new IntegerParameter(0, 0, 2, "c")
    ), rnd)
    val strategy = new ConcurrentGeneticSearchStrategy(optimizee, rnd)
    val result = strategy.doOptimization(seed, 50.0)
    assert(result.fitness >= 0.0)
    assert(result.pa != null)
  }
}
