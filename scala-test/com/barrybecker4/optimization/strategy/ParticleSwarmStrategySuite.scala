package com.barrybecker4.optimization.strategy

import com.barrybecker4.optimization.{OptimizationListener, OptimizationStatus, Optimizer}
import com.barrybecker4.optimization.optimizee.{BudgetedOptimizee, Optimizee}
import com.barrybecker4.optimization.optimizee.optimizees.{AckleyProblem, RastriginProblem, RosenbrockProblem, SphereProblem}
import com.barrybecker4.optimization.parameter.{
  NumericParameterArray, ParameterArray, ParameterArrayWithFitness, PermutedParameterArray
}
import com.barrybecker4.optimization.parameter.types.{DoubleParameter, IntegerParameter}
import org.scalatest.funsuite.AnyFunSuite

import scala.collection.mutable.ArrayBuffer
import scala.util.Random

class ParticleSwarmStrategySuite extends AnyFunSuite {

  // Fitness thresholds below were chosen with a wide margin after running the algorithm over hundreds of seeds,
  // so they should hold for any reasonable change to the random stream, not just the seeds used here.

  test("converges on the convex Sphere problem") {
    val problem = SphereProblem(dimensions = 2)
    val strategy = new ParticleSwarmStrategy(problem, new Random(1))

    val solution = strategy.doOptimization(problem.getInitialGuess, problem.getFitnessRange)

    assert(solution.fitness < 1.0e-6, s"Expected near-zero fitness on Sphere, got ${solution.fitness}")
  }

  test("converges on a higher dimensional Sphere problem") {
    val problem = SphereProblem(dimensions = 5)
    val strategy = new ParticleSwarmStrategy(problem, new Random(2))

    val solution = strategy.doOptimization(problem.getInitialGuess, problem.getFitnessRange)

    assert(solution.fitness < 1.0e-6, s"Expected near-zero fitness on 5-D Sphere, got ${solution.fitness}")
  }

  test("finds the narrow curved valley minimum of the Rosenbrock problem") {
    val problem = RosenbrockProblem(dimensions = 2)
    val strategy = new ParticleSwarmStrategy(problem, new Random(3))

    val solution = strategy.doOptimization(problem.getInitialGuess, problem.getFitnessRange)

    assert(solution.fitness < 1.0e-3, s"Expected to reach the (1, 1) minimum on Rosenbrock, got ${solution.fitness}")
    assert(math.abs(solution.pa.get(0).getValue - 1.0) < 0.05)
    assert(math.abs(solution.pa.get(1).getValue - 1.0) < 0.05)
  }

  test("finds the central basin of the Ackley function from far away") {
    val problem = AckleyProblem(dimensions = 2)
    val strategy = new ParticleSwarmStrategy(problem, new Random(4))
    strategy.setConfig(ParticleSwarmConfig(swarmSize = 40, topology = ParticleSwarmTopology.Ring))

    val solution = strategy.doOptimization(problem.getInitialGuess, problem.getFitnessRange)

    assert(solution.fitness < 1.0e-3, s"Expected the Ackley global minimum, got ${solution.fitness}")
  }

  test("ring topology escapes the many local minima of the Rastrigin function") {
    val problem = RastriginProblem(dimensions = 2)
    val strategy = new ParticleSwarmStrategy(problem, new Random(5))
    strategy.setConfig(ParticleSwarmConfig(swarmSize = 40, maxIterations = 400, topology = ParticleSwarmTopology.Ring))

    val solution = strategy.doOptimization(problem.getInitialGuess, problem.getFitnessRange)

    // The best local minima away from the origin have fitness 1.0, so anything below that is the global basin.
    assert(solution.fitness < 0.5, s"Expected the global Rastrigin minimum, got ${solution.fitness}")
  }

  test("the initial guess is part of the swarm, so the result is never worse than it") {
    val problem = RastriginProblem(dimensions = 2)
    val initial = problem.getInitialGuess
    val initialFitness = problem.evaluateFitness(initial)
    val counting = new PsoFunctionOptimizee(problem.evaluateFitness)
    val strategy = new ParticleSwarmStrategy(counting, new Random(6))
    strategy.setConfig(ParticleSwarmConfig(swarmSize = 10, maxIterations = 0))

    val solution = strategy.doOptimization(initial, problem.getFitnessRange)

    assert(solution.fitness <= initialFitness)
    assert(counting.evaluations == 10, "with no iterations, only the initial swarm should be evaluated")
  }

  test("same seed gives the same result and a different seed gives a different one") {
    val problem = RastriginProblem(dimensions = 3)
    val initial = problem.getInitialGuess
    val unconverged = ParticleSwarmConfig(maxIterations = 5, stagnationIterations = 0)

    def run(seed: Int): ParameterArrayWithFitness = {
      val strategy = new ParticleSwarmStrategy(problem, new Random(seed))
      strategy.setConfig(unconverged)
      strategy.doOptimization(initial, problem.getFitnessRange)
    }

    val first = run(42)
    val again = run(42)
    val other = run(43)

    assert(first.fitness == again.fitness)
    assert((0 until first.pa.size).forall(i => first.pa.get(i).getValue == again.pa.get(i).getValue))
    assert(first.fitness != other.fitness)
  }

  test("particles never leave the parameter bounds and settle in the corner a linear objective points to") {
    // minimized at x = 2, y = -1, z = 100: the corner of the box, so particles keep hitting the walls
    val linear = new PsoFunctionOptimizee({ p =>
      (p.get(0).getValue - 2.0) + (-1.0 - p.get(1).getValue) + (100.0 - p.get(2).getValue)
    })
    val initial = doubleArray(new Random(1), (6.0, 2.0, 10.0), (-2.0, -3.0, -1.0), (50.0, 0.0, 100.0))
    val strategy = new ParticleSwarmStrategy(linear, new Random(7))

    val solution = strategy.doOptimization(initial, 200.0)

    assert(linear.seen.nonEmpty)
    for (position <- linear.seen) {
      assert(position(0) >= 2.0 && position(0) <= 10.0, s"x out of bounds: ${position(0)}")
      assert(position(1) >= -3.0 && position(1) <= -1.0, s"y out of bounds: ${position(1)}")
      assert(position(2) >= 0.0 && position(2) <= 100.0, s"z out of bounds: ${position(2)}")
    }
    assert(solution.fitness < 1.0e-9, s"Expected the corner (2, -1, 100), got ${solution.fitness}")
    assert(math.abs(solution.pa.get(0).getValue - 2.0) < 1.0e-6)
    assert(math.abs(solution.pa.get(1).getValue + 1.0) < 1.0e-6)
    assert(math.abs(solution.pa.get(2).getValue - 100.0) < 1.0e-6)
  }

  test("integer parameters only ever take integer values within their range") {
    val target = new PsoFunctionOptimizee({ p =>
      val a = p.get(0).getValue - 7.0
      val b = p.get(1).getValue - 13.0
      a * a + b * b
    })
    val initial = NumericParameterArray(
      IndexedSeq(IntegerParameter(1, 0, 20, "a"), IntegerParameter(1, 0, 20, "b")),
      rnd = new Random(1))
    val strategy = new ParticleSwarmStrategy(target, new Random(8))

    val solution = strategy.doOptimization(initial, 400.0)

    for (position <- target.seen; v <- position) {
      assert(v == math.rint(v) && v >= 0.0 && v <= 20.0, s"expected an integer in [0, 20], got $v")
    }
    assert(solution.fitness == 0.0)
    assert(solution.pa.get(0).getValue == 7.0)
    assert(solution.pa.get(1).getValue == 13.0)
  }

  test("stops when the optimal fitness is reached instead of using the whole iteration budget") {
    val problem = SphereProblem(dimensions = 2)
    val goodEnough = new PsoFunctionOptimizee(problem.evaluateFitness, optimal = 5.0)
    val strategy = new ParticleSwarmStrategy(goodEnough, new Random(9))

    val solution = strategy.doOptimization(problem.getInitialGuess, problem.getFitnessRange)

    assert(solution.fitness <= 5.0)
    assert(goodEnough.evaluations <= 30 * 20,
      s"should stop within a few iterations, but used ${goodEnough.evaluations} evaluations")
  }

  test("stops after the configured number of iterations without improvement") {
    val flat = new PsoFunctionOptimizee(_ => 1.0)
    val strategy = new ParticleSwarmStrategy(flat, new Random(10))
    strategy.setConfig(ParticleSwarmConfig(swarmSize = 10, maxIterations = 1000, stagnationIterations = 5))

    strategy.doOptimization(doubleArray(new Random(1), (0.0, -1.0, 1.0)), 1.0)

    assert(flat.evaluations == 10 * (1 + 5), "the initial swarm plus 5 stagnant iterations")
  }

  test("stops at maxIterations when stagnation detection is disabled") {
    val flat = new PsoFunctionOptimizee(_ => 1.0)
    val strategy = new ParticleSwarmStrategy(flat, new Random(11))
    strategy.setConfig(ParticleSwarmConfig(swarmSize = 10, maxIterations = 7, stagnationIterations = 0))

    strategy.doOptimization(doubleArray(new Random(1), (0.0, -1.0, 1.0)), 1.0)

    assert(flat.evaluations == 10 * (1 + 7), "the initial swarm plus 7 iterations")
  }

  test("returns best-so-far when the evaluation budget runs out") {
    val problem = SphereProblem(dimensions = 2)
    val budgeted = new BudgetedOptimizee(problem, maxEvaluations = 50)
    val strategy = new ParticleSwarmStrategy(budgeted, new Random(12))

    val solution = strategy.doOptimization(problem.getInitialGuess, problem.getFitnessRange)

    assert(budgeted.wasBudgetExhausted)
    assert(budgeted.evaluationsUsed == 50)
    assert(solution.fitness.isFinite)
    assert(solution.fitness == problem.evaluateFitness(solution.pa), "the reported fitness must match its position")
    assert(strategy.bestSoFar.map(_.fitness).contains(solution.fitness))
  }

  test("returns the initial guess with the maximum fitness when no evaluation is allowed") {
    val problem = SphereProblem(dimensions = 2)
    val budgeted = new BudgetedOptimizee(problem, maxEvaluations = 0)
    val strategy = new ParticleSwarmStrategy(budgeted, new Random(13))
    val initial = problem.getInitialGuess

    val solution = strategy.doOptimization(initial, problem.getFitnessRange)

    assert(solution.fitness == Double.MaxValue)
    assert(solution.pa eq initial)
  }

  test("reports a monotonically improving best to the listener, ending with the returned solution") {
    val problem = RosenbrockProblem(dimensions = 2)
    val reported = ArrayBuffer.empty[Double]
    val strategy = new ParticleSwarmStrategy(problem, new Random(14))
    strategy.setListener(new OptimizationListener {
      override def optimizerChanged(params: ParameterArrayWithFitness): Unit = reported += params.fitness
    })

    val solution = strategy.doOptimization(problem.getInitialGuess, problem.getFitnessRange)

    assert(reported.nonEmpty)
    assert(reported.zip(reported.drop(1)).forall { case (before, after) => after < before },
      "each notification should be a strict improvement")
    assert(reported.last == solution.fitness)
  }

  test("a NaN fitness is never taken for an improvement") {
    // NaN for x < 0, so about half of the swarm is evaluated in a region that returns NaN
    val halfNaN = new PsoFunctionOptimizee({ p =>
      val x = p.get(0).getValue
      if (x < 0.0) Double.NaN else x * x
    })
    val initial = doubleArray(new Random(1), (3.0, -5.0, 5.0))
    val strategy = new ParticleSwarmStrategy(halfNaN, new Random(15))

    val solution = strategy.doOptimization(initial, 25.0)

    assert(solution.fitness.isFinite)
    assert(solution.fitness <= 9.0, "never worse than the initial guess at x = 3")
  }

  test("tiny swarms work with every topology") {
    val problem = SphereProblem(dimensions = 2)
    val initialFitness = problem.evaluateFitness(problem.getInitialGuess)
    for (topology <- ParticleSwarmTopology.values; size <- Seq(1, 2, 3)) {
      val strategy = new ParticleSwarmStrategy(problem, new Random(16))
      strategy.setConfig(ParticleSwarmConfig(swarmSize = size, maxIterations = 20, topology = topology))

      val solution = strategy.doOptimization(problem.getInitialGuess, problem.getFitnessRange)

      assert(solution.fitness.isFinite && solution.fitness <= initialFitness,
        s"swarmSize = $size, topology = $topology gave ${solution.fitness}")
    }
  }

  test("rejects an optimizee that evaluates by comparison") {
    val strategy = new ParticleSwarmStrategy(new PermutationIdentityCompareOptimizee, new Random(17))

    intercept[IllegalArgumentException] {
      strategy.doOptimization(doubleArray(new Random(1), (0.0, -1.0, 1.0)), 1.0)
    }
  }

  test("rejects parameter arrays that are not numeric") {
    val strategy = new ParticleSwarmStrategy(SphereProblem(dimensions = 2), new Random(18))

    intercept[IllegalArgumentException] {
      strategy.doOptimization(new PermutedParameterArray(new Random(1)), 1.0)
    }
  }

  test("is available from the OptimizationStrategyType and the Optimizer facade") {
    val problem = SphereProblem(dimensions = 2)
    val optType = OptimizationStrategyType.PARTICLE_SWARM

    assert(OptimizationStrategyType.valuesForUi.contains(optType))
    assert(optType.getDescription.nonEmpty)
    assert(optType.getStrategy(problem, problem.getFitnessRange, new Random(1)).isInstanceOf[ParticleSwarmStrategy])

    val result = new Optimizer(problem).doOptimization(
      optType, problem.getInitialGuess, problem.getFitnessRange, new Random(1))

    assert(result.status != OptimizationStatus.BudgetExhausted)
    assert(result.best.fitness < 1.0e-6, s"Optimizer facade gave ${result.best.fitness}")
  }

  test("the Optimizer facade reports a budget exhausted run with the best-so-far") {
    val problem = SphereProblem(dimensions = 2)

    val result = new Optimizer(problem).doOptimization(
      OptimizationStrategyType.PARTICLE_SWARM, problem.getInitialGuess, problem.getFitnessRange,
      new Random(1), maxEvaluations = Some(100))

    assert(result.status == OptimizationStatus.BudgetExhausted)
    assert(result.evaluationsUsed.contains(100))
    assert(result.best.fitness.isFinite)
  }

  /** @param specs (value, min, max) for each parameter */
  private def doubleArray(rnd: Random, specs: (Double, Double, Double)*): NumericParameterArray =
    NumericParameterArray(
      specs.zipWithIndex.map { case ((value, min, max), i) => DoubleParameter(value, min, max, "x_" + i) }.toIndexedSeq,
      rnd = rnd)
}

/** Wraps a fitness function, counting evaluations and recording every position that was evaluated. */
private final class PsoFunctionOptimizee(fitness: ParameterArray => Double, optimal: Double = 0.0) extends Optimizee {

  var evaluations = 0
  val seen: ArrayBuffer[Array[Double]] = ArrayBuffer.empty

  override def getName: String = "pso-test-function"
  override def evaluateByComparison: Boolean = false

  override def evaluateFitness(params: ParameterArray): Double = {
    evaluations += 1
    seen += Array.tabulate(params.size)(i => params.get(i).getValue)
    fitness(params)
  }

  override def compareFitness(params1: ParameterArray, params2: ParameterArray): Double =
    throw new UnsupportedOperationException

  override def getOptimalFitness: Double = optimal
}