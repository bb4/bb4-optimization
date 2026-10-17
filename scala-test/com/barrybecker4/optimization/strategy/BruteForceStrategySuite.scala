package com.barrybecker4.optimization.strategy

import com.barrybecker4.math.MathUtil
import com.barrybecker4.optimization.optimizee.optimizees.{LineGraphDemoProblem, SphereProblem}
import com.barrybecker4.optimization.parameter.PermutedParameterArray
import com.barrybecker4.optimization.parameter.types.IntegerParameter
import org.scalatest.funsuite.AnyFunSuite

import scala.util.Random

class BruteForceStrategySuite extends AnyFunSuite {

  test("brute force finds identity permutation under compareFitness") {
    MathUtil.RANDOM.setSeed(7)
    val rnd = new Random(7)
    val optimizee = new PermutationIdentityCompareOptimizee
    val seed = PermutedParameterArray(IndexedSeq(
      new IntegerParameter(0, 0, 2, "a"),
      new IntegerParameter(1, 0, 2, "b"),
      new IntegerParameter(2, 0, 2, "c")
    ), rnd)
    val strategy = new BruteForceStrategy(optimizee)
    val result = strategy.doOptimization(seed, 100.0)
    assertResult(0.0)(result.fitness)
  }

  test("brute force finds the integer line optimum") {
    val problem = new LineGraphDemoProblem
    val strategy = new BruteForceStrategy(problem)
    val result = strategy.doOptimization(problem.getInitialGuess, problem.getFitnessRange)
    assert(result.fitness == 0.0, s"expected the target on the line, got ${result.fitness}")
  }

  test("brute force refuses a continuous parameter space") {
    val problem = SphereProblem(dimensions = 2)
    val strategy = new BruteForceStrategy(problem)
    intercept[IllegalArgumentException] {
      strategy.doOptimization(problem.getInitialGuess, problem.getFitnessRange)
    }
  }
}
