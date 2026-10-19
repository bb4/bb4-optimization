package com.barrybecker4.optimization.strategy

import com.barrybecker4.optimization.optimizee.BudgetedOptimizee
import com.barrybecker4.optimization.optimizee.optimizees.{AckleyProblem, RastriginProblem, SphereProblem}
import com.barrybecker4.optimization.parameter.{NumericParameterArray, ParameterArrayWithFitness, PermutedParameterArray}
import com.barrybecker4.optimization.parameter.types.{DoubleParameter, IntegerParameter}
import com.barrybecker4.optimization.strategy.optimizees.CompareModeTestOptimizee
import com.barrybecker4.optimization.strategy.particleswarm.optimizees.PsoFunctionOptimizee
import org.scalatest.funsuite.AnyFunSuite

import scala.util.Random

class DifferentialEvolutionStrategySuite extends AnyFunSuite {

  test("Differential Evolution converges on convex Sphere problem") {
    val problem = SphereProblem(dimensions = 2)
    val strategy = DifferentialEvolutionStrategy(problem, populationSize = 30)

    val solution = strategy.doOptimization(problem.getInitialGuess, problem.getFitnessRange)

    assert(solution.fitness <= 1.0e-3,
      s"Expected fitness <= 1e-3, but got ${solution.fitness}")
  }

  test("Differential Evolution escapes local minima on Ackley function") {
    val problem = AckleyProblem(dimensions = 2)
    val strategy = DifferentialEvolutionStrategy(problem, populationSize = 40, fWeight = 0.7, crossoverRate = 0.8)

    val solution = strategy.doOptimization(problem.getInitialGuess, problem.getFitnessRange)

    assert(solution.fitness <= 0.05, s"DE should reach the near-zero basin on Ackley, got ${solution.fitness}")
  }

  test("population smaller than 4 is rejected instead of spinning") {
    val problem = SphereProblem(dimensions = 2)
    intercept[IllegalArgumentException] {
      DifferentialEvolutionStrategy(problem, populationSize = 3)
    }
  }

  private def integers(rnd: Random, n: Int, lo: Int, hi: Int, start: Int) =
    NumericParameterArray(IndexedSeq.tabulate(n)(i => IntegerParameter(start, lo, hi, "i" + i)), rnd = rnd)

  private def doubles(rnd: Random, specs: (Double, Double, Double)*) =
    NumericParameterArray(
      specs.zipWithIndex.map { case ((v, lo, hi), i) => DoubleParameter(v, lo, hi, "x" + i) }.toIndexedSeq, rnd = rnd)

  test("integer parameters are rounded, not truncated, so mutation does not push values toward zero") {
    val pop = 40
    val means = (1 to 100).map { seed =>
      val flat = new PsoFunctionOptimizee(_ => 1.0)
      val strategy = new DifferentialEvolutionStrategy(flat, populationSize = pop, rnd = new Random(seed), maxIterations = 1)
      strategy.doOptimization(integers(new Random(1), n = 2, lo = 0, hi = 10, start = 5), 1.0)
      // on a flat landscape the 40 trials of the one generation are exactly the last 40 evaluations
      val trials = flat.seen.takeRight(pop).flatten
      trials.sum / trials.size
    }
    val mean = means.sum / means.size
    // unbiased rounding keeps the mean of the symmetric range at 5; truncation would pull it down to about 4.6
    assert(math.abs(mean - 5.0) < 0.15, s"mean trial value is $mean")
  }

  test("integer parameters only ever take integer values within their range") {
    val target = new PsoFunctionOptimizee({ p =>
      val a = p.get(0).getValue - 7.0
      val b = p.get(1).getValue - 13.0
      a * a + b * b
    })
    val strategy = new DifferentialEvolutionStrategy(target, populationSize = 20, rnd = new Random(4), maxIterations = 60)

    val solution = strategy.doOptimization(integers(new Random(1), n = 2, lo = 0, hi = 20, start = 1), 400.0)

    for (position <- target.seen; v <- position)
      assert(v == math.rint(v) && v >= 0.0 && v <= 20.0, s"expected an integer in [0, 20], got $v")
    assert(solution.fitness == 0.0, s"expected the integer optimum (7, 13), got ${solution.fitness}")
  }

  test("trial vectors never leave the parameter bounds") {
    val linear = new PsoFunctionOptimizee(p => -p.get(0).getValue - p.get(1).getValue)
    val strategy = new DifferentialEvolutionStrategy(linear, rnd = new Random(5), fWeight = 1.5, maxIterations = 40)

    val solution = strategy.doOptimization(doubles(new Random(1), (0.0, -2.0, 3.0), (1.0, 0.0, 4.0)), 10.0)

    for (position <- linear.seen) {
      assert(position(0) >= -2.0 && position(0) <= 3.0, s"x out of bounds: ${position(0)}")
      assert(position(1) >= 0.0 && position(1) <= 4.0, s"y out of bounds: ${position(1)}")
    }
    assert(solution.fitness < -5.0, s"the corner (3, 4) with fitness -7 is optimal, so expected to get close, got ${solution.fitness}")
  }

  test("the same seed gives the same result") {
    val problem = RastriginProblem(dimensions = 3)
    def run(seed: Int): ParameterArrayWithFitness =
      new DifferentialEvolutionStrategy(problem, rnd = new Random(seed), maxIterations = 15)
        .doOptimization(problem.getInitialGuess, problem.getFitnessRange)

    val (a, b, c) = (run(8), run(8), run(9))
    assert(a.fitness == b.fitness)
    assert(a.fitness != c.fitness)
  }

  test("with no generations the best of the initial population is returned after one evaluation each") {
    val counting = new PsoFunctionOptimizee(p => math.abs(p.get(0).getValue))
    val strategy = new DifferentialEvolutionStrategy(counting, populationSize = 12, rnd = new Random(6), maxIterations = 0)
    val initial = doubles(new Random(1), (9.0, -10.0, 10.0))

    val solution = strategy.doOptimization(initial, 10.0)

    assert(counting.evaluations == 12)
    assert(solution.fitness == counting.seen.map(p => math.abs(p(0))).min)
  }

  test("stops as soon as a generation reaches the optimal fitness") {
    val problem = SphereProblem(dimensions = 2)
    val goodEnough = new PsoFunctionOptimizee(problem.evaluateFitness, optimal = 5.0)
    val strategy = new DifferentialEvolutionStrategy(goodEnough, populationSize = 20, rnd = new Random(7), maxIterations = 1000)

    val solution = strategy.doOptimization(problem.getInitialGuess, problem.getFitnessRange)

    assert(solution.fitness <= 5.0)
    assert(goodEnough.evaluations < 20 * 30, s"should stop long before 1000 generations, used ${goodEnough.evaluations}")
  }

  test("the smallest allowed population of 4 can always find three distinct donors") {
    val problem = SphereProblem(dimensions = 2)
    val strategy = new DifferentialEvolutionStrategy(problem, populationSize = 4, rnd = new Random(10), maxIterations = 50)

    val solution = strategy.doOptimization(problem.getInitialGuess, problem.getFitnessRange)

    assert(solution.fitness.isFinite)
  }

  test("returns the best candidate evaluated when the evaluation budget runs out") {
    val problem = SphereProblem(dimensions = 2)
    for (budget <- Seq(1, 7, 40, 95)) {
      val inner = new PsoFunctionOptimizee(problem.evaluateFitness)
      val budgeted = new BudgetedOptimizee(inner, maxEvaluations = budget)
      val strategy = new DifferentialEvolutionStrategy(budgeted, populationSize = 10, rnd = new Random(11))

      val solution = strategy.doOptimization(problem.getInitialGuess, problem.getFitnessRange)

      assert(budgeted.wasBudgetExhausted, s"budget $budget")
      assert(inner.evaluations == budget)
      assert(solution.fitness == inner.seen.map(p => p.map(v => v * v).sum).min,
        s"budget $budget: must return the best evaluated candidate")
    }
  }

  test("a zero budget returns the initial guess with the worst fitness") {
    val problem = SphereProblem(dimensions = 2)
    val budgeted = new BudgetedOptimizee(problem, maxEvaluations = 0)
    val initial = problem.getInitialGuess

    val solution = new DifferentialEvolutionStrategy(budgeted).doOptimization(initial, problem.getFitnessRange)

    assert(solution.fitness == Double.MaxValue)
    assert(solution.pa eq initial)
  }

  test("a run that is cut off before anything is evaluated does not return the previous run's result") {
    val problem = SphereProblem(dimensions = 2)
    val budgeted = new BudgetedOptimizee(problem, maxEvaluations = 60)
    val strategy = new DifferentialEvolutionStrategy(budgeted, populationSize = 10, rnd = new Random(12))
    val first = strategy.doOptimization(problem.getInitialGuess, problem.getFitnessRange)
    assert(first.fitness < Double.MaxValue && budgeted.wasBudgetExhausted)

    val second = strategy.doOptimization(problem.getInitialGuess, problem.getFitnessRange)

    assert(second.fitness == Double.MaxValue, s"nothing could be evaluated, but got ${second.fitness}")
  }

  test("a NaN fitness for the initial guess does not freeze the search") {
    val nanAtStart = new PsoFunctionOptimizee({ p =>
      val x = p.get(0).getValue
      if (x == 4.0) Double.NaN else x * x
    })
    val strategy = new DifferentialEvolutionStrategy(nanAtStart, populationSize = 10, rnd = new Random(13), maxIterations = 80)

    val solution = strategy.doOptimization(doubles(new Random(1), (4.0, -5.0, 5.0)), 25.0)

    assert(!solution.fitness.isNaN)
    assert(solution.fitness < 1.0e-6, s"expected to find the minimum at 0, got ${solution.fitness}")
  }

  test("the listener hears the initial best and an improvement for each generation") {
    val problem = SphereProblem(dimensions = 2)
    val reported = scala.collection.mutable.ArrayBuffer.empty[Double]
    val strategy = new DifferentialEvolutionStrategy(problem, populationSize = 10, rnd = new Random(14), maxIterations = 5)
    strategy.setListener(new com.barrybecker4.optimization.OptimizationListener {
      override def optimizerChanged(params: ParameterArrayWithFitness): Unit = reported += params.fitness
    })

    val solution = strategy.doOptimization(problem.getInitialGuess, problem.getFitnessRange)

    assert(reported.size == 1 + 5)
    assert(reported.zip(reported.tail).forall { case (before, after) => after <= before })
    assert(reported.last == solution.fitness)
  }

  test("rejects optimizees that evaluate by comparison and arrays that are not numeric") {
    val initial = doubles(new Random(1), (0.0, -1.0, 1.0))
    intercept[IllegalArgumentException] {
      new DifferentialEvolutionStrategy(new CompareModeTestOptimizee).doOptimization(initial, 1.0)
    }
    intercept[IllegalArgumentException] {
      new DifferentialEvolutionStrategy(SphereProblem(dimensions = 2)).doOptimization(new PermutedParameterArray(new Random(1)), 1.0)
    }
  }

  test("constructor rejects settings that make no sense") {
    val problem = SphereProblem(dimensions = 2)
    intercept[IllegalArgumentException] { new DifferentialEvolutionStrategy(problem, crossoverRate = 1.5) }
    intercept[IllegalArgumentException] { new DifferentialEvolutionStrategy(problem, crossoverRate = -0.1) }
    intercept[IllegalArgumentException] { new DifferentialEvolutionStrategy(problem, fWeight = Double.NaN) }
    intercept[IllegalArgumentException] { new DifferentialEvolutionStrategy(problem, maxIterations = -1) }
  }
}
