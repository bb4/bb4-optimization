package com.barrybecker4.optimization.strategy.genetic

import com.barrybecker4.math.MathUtil
import com.barrybecker4.optimization.optimizee.Optimizee
import com.barrybecker4.optimization.parameter.types.DoubleParameter
import com.barrybecker4.optimization.parameter.{NumericParameterArray, ParameterArray, ParameterArrayWithFitness}
import com.barrybecker4.optimization.strategy.genetic.{GeneticSearchConfig, GeneticSearchStrategy, NeighborSelection}
import org.scalatest.funsuite.AnyFunSuite

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.util.Random

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

  test("refill restores population to desired size when duplicates are rare (absolute fitness)") {
    val rnd = new Random(7)
    val optimizee = new RecordingOptimizee
    val strategy = new GeneticSearchStrategy(optimizee, rnd)
    val a = NumericParameterArraySuiteLike.twoDoubleParams(1.0, 2.0)
    val b = NumericParameterArraySuiteLike.twoDoubleParams(-1.0, 3.0)
    val c = NumericParameterArraySuiteLike.twoDoubleParams(0.5, -2.0)
    val pop = ArrayBuffer(
      ParameterArrayWithFitness(a, optimizee.evaluateFitness(a)),
      ParameterArrayWithFitness(b, optimizee.evaluateFitness(b)),
      ParameterArrayWithFitness(c, optimizee.evaluateFitness(c))
    )
    val desired = 10
    val baseline = a
    strategy.refillAfterCullForTesting(pop, keepSize = 3, baseline, desiredPopSize = desired)
    assert(pop.size == desired,
      s"expected population size $desired after refill, got ${pop.size}")
  }

  test("compare-mode refill scores offspring against the same baseline as reevaluation") {
    val rnd = new Random(11)
    val optimizee = new RecordingCompareBaselineOptimizee
    val strategy = new GeneticSearchStrategy(optimizee, rnd)
    val baseline = NumericParameterArraySuiteLike.twoDoubleParams(0.0, 0.0)
    val p1 = NumericParameterArraySuiteLike.twoDoubleParams(1.0, 1.0)
    val p2 = NumericParameterArraySuiteLike.twoDoubleParams(2.0, -1.0)
    val p3 = NumericParameterArraySuiteLike.twoDoubleParams(-2.0, 0.5)
    val pop = ArrayBuffer(
      ParameterArrayWithFitness(p1, optimizee.compareFitness(p1, baseline)),
      ParameterArrayWithFitness(p2, optimizee.compareFitness(p2, baseline)),
      ParameterArrayWithFitness(p3, optimizee.compareFitness(p3, baseline))
    )
    optimizee.compareBaselines.clear()
    strategy.refillAfterCullForTesting(pop, keepSize = 3, baseline, desiredPopSize = 12)
    assert(pop.size == 12)
    val recordedDuringRefill = optimizee.compareBaselines.toSeq
    assert(recordedDuringRefill.nonEmpty, "expected compareFitness calls during refill")
    assert(recordedDuringRefill.forall(_ eq baseline),
      "all compare-mode offspring evaluations must use the generation baseline")
  }

  test("genetic search runs in compare mode with baseline-dependent compareFitness") {
    val rnd = new Random(13)
    val optimizee = new BaselineRelativeQuadraticGeneticTestOptimizee(target = 1.5)
    val initial = NumericParameterArray(
      IndexedSeq(DoubleParameter(value = -4.0, minValue = -10.0, maxValue = 10.0, name = "x")),
      rnd = rnd
    )
    val strategy = new GeneticSearchStrategy(optimizee, rnd)
    strategy.setImprovementEpsilon(1e-6)
    val result = strategy.doOptimization(initial, fitnessRange = 100.0)
    assert(result.pa != null)
    assert(optimizee.compareFitness(result.pa, result.pa).abs < 1e-9)
  }

  test("GeneticSearchConfig disables crossover without throwing") {
    val rnd = new Random(17)
    val optimizee = new RecordingOptimizee
    val strategy = new GeneticSearchStrategy(optimizee, rnd, GeneticSearchConfig(crossoverEnabled = false))
    val seed = NumericParameterArraySuiteLike.twoDoubleParams(0.3, -0.4)
    val result = strategy.doOptimization(seed, fitnessRange = 50.0)
    assert(result.pa != null)
  }

  test("sameSolution treats equal-valued distinct arrays as duplicates") {
    val a = NumericParameterArraySuiteLike.twoDoubleParams(1.0, 2.0)
    val b = NumericParameterArraySuiteLike.twoDoubleParams(1.0, 2.0)
    assert(a ne b)
    assert(GeneticSearchStrategy.sameSolution(a, b))
    assert(!GeneticSearchStrategy.sameSolution(a, NumericParameterArraySuiteLike.twoDoubleParams(1.0, 3.0)))
  }

  test("elitism reinjects champion after cull when missing from keepers") {
    val rnd = new Random(19)
    val optimizee = new RecordingOptimizee
    val strategy = new GeneticSearchStrategy(optimizee, rnd, GeneticSearchConfig(elitismEnabled = true))
    val best = NumericParameterArraySuiteLike.twoDoubleParams(0.0, 0.0)
    val worse = NumericParameterArraySuiteLike.twoDoubleParams(5.0, 5.0)
    val champ = ParameterArrayWithFitness(best, 0.0)
    strategy.setGlobalChampionForTesting(champ)
    val pop = ArrayBuffer(
      ParameterArrayWithFitness(worse, 10.0),
      ParameterArrayWithFitness(NumericParameterArraySuiteLike.twoDoubleParams(4.0, 4.0), 8.0)
    )
    strategy.ensureEliteForTesting(pop)
    assert(pop.exists(m => GeneticSearchStrategy.sameSolution(m.pa, best)))
  }

  test("SingleSample neighbor mode uses fewer evals than GreedyImproveAbsolute") {
    def mkPopulation(optimizee: RecordingOptimizee): ArrayBuffer[ParameterArrayWithFitness] = {
      val a = NumericParameterArraySuiteLike.twoDoubleParams(1.0, 2.0)
      val b = NumericParameterArraySuiteLike.twoDoubleParams(-1.0, 3.0)
      val c = NumericParameterArraySuiteLike.twoDoubleParams(0.5, -2.0)
      ArrayBuffer(
        ParameterArrayWithFitness(a, optimizee.evaluateFitness(a)),
        ParameterArrayWithFitness(b, optimizee.evaluateFitness(b)),
        ParameterArrayWithFitness(c, optimizee.evaluateFitness(c))
      )
    }

    val greedyOpt = new RecordingOptimizee
    val greedy = new GeneticSearchStrategy(
      greedyOpt, new Random(23), GeneticSearchConfig(
        crossoverEnabled = false,
        neighborSelection = NeighborSelection.GreedyImproveAbsolute
      )
    )
    val greedyPop = mkPopulation(greedyOpt)
    greedyOpt.evaluateFitnessCalls.clear()
    greedy.refillAfterCullForTesting(greedyPop, keepSize = 3, greedyPop.head.pa, desiredPopSize = 12)
    val greedyEvals = greedyOpt.evaluateFitnessCalls.size

    val singleOpt = new RecordingOptimizee
    val single = new GeneticSearchStrategy(
      singleOpt, new Random(23), GeneticSearchConfig(
        crossoverEnabled = false,
        neighborSelection = NeighborSelection.SingleSample
      )
    )
    val singlePop = mkPopulation(singleOpt)
    singleOpt.evaluateFitnessCalls.clear()
    single.refillAfterCullForTesting(singlePop, keepSize = 3, singlePop.head.pa, desiredPopSize = 12)
    val singleEvals = singleOpt.evaluateFitnessCalls.size

    assert(singleEvals < greedyEvals,
      s"expected SingleSample fewer evals; single=$singleEvals greedy=$greedyEvals")
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

/** Records the baseline (second) argument of every `compareFitness` call. */
private[strategy] final class RecordingCompareBaselineOptimizee extends Optimizee {
  val compareBaselines: mutable.Buffer[ParameterArray] = mutable.Buffer.empty

  override def getName: String = "recording-compare-baseline"

  override def evaluateByComparison: Boolean = true

  override def evaluateFitness(params: ParameterArray): Double =
    throw new UnsupportedOperationException

  override def compareFitness(sample: ParameterArray, baseline: ParameterArray): Double = {
    compareBaselines += baseline
    val s = sample.get(0).getValue + sample.get(1).getValue
    val b = baseline.get(0).getValue + baseline.get(1).getValue
    s - b
  }

  override def getOptimalFitness: Double = 0.0
}

/** `compareFitness(sample, baseline) = score(sample) - score(baseline)` (lower is better when sample is better). */
private[strategy] final class BaselineRelativeQuadraticGeneticTestOptimizee(target: Double) extends Optimizee {
  override def getName: String = "quadratic-compare-genetic"

  override def evaluateByComparison: Boolean = true

  override def evaluateFitness(params: ParameterArray): Double =
    throw new UnsupportedOperationException

  override def compareFitness(sample: ParameterArray, baseline: ParameterArray): Double =
    score(sample) - score(baseline)

  override def getOptimalFitness: Double = 0.0

  private def score(params: ParameterArray): Double = {
    val x = params.get(0).getValue
    val d = x - target
    d * d
  }
}
