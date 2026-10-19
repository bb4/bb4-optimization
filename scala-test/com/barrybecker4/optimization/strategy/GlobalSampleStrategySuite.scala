package com.barrybecker4.optimization.strategy

import com.barrybecker4.optimization.optimizee.{BudgetedOptimizee, EvaluationBudgetExceededException}
import com.barrybecker4.optimization.parameter.NumericParameterArray
import com.barrybecker4.optimization.parameter.types.DoubleParameter
import com.barrybecker4.optimization.strategy.hillclimbing.SeedSelection
import com.barrybecker4.optimization.strategy.hillclimbing.optimizees.BaselineRelativeQuadraticCompareOptimizee
import com.barrybecker4.optimization.strategy.optimizees.FlatLandscapeOptimizee
import com.barrybecker4.optimization.strategy.particleswarm.optimizees.PsoFunctionOptimizee
import org.scalatest.funsuite.AnyFunSuite

import scala.util.Random

class GlobalSampleStrategySuite extends AnyFunSuite {

  test("diverse topK selects well-separated seeds on a flat landscape") {
    val rnd = new Random(42)
    val optimizee = new FlatLandscapeOptimizee
    
    val initial = NumericParameterArray(
      IndexedSeq(
        DoubleParameter(value = 0.0, minValue = -10.0, maxValue = 10.0, name = "x"),
        DoubleParameter(value = 0.0, minValue = -10.0, maxValue = 10.0, name = "y")
      ),
      rnd = rnd
    )
    val gs = new GlobalSampleStrategy(optimizee)
    
    gs.setSamplingRate(100)

    val diverse = gs.doOptimizationTopK(
      initial,
      fitnessRange = 100.0,
      k = 2,
      SeedSelection.DiverseFromTopM(topM = 50, minSeparation = 0.15)
    )

    assert(diverse.size == 2)
    val xs = diverse.map(_.pa.get(0).getValue)
    val ys = diverse.map(_.pa.get(1).getValue)
    val spread = diverse.head.pa.distance(diverse(1).pa)
    assert(spread > 8.0, s"expected well-separated seeds, distance=$spread xs=$xs ys=$ys")
  }

  private def oneDim(rnd: Random = new Random(1)) = NumericParameterArray(
    IndexedSeq(DoubleParameter(value = 0.0, minValue = -10.0, maxValue = 10.0, name = "x")), rnd = rnd)

  private def square(p: com.barrybecker4.optimization.parameter.ParameterArray) = {
    val x = p.get(0).getValue
    x * x
  }

  test("TopFitness returns the k best samples, best first") {
    val optimizee = new PsoFunctionOptimizee(square)
    val gs = new GlobalSampleStrategy(optimizee)
    gs.setSamplingRate(21)

    val seeds = gs.doOptimizationTopK(oneDim(), 10.0, k = 4, SeedSelection.TopFitness)

    val all = optimizee.seen.map(p => p(0) * p(0)).sorted
    assert(seeds.map(_.fitness) == all.take(4), s"expected ${all.take(4)}, got ${seeds.map(_.fitness)}")
  }

  test("asking for more seeds than there are samples returns every sample once") {
    val optimizee = new PsoFunctionOptimizee(square)
    val gs = new GlobalSampleStrategy(optimizee)
    gs.setSamplingRate(5)

    val seeds = gs.doOptimizationTopK(oneDim(), 10.0, k = 50, SeedSelection.TopFitness)

    assert(seeds.size == optimizee.evaluations)
    assert(seeds.map(_.pa).distinct.size == seeds.size)
  }

  test("doOptimization returns the best sample and notifies the listener of each improvement") {
    val optimizee = new PsoFunctionOptimizee(square)
    val gs = new GlobalSampleStrategy(optimizee)
    gs.setSamplingRate(11)
    val reported = scala.collection.mutable.ArrayBuffer.empty[Double]
    gs.setListener(new com.barrybecker4.optimization.OptimizationListener {
      override def optimizerChanged(params: com.barrybecker4.optimization.parameter.ParameterArrayWithFitness): Unit =
        reported += params.fitness
    })

    val best = gs.doOptimization(oneDim(), 10.0)

    assert(best.fitness == optimizee.seen.map(p => p(0) * p(0)).min)
    assert(reported.nonEmpty && reported.zip(reported.tail).forall { case (a, b) => b < a })
    assert(reported.last == best.fitness)
  }

  test("sampling stops early once the optimal fitness is found") {
    val optimizee = new PsoFunctionOptimizee(square, optimal = 1000.0) // everything is good enough
    val gs = new GlobalSampleStrategy(optimizee)
    gs.setSamplingRate(50)

    gs.doOptimization(oneDim(), 10.0)

    assert(optimizee.evaluations == 1)
  }

  test("a NaN fitness is never selected and does not stop better samples from being kept") {
    // NaN for the first few samples, which fill the reservoir before anything finite arrives
    var calls = 0
    val optimizee = new PsoFunctionOptimizee({ p =>
      calls += 1
      if (calls <= 3) Double.NaN else square(p)
    })
    val gs = new GlobalSampleStrategy(optimizee)
    gs.setSamplingRate(21)

    val seeds = gs.doOptimizationTopK(oneDim(), 10.0, k = 3, SeedSelection.TopFitness)

    val finite = optimizee.seen.drop(3).map(p => p(0) * p(0)).sorted
    assert(seeds.map(_.fitness) == finite.take(3), s"got ${seeds.map(_.fitness)}")
  }

  test("in comparison mode samples are scored against the initial guess") {
    val optimizee = new BaselineRelativeQuadraticCompareOptimizee(target = 1.0)
    val initial = oneDim()
    val gs = new GlobalSampleStrategy(optimizee)
    gs.setSamplingRate(21)

    val best = gs.doOptimization(initial, 10.0)

    assert(best.fitness == optimizee.compareFitness(best.pa, initial))
    assert(best.fitness < 0.0)
  }

  test("the best sample seen so far is kept when the budget runs out") {
    val inner = new PsoFunctionOptimizee(square)
    val budgeted = new BudgetedOptimizee(inner, maxEvaluations = 4)
    val gs = new GlobalSampleStrategy(budgeted)
    gs.setSamplingRate(21)

    intercept[EvaluationBudgetExceededException] { gs.doOptimization(oneDim(), 10.0) }

    assert(gs.bestSoFar.map(_.fitness).contains(inner.seen.map(p => p(0) * p(0)).min))
  }

  test("a sampling rate that is not positive is rejected") {
    val gs = new GlobalSampleStrategy(new FlatLandscapeOptimizee)
    intercept[IllegalArgumentException] { gs.setSamplingRate(0) }
    intercept[IllegalArgumentException] { gs.setSamplingRate(-3) }
  }

  test("diverse selection keeps the global best first and respects the requested separation when possible") {
    val optimizee = new PsoFunctionOptimizee(p => math.abs(p.get(0).getValue - 3.0))
    val gs = new GlobalSampleStrategy(optimizee)
    gs.setSamplingRate(41)

    val seeds = gs.doOptimizationTopK(oneDim(), 20.0, k = 3, SeedSelection.DiverseFromTopM(topM = 41, minSeparation = 0.2))

    assert(seeds.size == 3)
    assert(seeds.head.fitness == seeds.map(_.fitness).min, "the global best leads")
    for (a <- seeds.indices; b <- a + 1 until seeds.size)
      assert(seeds(a).pa.distance(seeds(b).pa) > 2.0, s"seeds $a and $b are too close")
  }
}
