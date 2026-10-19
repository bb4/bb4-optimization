package com.barrybecker4.optimization.strategy.genetic

import com.barrybecker4.math.MathUtil
import com.barrybecker4.optimization.optimizee.{BudgetedOptimizee, Optimizee}
import com.barrybecker4.optimization.parameter.types.{DoubleParameter, IntegerParameter}
import com.barrybecker4.optimization.parameter.{NumericParameterArray, ParameterArray, PermutedParameterArray}
import com.barrybecker4.optimization.strategy.genetic.{ConcurrentGeneticSearchStrategy, GeneticSearchConfig, GeneticSearchStrategy, NeighborSelection}
import com.barrybecker4.optimization.strategy.genetic.optimizees.BaselineRelativeQuadraticGeneticTestOptimizee
import com.barrybecker4.optimization.strategy.optimizees.CompareModeTestOptimizee
import org.scalatest.funsuite.AnyFunSuite

import java.util.concurrent.atomic.AtomicInteger
import scala.util.Random

class ConcurrentGeneticSearchStrategySuite extends AnyFunSuite {

  test("concurrent genetic completes in compare mode on tiny permutation space") {
    MathUtil.RANDOM.setSeed(42)
    val rnd = new Random(42)
    val optimizee = new CompareModeTestOptimizee
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

  test("concurrent evaluateOffspring path completes with thread-safe absolute optimizee") {
    val rnd = new Random(31)
    val optimizee = new CountingAbsoluteQuadraticOptimizee(target = 1.0)
    val seed = NumericParameterArray(
      IndexedSeq(DoubleParameter(value = -4.0, minValue = -10.0, maxValue = 10.0, name = "x")),
      rnd = rnd
    )
    val strategy = new ConcurrentGeneticSearchStrategy(
      optimizee,
      rnd,
      GeneticSearchConfig(
        neighborSelection = NeighborSelection.SingleSample,
        maxIterations = 8,
        maxStagnantGenerations = 3
      )
    )
    strategy.setImprovementEpsilon(1e-4)
    val result = strategy.doOptimization(seed, fitnessRange = 100.0)
    assert(optimizee.evals.get() > 1)
    assert(result.fitness < 16.0)
  }

  test("concurrent and sequential genetic reach similar fitness on seeded quadratic") {
    val seedParams = IndexedSeq(DoubleParameter(value = -4.0, minValue = -10.0, maxValue = 10.0, name = "x"))
    val cfg = GeneticSearchConfig(
      neighborSelection = NeighborSelection.SingleSample,
      maxIterations = 12,
      maxStagnantGenerations = 4,
      elitismEnabled = true
    )

    val seqRnd = new Random(41)
    val seqOpt = new UnimodalQuadraticGeneticOptimizee(target = 1.5)
    val seq = new GeneticSearchStrategy(seqOpt, seqRnd, cfg)
    seq.setImprovementEpsilon(1e-5)
    val seqResult = seq.doOptimization(NumericParameterArray(seedParams, rnd = seqRnd), 100.0)

    val parRnd = new Random(41)
    val parOpt = new UnimodalQuadraticGeneticOptimizee(target = 1.5)
    val par = new ConcurrentGeneticSearchStrategy(parOpt, parRnd, cfg)
    par.setImprovementEpsilon(1e-5)
    val parResult = par.doOptimization(NumericParameterArray(seedParams, rnd = parRnd), 100.0)

    assert(math.abs(seqResult.fitness - parResult.fitness) < 0.5,
      s"expected similar fitness; seq=${seqResult.fitness} par=${parResult.fitness}")
  }

  test("concurrent compare mode converges to the optimum") {
    val rnd = new Random(13)
    val optimizee = new BaselineRelativeQuadraticGeneticTestOptimizee(target = 1.5)
    val strategy = new ConcurrentGeneticSearchStrategy(optimizee, rnd)
    strategy.setImprovementEpsilon(1e-6)
    val initial = NumericParameterArray(
      IndexedSeq(DoubleParameter(value = -4.0, minValue = -10.0, maxValue = 10.0, name = "x")), rnd = rnd)
    val result = strategy.doOptimization(initial, fitnessRange = 100.0)
    assert(math.abs(result.pa.get(0).getValue - 1.5) < 0.25)
    assert(math.abs(result.fitness - optimizee.compareFitness(result.pa, initial)) < 1e-9)
  }

  test("concurrent run with a budget never overshoots it and returns the best candidate evaluated") {
    for (seed <- 1 to 5) {
      val rnd = new Random(seed)
      val inner = new CountingAbsoluteQuadraticOptimizee(target = 1.0)
      val budgeted = new BudgetedOptimizee(inner, maxEvaluations = 40)
      val strategy = new ConcurrentGeneticSearchStrategy(
        budgeted, rnd, GeneticSearchConfig(neighborSelection = NeighborSelection.SingleSample))
      val initial = NumericParameterArray(
        IndexedSeq(DoubleParameter(value = -4.0, minValue = -10.0, maxValue = 10.0, name = "x")), rnd = rnd)
      val result = strategy.doOptimization(initial, fitnessRange = 100.0)
      assert(budgeted.wasBudgetExhausted)
      assert(budgeted.evaluationsUsed == 40)
      assert(inner.evals.get() == 40, "the budget must be exact even when evaluated from several threads")
      assert(result.fitness == inner.minSeen, s"seed $seed: returned ${result.fitness}, best seen ${inner.minSeen}")
    }
  }
}

private final class CountingAbsoluteQuadraticOptimizee(target: Double) extends Optimizee {
  val evals = new AtomicInteger(0)
  private val minBits = new java.util.concurrent.atomic.AtomicLong(java.lang.Double.doubleToLongBits(Double.MaxValue))
  /** The lowest fitness evaluated so far (thread safe). */
  def minSeen: Double = java.lang.Double.longBitsToDouble(minBits.get())
  override def getName: String = "counting-quadratic"
  override def evaluateByComparison: Boolean = false
  override def evaluateFitness(params: ParameterArray): Double = {
    evals.incrementAndGet()
    val x = params.get(0).getValue
    val d = x - target
    val f = d * d
    minBits.updateAndGet(bits => if (f < java.lang.Double.longBitsToDouble(bits)) java.lang.Double.doubleToLongBits(f) else bits)
    f
  }
  override def compareFitness(params1: ParameterArray, params2: ParameterArray): Double =
    throw new UnsupportedOperationException
  override def getOptimalFitness: Double = 0.0
}

private final class UnimodalQuadraticGeneticOptimizee(target: Double) extends Optimizee {
  override def getName: String = "unimodal-quadratic-genetic"
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
