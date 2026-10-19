package com.barrybecker4.optimization.strategy

import com.barrybecker4.math.MathUtil
import com.barrybecker4.optimization.optimizee.optimizees.{LineGraphDemoProblem, SphereProblem}
import com.barrybecker4.optimization.optimizee.{BudgetedOptimizee, EvaluationBudgetExceededException}
import com.barrybecker4.optimization.parameter.{NumericParameterArray, PermutedParameterArray}
import com.barrybecker4.optimization.parameter.types.IntegerParameter
import com.barrybecker4.optimization.strategy.hillclimbing.optimizees.BaselineRelativeQuadraticCompareOptimizee
import com.barrybecker4.optimization.strategy.particleswarm.optimizees.PsoFunctionOptimizee
import com.barrybecker4.optimization.strategy.optimizees.CompareModeTestOptimizee
import org.scalatest.funsuite.AnyFunSuite

import scala.util.Random

class BruteForceStrategySuite extends AnyFunSuite {

  test("brute force finds identity permutation under compareFitness") {
    MathUtil.RANDOM.setSeed(7)
    val rnd = new Random(7)
    val optimizee = new CompareModeTestOptimizee
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

  private def lattice(bounds: (Int, Int)*): NumericParameterArray =
    NumericParameterArray(
      bounds.zipWithIndex.map { case ((lo, hi), i) => IntegerParameter(lo, lo, hi, "i" + i) }.toIndexedSeq,
      rnd = new Random(1))

  test("every point of the integer lattice is evaluated exactly once") {
    val counting = new PsoFunctionOptimizee(p => 10.0 + p.get(0).getValue * p.get(1).getValue) // never optimal
    val result = new BruteForceStrategy(counting).doOptimization(lattice((-1, 2), (3, 5)), 10.0)

    assert(counting.evaluations == 4 * 3)
    val expected = for (a <- -1 to 2; b <- 3 to 5) yield Seq(a.toDouble, b.toDouble)
    assert(counting.seen.map(_.toSeq).toSet == expected.toSet)
    assert(result.fitness == 10.0 + (-1) * 5, s"the minimum is at (-1, 5), got ${result.fitness}")
  }

  test("stops enumerating as soon as the optimal fitness is reached") {
    val target = new PsoFunctionOptimizee(p => math.abs(p.get(0).getValue - 1.0) + math.abs(p.get(1).getValue - 2.0))
    val result = new BruteForceStrategy(target).doOptimization(lattice((0, 9), (0, 9)), 20.0)

    assert(result.fitness == 0.0)
    assert(target.evaluations == 1 * 10 + 3, "(0,0..9), then (1,0), (1,1), (1,2)")
  }

  test("integer lattice in comparison mode is scored against the initial guess") {
    val optimizee = new BaselineRelativeQuadraticCompareOptimizee(target = 3.0)
    val initial = lattice((0, 6))

    val result = new BruteForceStrategy(optimizee).doOptimization(initial, 10.0)

    assert(result.pa.get(0).getValue == 3.0)
    assert(result.fitness == optimizee.compareFitness(result.pa, initial))
  }

  test("a lattice with too many candidates is refused with an explanation") {
    val problem = new PsoFunctionOptimizee(_ => 1.0)
    val e = intercept[IllegalArgumentException] {
      new BruteForceStrategy(problem).doOptimization(lattice((0, 999), (0, 999), (0, 1)), 1.0)
    }
    assert(e.getMessage.contains("candidates"))
    assert(problem.evaluations == 0, "must refuse before evaluating anything")
  }

  test("an inverted range is reported as such, not as a space that is too big") {
    val e = intercept[IllegalArgumentException] {
      new BruteForceStrategy(new PsoFunctionOptimizee(_ => 1.0)).doOptimization(lattice((5, 2)), 1.0)
    }
    assert(e.getMessage.toLowerCase.contains("empty"), e.getMessage)
  }

  test("the best point evaluated so far is available when the budget runs out") {
    val inner = new PsoFunctionOptimizee(p => 100.0 - p.get(0).getValue)
    val budgeted = new BudgetedOptimizee(inner, maxEvaluations = 6)
    val strategy = new BruteForceStrategy(budgeted)

    intercept[EvaluationBudgetExceededException] { strategy.doOptimization(lattice((0, 20)), 100.0) }

    assert(strategy.bestSoFar.map(_.fitness).contains(95.0), "the first 6 points are 0..5, so 5 is the best")
  }

  test("a permutation space is sampled without repeats and the best sample is returned") {
    val optimizee = new PsoFunctionOptimizee(p => 1.0 + (0 until p.size).map(i => math.abs(p.get(i).getValue - i)).sum)
    val seed = PermutedParameterArray(IndexedSeq(
      new IntegerParameter(2, 0, 2, "a"), new IntegerParameter(1, 0, 2, "b"), new IntegerParameter(0, 0, 2, "c")), new Random(1))

    val result = new BruteForceStrategy(optimizee).doOptimization(seed, 10.0)

    // PermutedGlobalSampler counts a tour and its mirror image as one (n!/2 = 3 samples for 3 parameters)
    assert(optimizee.evaluations == 3)
    assert(optimizee.seen.map(_.toSeq).distinct.size == optimizee.evaluations, "no permutation is evaluated twice")
    val costs = optimizee.seen.map(p => 1.0 + p.zipWithIndex.map { case (v, i) => math.abs(v - i) }.sum)
    assert(result.fitness == costs.min)
  }
}
