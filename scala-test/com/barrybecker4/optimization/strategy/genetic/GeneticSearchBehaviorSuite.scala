package com.barrybecker4.optimization.strategy.genetic

import com.barrybecker4.optimization.OptimizationDiagnostics
import com.barrybecker4.optimization.optimizee.{BudgetedOptimizee, Optimizee}
import com.barrybecker4.optimization.parameter.types.DoubleParameter
import com.barrybecker4.optimization.parameter.{NumericParameterArray, ParameterArray, ParameterArrayWithFitness}
import com.barrybecker4.optimization.strategy.genetic.optimizees.{BaselineRelativeQuadraticGeneticTestOptimizee, RecordingOptimizee}
import com.barrybecker4.optimization.strategy.optimizees.FlatLandscapeOptimizee
import org.scalatest.funsuite.AnyFunSuite

import scala.collection.mutable.ArrayBuffer
import scala.util.Random

class GeneticSearchBehaviorSuite extends AnyFunSuite {

  private def start(x: Double, rnd: Random) =
    NumericParameterArray(IndexedSeq(DoubleParameter(value = x, minValue = -10.0, maxValue = 10.0, name = "x")), rnd = rnd)

  /** Counts generations via the per-generation trace line. */
  private final class GenerationCounter extends OptimizationDiagnostics {
    var generations = 0
    override def trace(msg: => String): Unit = if (msg.startsWith("currBest")) generations += 1
  }

  private final class QuadraticWithRecord(target: Double) extends Optimizee {
    val evaluated = ArrayBuffer[Double]()
    override def getName: String = "q"
    override def evaluateByComparison: Boolean = false
    override def evaluateFitness(params: ParameterArray): Double = {
      val d = params.get(0).getValue - target
      val f = d * d
      evaluated += f
      f
    }
    override def compareFitness(a: ParameterArray, b: ParameterArray): Double = throw new UnsupportedOperationException
    override def getOptimalFitness: Double = 0.0
  }

  // ---- compare mode ----------------------------------------------------------------------------------------

  test("compare mode converges to the optimum for every seed") {
    for (seed <- 1 to 8) {
      val rnd = new Random(seed)
      val optimizee = new BaselineRelativeQuadraticGeneticTestOptimizee(target = 1.5)
      val strategy = new GeneticSearchStrategy(optimizee, rnd)
      strategy.setImprovementEpsilon(1e-6)
      val result = strategy.doOptimization(start(-4.0, rnd), fitnessRange = 100.0)
      assert(math.abs(result.pa.get(0).getValue - 1.5) < 0.25, s"seed $seed ended at ${result.pa.get(0).getValue}")
    }
  }

  test("compare mode reports the cumulative improvement over the initial guess") {
    val rnd = new Random(3)
    val optimizee = new BaselineRelativeQuadraticGeneticTestOptimizee(target = 1.5)
    val initial = start(-4.0, rnd)
    val strategy = new GeneticSearchStrategy(optimizee, rnd)
    strategy.setImprovementEpsilon(1e-6)
    val result = strategy.doOptimization(initial, fitnessRange = 100.0)
    assert(result.fitness < 0.0)
    assert(math.abs(result.fitness - optimizee.compareFitness(result.pa, initial)) < 1e-9,
      s"fitness ${result.fitness} should equal the score difference vs the initial guess")
  }

  test("compare mode stops when progress stalls instead of running every generation") {
    val rnd = new Random(5)
    val optimizee = new BaselineRelativeQuadraticGeneticTestOptimizee(target = 1.5)
    val counter = new GenerationCounter
    val strategy = new GeneticSearchStrategy(optimizee, rnd)
    strategy.setDiagnostics(counter)
    strategy.setImprovementEpsilon(1e-6)
    strategy.doOptimization(start(-4.0, rnd), fitnessRange = 100.0)
    assert(counter.generations < 40, s"ran ${counter.generations} generations")
  }

  test("compare mode keeps the initial guess when no neighbor improves on it") {
    val rnd = new Random(7)
    val optimizee = new BaselineRelativeQuadraticGeneticTestOptimizee(target = 1.5)
    val initial = start(1.5, rnd) // already optimal
    val strategy = new GeneticSearchStrategy(optimizee, rnd)
    val result = strategy.doOptimization(initial, fitnessRange = 100.0)
    assert(result.pa.get(0).getValue == 1.5)
    assert(result.fitness == 0.0)
  }

  // ---- budget ----------------------------------------------------------------------------------------------

  test("a budget cut part way through the initial batch returns the best candidate evaluated so far") {
    for (budget <- Seq(3, 6, 9)) {
      val rnd = new Random(5)
      val inner = new QuadraticWithRecord(target = 1.5)
      val budgeted = new BudgetedOptimizee(inner, maxEvaluations = budget)
      val result = new GeneticSearchStrategy(budgeted, rnd).doOptimization(start(-4.0, rnd), 100.0)
      assert(budgeted.wasBudgetExhausted)
      assert(result.fitness == inner.evaluated.min, s"budget $budget: returned ${result.fitness}, best seen ${inner.evaluated.min}")
    }
  }

  test("a zero budget returns the initial guess with worst fitness instead of throwing") {
    val rnd = new Random(5)
    val budgeted = new BudgetedOptimizee(new QuadraticWithRecord(1.5), maxEvaluations = 0)
    val result = new GeneticSearchStrategy(budgeted, rnd).doOptimization(start(-4.0, rnd), 100.0)
    assert(result.fitness == Double.MaxValue)
    assert(result.pa.get(0).getValue == -4.0)
  }

  // ---- stopping --------------------------------------------------------------------------------------------

  test("maxIterations is the exact number of generations run") {
    for (limit <- Seq(1, 3, 5)) {
      val rnd = new Random(2)
      val counter = new GenerationCounter
      val strategy = new GeneticSearchStrategy(
        new QuadraticWithRecord(1.5), rnd,
        GeneticSearchConfig(maxIterations = limit, maxStagnantGenerations = 1000))
      strategy.setDiagnostics(counter)
      strategy.doOptimization(start(-4.0, rnd), 100.0)
      assert(counter.generations == limit)
    }
  }

  test("a zero improvement epsilon still stops on a flat landscape") {
    val rnd = new Random(2)
    val counter = new GenerationCounter
    val strategy = new GeneticSearchStrategy(
      new FlatLandscapeOptimizee, rnd, GeneticSearchConfig(improvementEpsilon = 0.0, maxStagnantGenerations = 3))
    strategy.setDiagnostics(counter)
    strategy.doOptimization(start(2.0, rnd), 100.0)
    assert(counter.generations == 4, s"expected to stop after 4 stagnant generations, ran ${counter.generations}")
  }

  test("stops as soon as the optimal fitness is reached") {
    val rnd = new Random(2)
    val counter = new GenerationCounter
    val strategy = new GeneticSearchStrategy(new QuadraticWithRecord(1.5), rnd)
    strategy.setDiagnostics(counter)
    strategy.doOptimization(start(1.5, rnd), 100.0) // seed is already at fitness 0
    assert(counter.generations == 0)
  }

  // ---- evaluations -----------------------------------------------------------------------------------------

  test("greedy mutation does not re-evaluate its parent") {
    val optimizee = new RecordingOptimizee
    val strategy = new GeneticSearchStrategy(
      optimizee, new Random(23), GeneticSearchConfig(crossoverEnabled = false, neighborSelection = NeighborSelection.GreedyImproveAbsolute))
    val parents = Seq((1.0, 2.0), (-1.0, 3.0), (0.5, -2.0)).map { case (a, b) => NumericParameterArraySuiteLike.twoDoubleParams(a, b) }
    val pop = ArrayBuffer(parents.map(p => ParameterArrayWithFitness(p, optimizee.evaluateFitness(p)))*)
    optimizee.evaluateFitnessCalls.clear()
    strategy.refillAfterCullForTesting(pop, keepSize = 3, parents.head, desiredPopSize = 12)
    assert(pop.size == 12)
    assert(!optimizee.evaluateFitnessCalls.exists(call => parents.exists(_ eq call)),
      "the known parent fitness should be reused")
  }

  test("refill never adds duplicates to the population") {
    val optimizee = new RecordingOptimizee
    val strategy = new GeneticSearchStrategy(optimizee, new Random(29))
    val parents = Seq((1.0, 2.0), (-1.0, 3.0), (0.5, -2.0)).map { case (a, b) => NumericParameterArraySuiteLike.twoDoubleParams(a, b) }
    val pop = ArrayBuffer(parents.map(p => ParameterArrayWithFitness(p, optimizee.evaluateFitness(p)))*)
    strategy.refillAfterCullForTesting(pop, keepSize = 3, parents.head, desiredPopSize = 40)
    val keys = pop.map(m => Solutions.key(m.pa))
    assert(keys.distinct.size == pop.size)
  }

  // ---- elitism and keys ------------------------------------------------------------------------------------

  test("elitism replaces the worst member when the population is full") {
    val strategy = new GeneticSearchStrategy(new RecordingOptimizee, new Random(19))
    val pop = ArrayBuffer(
      ParameterArrayWithFitness(NumericParameterArraySuiteLike.twoDoubleParams(5.0, 5.0), 10.0),
      ParameterArrayWithFitness(NumericParameterArraySuiteLike.twoDoubleParams(4.0, 4.0), 8.0),
      ParameterArrayWithFitness(NumericParameterArraySuiteLike.twoDoubleParams(3.0, 3.0), 6.0))
    val champion = ParameterArrayWithFitness(NumericParameterArraySuiteLike.twoDoubleParams(0.0, 0.0), 0.0)
    strategy.setGlobalChampionForTesting(champion)
    strategy.ensureEliteForTesting(pop) // no run has set a desired size, so the population counts as full
    assert(pop.exists(m => Solutions.same(m.pa, champion.pa)))
    assert(pop.size == 3 && !pop.exists(_.fitness == 10.0), "the worst member should have been replaced")
  }

  test("elitism disabled leaves the population alone") {
    val strategy = new GeneticSearchStrategy(new RecordingOptimizee, new Random(19), GeneticSearchConfig(elitismEnabled = false))
    strategy.setGlobalChampionForTesting(ParameterArrayWithFitness(NumericParameterArraySuiteLike.twoDoubleParams(0.0, 0.0), 0.0))
    val pop = ArrayBuffer(ParameterArrayWithFitness(NumericParameterArraySuiteLike.twoDoubleParams(5.0, 5.0), 10.0))
    strategy.ensureEliteForTesting(pop)
    assert(pop.size == 1)
  }

  test("solution keys agree with sameSolution") {
    val a = NumericParameterArraySuiteLike.twoDoubleParams(1.0, 2.0)
    val b = NumericParameterArraySuiteLike.twoDoubleParams(1.0, 2.0)
    val c = NumericParameterArraySuiteLike.twoDoubleParams(1.0, 3.0)
    assert(Solutions.key(a) == Solutions.key(b))
    assert(Solutions.key(a) != Solutions.key(c))
    val pos = NumericParameterArraySuiteLike.twoDoubleParams(0.0, 1.0)
    val neg = NumericParameterArraySuiteLike.twoDoubleParams(-0.0, 1.0)
    assert(Solutions.same(pos, neg))
    assert(Solutions.key(pos) == Solutions.key(neg))
  }

  // ---- same seed ------------------------------------------------------------------------------------------

  test("the same seed gives the same result") {
    def run() = {
      val rnd = new Random(31)
      val strategy = new GeneticSearchStrategy(new QuadraticWithRecord(1.5), rnd)
      strategy.setImprovementEpsilon(1e-6)
      strategy.doOptimization(start(-4.0, rnd), 100.0)
    }
    val (a, b) = (run(), run())
    assert(a.fitness == b.fitness && a.pa.get(0).getValue == b.pa.get(0).getValue)
  }
}
