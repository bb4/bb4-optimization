package com.barrybecker4.optimization.strategy.iteratedlocal

import com.barrybecker4.optimization.optimizee.optimizees.problems.{SubsetSumProblem, TYPICAL_SS}
import com.barrybecker4.optimization.optimizee.{BudgetedOptimizee, Optimizee}
import com.barrybecker4.optimization.parameter.types.IntegerParameter
import com.barrybecker4.optimization.parameter.{ParameterArray, ParameterArrayWithFitness, PermutedParameterArray}
import com.barrybecker4.optimization.strategy.iteratedlocal.IteratedLocalSearchStrategy.strategyFor
import com.barrybecker4.optimization.strategy.iteratedlocal.{IteratedLocalSearchConfig, IteratedLocalSearchStrategy, LocalSearchAcceptance}
import com.barrybecker4.optimization.strategy.OptimizationStrategyType
import com.barrybecker4.optimization.{OptimizationListener, OptimizationStatus, Optimizer}
import org.scalatest.funsuite.AnyFunSuite

import scala.collection.mutable.ArrayBuffer
import scala.util.Random

class IteratedLocalSearchStrategySuite extends AnyFunSuite {

  // Fitness thresholds below were chosen with a wide margin after running the algorithm over hundreds of seeds
  // (the discrete neighbor operators were ported exactly), so they should not depend on the exact random stream.

  /** Which slot of a regular 14-gon each city sits on, so the optimal tour visits the slots in order. */
  private val CircleSlots = Vector(10, 5, 0, 1, 9, 4, 6, 2, 3, 12, 11, 13, 8, 7)
  private val CircleFitnessRange = 140.0

  private def identityTour(size: Int, rnd: Random): PermutedParameterArray =
    PermutedParameterArray((0 until size).map(i => IntegerParameter(i, 0, size - 1, "p" + i)), rnd)
  

  test("iterated local search finds the optimal tour of a 14 city circle") {
    val tour = new IlsCircleTourOptimizee(CircleSlots)
    val strategy = strategyFor(tour, IteratedLocalSearchConfig.IteratedLocalSearch)

    val solution = strategy.doOptimization(identityTour(14, new Random(2)), CircleFitnessRange)

    assert(solution.fitness < 1.0e-6, s"Expected the optimal tour, but ${solution.fitness} longer")
  }

  test("variable neighborhood search finds the optimal tour of a 14 city circle") {
    val tour = new IlsCircleTourOptimizee(CircleSlots)
    val strategy = strategyFor(tour, IteratedLocalSearchConfig.VariableNeighborhoodSearch)

    val solution = strategy.doOptimization(identityTour(14, new Random(2)), CircleFitnessRange)

    assert(solution.fitness < 1.0e-6, s"Expected the optimal tour, but ${solution.fitness} longer")
  }

  test("is never worse than its own first local search") {
    def run(config: IteratedLocalSearchConfig): ParameterArrayWithFitness =
      strategyFor(new IlsCircleTourOptimizee(CircleSlots), config)
        .doOptimization(identityTour(14, new Random(2)), CircleFitnessRange)

    val firstLocalSearchOnly = run(IteratedLocalSearchConfig.Default.copy(maxIterations = 0))
    val full = run(IteratedLocalSearchConfig.Default)

    assert(full.fitness <= firstLocalSearchOnly.fitness)
  }

  test("finds a subset of the typical subset sum problem that adds up to zero, with both presets") {
    val presets = Seq(
      "iterated local search" -> IteratedLocalSearchConfig.IteratedLocalSearch,
      "variable neighborhood search" -> IteratedLocalSearchConfig.VariableNeighborhoodSearch
    )
    for ((name, config) <- presets) {
      val problem = new SubsetSumProblem(TYPICAL_SS)
      val strategy = strategyFor(problem, config)

      val solution = strategy.doOptimization(problem.getInitialGuess, problem.getFitnessRange)

      val members = (0 until solution.pa.size).map(i => solution.pa.get(i).getValue.toInt)
      assert(solution.fitness == 0.0, s"$name: expected a zero sum subset, got fitness ${solution.fitness}")
      assert(members.nonEmpty && members.sum == 0, s"$name: expected a non-empty subset that sums to 0: $members")
    }
  }

  test("in comparison mode the fitness is relative to the initial guess and the sorted permutation is found") {
    val optimizee = new IlsSortednessCompareOptimizee
    val initial = PermutedParameterArray(
      (0 until 7).map(i => IntegerParameter(6 - i, 0, 6, "p" + i)), new Random(3))
    val strategy = strategyFor(optimizee, IteratedLocalSearchConfig.Default)

    val solution = strategy.doOptimization(initial, 10.0)

    assert(solution.fitness == optimizee.compareFitness(solution.pa, initial),
      "reported fitness must be relative to the initial guess")
    assert(solution.fitness == -24.0, s"expected the sorted order (24 better than reversed), got ${solution.fitness}")
  }

  test("never evaluates the same solution twice") {
    val flat = new IlsRecordingOptimizee(_ => 1.0)
    val initial = identityTour(5, new Random(4))

    strategyFor(flat, IteratedLocalSearchConfig.Default).doOptimization(initial, 1.0)

    assert(flat.evaluated.head == Vector(0.0, 1.0, 2.0, 3.0, 4.0), "the initial guess is evaluated first")
    assert(flat.evaluated.distinct.size == flat.evaluations, "a solution was evaluated more than once")
    assert(flat.evaluations <= 120, "there are only 5! = 120 distinct permutations")
  }

  test("terminates on a problem with a single possible solution") {
    val single = new IlsRecordingOptimizee(_ => 3.0)
    val initial = identityTour(1, new Random(5))

    val solution = strategyFor(single, IteratedLocalSearchConfig.Default).doOptimization(initial, 1.0)

    assert(solution.fitness == 3.0)
    assert(single.evaluations == 1)
  }

  test("with no iterations only the first local search runs") {
    val tour = new IlsCircleTourOptimizee(CircleSlots)
    val initial = identityTour(14, new Random(2))
    val initialFitness = tour.evaluateFitness(initial)

    val solution = strategyFor(tour, IteratedLocalSearchConfig.Default.copy(maxIterations = 0))
      .doOptimization(initial, CircleFitnessRange)

    assert(solution.fitness < initialFitness, "the first local search should improve a bad starting tour")
  }

  test("stops as soon as the optimal fitness is reached instead of using all of its iterations") {
    val tour = new IlsCircleTourOptimizee(CircleSlots)
    val strategy = strategyFor(tour, IteratedLocalSearchConfig.Default)

    val solution = strategy.doOptimization(identityTour(14, new Random(2)), CircleFitnessRange)

    assert(solution.fitness < 1.0e-6)
    // a run that keeps going for all 200 iterations needs about 34000 evaluations
    assert(tour.evaluations <= 6000, s"used ${tour.evaluations} evaluations")
  }

  test("every acceptance rule returns a result that is no worse than the initial guess") {
    val acceptanceRules = Seq(
      LocalSearchAcceptance.Better,
      LocalSearchAcceptance.BetterOrEqual,
      LocalSearchAcceptance.Metropolis(0.05)
    )
    for (acceptance <- acceptanceRules) {
      val tour = new IlsCircleTourOptimizee(CircleSlots)
      val initial = identityTour(14, new Random(2))
      val initialFitness = tour.evaluateFitness(initial)
      val strategy = strategyFor(tour, IteratedLocalSearchConfig.Default.copy(acceptance = acceptance))

      val solution = strategy.doOptimization(initial, CircleFitnessRange)

      assert(solution.fitness.isFinite && solution.fitness <= initialFitness, s"$acceptance gave ${solution.fitness}")
    }
  }

  test("returns best-so-far when the evaluation budget runs out") {
    val tour = new IlsCircleTourOptimizee(CircleSlots)
    val budgeted = new BudgetedOptimizee(tour, maxEvaluations = 40)
    val strategy = strategyFor(budgeted, IteratedLocalSearchConfig.Default)

    val solution = strategy.doOptimization(identityTour(14, new Random(2)), CircleFitnessRange)

    assert(budgeted.wasBudgetExhausted)
    assert(budgeted.evaluationsUsed == 40)
    assert(solution.fitness.isFinite)
    assert(solution.fitness == tour.evaluateFitness(solution.pa), "the reported fitness must match its solution")
    assert(strategy.bestSoFar.map(_.fitness).contains(solution.fitness))
  }

  test("returns the initial guess with the maximum fitness when no evaluation is allowed") {
    val budgeted = new BudgetedOptimizee(new IlsCircleTourOptimizee(CircleSlots), maxEvaluations = 0)
    val initial = identityTour(14, new Random(2))

    val solution = strategyFor(budgeted, IteratedLocalSearchConfig.Default).doOptimization(initial, CircleFitnessRange)

    assert(solution.fitness == Double.MaxValue)
    assert(solution.pa eq initial)
  }

  test("reports a strictly improving best to the listener, ending with the returned solution") {
    val tour = new IlsCircleTourOptimizee(CircleSlots)
    val reported = ArrayBuffer.empty[Double]
    val strategy = strategyFor(tour, IteratedLocalSearchConfig.Default)
    strategy.setListener(new OptimizationListener {
      override def optimizerChanged(params: ParameterArrayWithFitness): Unit = reported += params.fitness
    })

    val solution = strategy.doOptimization(identityTour(14, new Random(2)), CircleFitnessRange)

    assert(reported.nonEmpty)
    assert(reported.zip(reported.drop(1)).forall { case (before, after) => after < before },
      "each notification should be a strict improvement")
    assert(reported.last == solution.fitness)
  }

  test("both strategy types are available from the OptimizationStrategyType and the Optimizer facade") {
    val types = Seq(
      OptimizationStrategyType.ITERATED_LOCAL_SEARCH -> IteratedLocalSearchConfig.IteratedLocalSearch,
      OptimizationStrategyType.VARIABLE_NEIGHBORHOOD_SEARCH -> IteratedLocalSearchConfig.VariableNeighborhoodSearch
    )
    for ((optType, expectedConfig) <- types) {
      val tour = new IlsCircleTourOptimizee(CircleSlots)

      assert(OptimizationStrategyType.valuesForUi.contains(optType))
      assert(optType.getDescription.nonEmpty)
      optType.getStrategy(tour, CircleFitnessRange, new Random(1)) match {
        case ils: IteratedLocalSearchStrategy => assert(ils.getConfig == expectedConfig)
        case other => fail(s"$optType should create an IteratedLocalSearchStrategy but got $other")
      }

      val result = new Optimizer(tour).doOptimization(
        optType, identityTour(14, new Random(2)), CircleFitnessRange, new Random(1))

      assert(result.status != OptimizationStatus.BudgetExhausted)
      assert(result.best.fitness < 1.0e-6, s"$optType gave ${result.best.fitness}")
    }
  }

  test("the Optimizer facade reports a budget exhausted run with the best-so-far") {
    val tour = new IlsCircleTourOptimizee(CircleSlots)

    val result = new Optimizer(tour).doOptimization(
      OptimizationStrategyType.ITERATED_LOCAL_SEARCH, identityTour(14, new Random(2)), CircleFitnessRange,
      new Random(1), maxEvaluations = Some(20))

    assert(result.status == OptimizationStatus.BudgetExhausted)
    assert(result.evaluationsUsed.contains(20))
    assert(result.best.fitness.isFinite)
  }
}

/** Cities on a circle, so that the best tour visits them in the order of their slots around the circle. */
private final class IlsCircleTourOptimizee(slots: IndexedSeq[Int]) extends Optimizee {

  private val n = slots.length

  private def point(slot: Int): (Double, Double) =
    (10.0 * math.cos(2.0 * math.Pi * slot / n), 10.0 * math.sin(2.0 * math.Pi * slot / n))

  private def distance(a: (Double, Double), b: (Double, Double)): Double =
    math.hypot(a._1 - b._1, a._2 - b._2)

  private val optimalLength: Double = (0 until n).map(k => distance(point(k), point((k + 1) % n))).sum

  var evaluations = 0

  override def getName: String = "circle-tour"
  override def evaluateByComparison: Boolean = false

  /** @return how much longer the tour is than the best possible tour */
  override def evaluateFitness(params: ParameterArray): Double = {
    evaluations += 1
    val cities = Array.tabulate(params.size)(i => params.get(i).getValue.toInt)
    var length = 0.0
    for (i <- cities.indices) {
      length += distance(point(slots(cities(i))), point(slots(cities((i + 1) % n))))
    }
    length - optimalLength
  }

  override def compareFitness(params1: ParameterArray, params2: ParameterArray): Double =
    throw new UnsupportedOperationException

  override def getOptimalFitness: Double = 1.0e-9
}

/** Compare mode: the score is how far each value is from its index, and the result is a score difference. */
private final class IlsSortednessCompareOptimizee extends Optimizee {

  override def getName: String = "sortedness-compare"
  override def evaluateByComparison: Boolean = true

  override def evaluateFitness(params: ParameterArray): Double =
    throw new UnsupportedOperationException

  override def compareFitness(sample: ParameterArray, baseline: ParameterArray): Double =
    score(sample) - score(baseline)

  override def getOptimalFitness: Double = 0.0

  private def score(params: ParameterArray): Double = {
    var sum = 0.0
    for (i <- 0 until params.size) {
      sum += math.abs(params.get(i).getValue - i)
    }
    sum
  }
}

/** Wraps a fitness function and records the values of every solution that was evaluated, in order. */
private final class IlsRecordingOptimizee(fitness: ParameterArray => Double, optimal: Double = 0.0) extends Optimizee {

  val evaluated: ArrayBuffer[Vector[Double]] = ArrayBuffer.empty

  def evaluations: Int = evaluated.size

  override def getName: String = "recording"
  override def evaluateByComparison: Boolean = false

  override def evaluateFitness(params: ParameterArray): Double = {
    evaluated += Vector.tabulate(params.size)(i => params.get(i).getValue)
    fitness(params)
  }

  override def compareFitness(params1: ParameterArray, params2: ParameterArray): Double =
    throw new UnsupportedOperationException

  override def getOptimalFitness: Double = optimal
}
