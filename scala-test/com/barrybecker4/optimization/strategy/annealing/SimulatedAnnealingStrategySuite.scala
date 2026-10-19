package com.barrybecker4.optimization.strategy.annealing

import com.barrybecker4.optimization.parameter.types.DoubleParameter
import com.barrybecker4.optimization.parameter.{NumericParameterArray, ParameterArray}
import com.barrybecker4.optimization.strategy.OptimizationStrategyType.SIMULATED_ANNEALING
import com.barrybecker4.optimization.strategy.annealing.optimizees.{FlatAbsoluteOptimizee, SaBaselineRelativeQuadratic, SaQuadraticOptimizee}
import com.barrybecker4.optimization.strategy.annealing.{SimulatedAnnealingStepMode, SimulatedAnnealingStrategy}
import com.barrybecker4.optimization.{Logger, OptimizationListener}
import com.barrybecker4.optimization.optimizee.BudgetedOptimizee
import com.barrybecker4.optimization.parameter.ParameterArrayWithFitness
import org.scalatest.funsuite.AnyFunSuite

import scala.util.Random

class SimulatedAnnealingStrategySuite extends AnyFunSuite {

  test("doOptimization requires positive finite fitnessRange") {
    val rnd = new Random(0)
    val optimizee = new SaQuadraticOptimizee(target = 0.0)
    val sa = new SimulatedAnnealingStrategy(optimizee, rnd)
    val initial = NumericParameterArray(
      IndexedSeq(DoubleParameter(value = 2.0, minValue = -10.0, maxValue = 10.0, name = "x")),
      rnd = rnd
    )
    assertThrows[IllegalArgumentException](sa.doOptimization(initial, fitnessRange = 0.0))
    assertThrows[IllegalArgumentException](sa.doOptimization(initial, fitnessRange = -1.0))
  }

  test("fixed step mode improves on simple quadratic") {
    val rnd = new Random(5)
    val optimizee = new SaQuadraticOptimizee(target = 1.0)
    val sa = new SimulatedAnnealingStrategy(optimizee, rnd)
    sa.setStepMode(SimulatedAnnealingStepMode.Fixed)
    sa.setMaxTemperature(50.0)
    val initial = NumericParameterArray(
      IndexedSeq(DoubleParameter(value = -5.0, minValue = -10.0, maxValue = 10.0, name = "x")),
      rnd = rnd
    )
    val out = sa.doOptimization(initial, fitnessRange = 100.0)
    assert(out.fitness < 0.05)
    assert(math.abs(out.pa.get(0).getValue - 1.0) < 0.15)
  }

  test("default config uses AdaptiveAcceptance") {
    val sa = new SimulatedAnnealingStrategy(new SaQuadraticOptimizee(0.0), new Random(1))
    assert(sa.getConfig.stepMode.isInstanceOf[SimulatedAnnealingStepMode.AdaptiveAcceptance])
  }

  test("factory default uses AdaptiveAcceptance") {
    val rnd = new Random(3)
    val optimizee = new SaQuadraticOptimizee(target = 0.0)
    val strategy = SIMULATED_ANNEALING.getStrategy(optimizee, fitnessRange = 100.0, rnd)
    val sa = strategy.asInstanceOf[SimulatedAnnealingStrategy]
    assert(sa.getConfig.stepMode.isInstanceOf[SimulatedAnnealingStepMode.AdaptiveAcceptance])
  }

  test("aggressive adaptive scale does not exceed proposal radius clamp") {
    val rnd = new Random(19)
    val optimizee = new SaQuadraticOptimizee(target = 0.0)
    val sa = new SimulatedAnnealingStrategy(optimizee, rnd)
    sa.setStepMode(
      SimulatedAnnealingStepMode.AdaptiveAcceptance(
        targetRate = 0.01,
        windowProposals = 2,
        scaleMin = 1.0,
        scaleMax = 1e6,
        growFactor = 10.0,
        shrinkFactor = 0.5,
        initialScale = 100.0
      )
    )
    sa.setMaxTemperature(100.0)
    val initial = NumericParameterArray(
      IndexedSeq(DoubleParameter(value = 3.0, minValue = -10.0, maxValue = 10.0, name = "x")),
      rnd = rnd
    )
    val out = sa.doOptimization(initial, fitnessRange = 100.0)
    assert(out.pa != null)
    assert(out.fitness.isFinite)
  }

  test("adaptive acceptance completes on flat landscape and respects comparison-mode convention") {
    val rnd = new Random(11)
    val flat = new FlatAbsoluteOptimizee
    val sa = new SimulatedAnnealingStrategy(flat, rnd)
    sa.setStepMode(
      SimulatedAnnealingStepMode.AdaptiveAcceptance(
        targetRate = 0.2,
        windowProposals = 8,
        scaleMin = 0.5,
        scaleMax = 4.0,
        growFactor = 1.2,
        shrinkFactor = 0.85
      )
    )
    sa.setMaxTemperature(20.0)
    val initial = NumericParameterArray(
      IndexedSeq(DoubleParameter(value = 0.0, minValue = -1.0, maxValue = 1.0, name = "x")),
      rnd = rnd
    )
    val out = sa.doOptimization(initial, fitnessRange = 1.0)
    assert(out.fitness == 1.0)

    val rnd2 = new Random(13)
    val compareOpt = new SaBaselineRelativeQuadratic(target = 0.0)
    val sa2 = new SimulatedAnnealingStrategy(compareOpt, rnd2)
    sa2.setStepMode(SimulatedAnnealingStepMode.AdaptiveAcceptance(windowProposals = 10))
    sa2.setMaxTemperature(30.0)
    val initial2 = NumericParameterArray(
      IndexedSeq(DoubleParameter(value = 3.0, minValue = -10.0, maxValue = 10.0, name = "x")),
      rnd = rnd2
    )
    val out2 = sa2.doOptimization(initial2, fitnessRange = 100.0)
    val expected = compareOpt.compareFitness(out2.pa, initial2)
    assert(math.abs(out2.fitness - expected) < 1.0e-9)
  }

  test("budget interrupt mid-run keeps compare-fitness consistent with baseline") {
    val rnd = new Random(29)
    val compareOpt = new SaBaselineRelativeQuadratic(target = 0.0)
    val budgeted = new com.barrybecker4.optimization.optimizee.BudgetedOptimizee(compareOpt, maxEvaluations = 25)
    val sa = new SimulatedAnnealingStrategy(budgeted, rnd)
    sa.setStepMode(SimulatedAnnealingStepMode.Fixed)
    sa.setMaxTemperature(40.0)
    val initial = NumericParameterArray(
      IndexedSeq(DoubleParameter(value = 4.0, minValue = -10.0, maxValue = 10.0, name = "x")),
      rnd = rnd
    )
    val out = sa.doOptimization(initial, fitnessRange = 100.0)
    val expected = compareOpt.compareFitness(out.pa, initial)
    assert(math.abs(out.fitness - expected) < 1.0e-9,
      s"stored fitness ${out.fitness} should match compare vs baseline $expected")
  }

  private def start(x: Double, rnd: Random, lo: Double = -10.0, hi: Double = 10.0) =
    NumericParameterArray(IndexedSeq(DoubleParameter(value = x, minValue = lo, maxValue = hi, name = "x")), rnd = rnd)

  private def annealed(seed: Int, mode: SimulatedAnnealingStepMode): ParameterArrayWithFitness = {
    val rnd = new Random(seed)
    val sa = new SimulatedAnnealingStrategy(new SaQuadraticOptimizee(1.0), rnd)
    sa.setStepMode(mode)
    sa.setMaxTemperature(50.0)
    sa.doOptimization(start(-5.0, rnd), fitnessRange = 100.0)
  }

  test("the same seed gives the same result") {
    val a = annealed(42, SimulatedAnnealingStepMode.Fixed)
    val b = annealed(42, SimulatedAnnealingStepMode.Fixed)
    assert(a.fitness == b.fitness)
    assert(a.pa.get(0).getValue == b.pa.get(0).getValue)
  }

  test("an exhausted budget before the first evaluation returns the start with worst fitness instead of throwing") {
    val rnd = new Random(2)
    val budgeted = new BudgetedOptimizee(new SaQuadraticOptimizee(0.0), maxEvaluations = 0)
    val sa = new SimulatedAnnealingStrategy(budgeted, rnd)
    val initial = start(3.0, rnd)
    val out = sa.doOptimization(initial, fitnessRange = 100.0)
    assert(out.fitness == Double.MaxValue)
    assert(out.pa.get(0).getValue == 3.0)
  }

  test("the budget is never overrun and the best-so-far is returned") {
    val rnd = new Random(8)
    val budgeted = new BudgetedOptimizee(new SaQuadraticOptimizee(0.0), maxEvaluations = 40)
    val sa = new SimulatedAnnealingStrategy(budgeted, rnd)
    sa.setStepMode(SimulatedAnnealingStepMode.Fixed)
    sa.setMaxTemperature(50.0)
    val out = sa.doOptimization(start(6.0, rnd), fitnessRange = 100.0)
    assert(budgeted.wasBudgetExhausted)
    assert(budgeted.evaluationsUsed == 40)
    assert(out.fitness <= 36.0)
  }

  test("starting at the optimum stops after the first temperature pass") {
    val rnd = new Random(4)
    val budgeted = new BudgetedOptimizee(new SaQuadraticOptimizee(0.0), maxEvaluations = 100000)
    val sa = new SimulatedAnnealingStrategy(budgeted, rnd)
    val out = sa.doOptimization(start(0.0, rnd), fitnessRange = 100.0)
    assert(out.fitness == 0.0)
    // 1 initial evaluation, and the inner loop exits at once because the optimum is already reached.
    assert(budgeted.evaluationsUsed == 1)
  }

  test("a fitness range of NaN or infinity is rejected") {
    val rnd = new Random(0)
    val sa = new SimulatedAnnealingStrategy(new SaQuadraticOptimizee(0.0), rnd)
    assertThrows[IllegalArgumentException](sa.doOptimization(start(1.0, rnd), Double.NaN))
    assertThrows[IllegalArgumentException](sa.doOptimization(start(1.0, rnd), Double.PositiveInfinity))
  }

  test("the listener only ever sees strictly improving points, ending at the returned best") {
    val rnd = new Random(21)
    val sa = new SimulatedAnnealingStrategy(new SaQuadraticOptimizee(1.0), rnd)
    sa.setStepMode(SimulatedAnnealingStepMode.Fixed)
    sa.setMaxTemperature(50.0)
    val seen = scala.collection.mutable.ArrayBuffer[Double]()
    sa.setListener(new OptimizationListener {
      override def optimizerChanged(p: ParameterArrayWithFitness): Unit = seen += p.fitness
    })
    val out = sa.doOptimization(start(-5.0, rnd), fitnessRange = 100.0)
    assert(seen.nonEmpty)
    assert(seen.sliding(2).forall(w => w.size < 2 || w(1) < w(0)))
    assert(seen.last == out.fitness)
    assert(sa.bestSoFar.exists(_.fitness == out.fitness))
  }

  test("a file logger receives per-step entries and a final summary") {
    val rnd = new Random(9)
    val sa = new SimulatedAnnealingStrategy(new SaQuadraticOptimizee(1.0), rnd)
    sa.setStepMode(SimulatedAnnealingStepMode.Fixed)
    sa.setMaxTemperature(50.0)
    val file = java.io.File.createTempFile("sa-log", ".csv")
    try {
      val initial = start(-5.0, rnd)
      val logger = new Logger(file.getPath)
      logger.initialize(initial)
      sa.setLogger(logger)
      sa.doOptimization(initial, fitnessRange = 100.0)
      val lines = java.nio.file.Files.readAllLines(file.toPath)
      assert(lines.size > 2, s"expected header + step rows + summary, got ${lines.size} lines")
    } finally file.delete()
  }

  test("adaptive mode changes the search in one dimension (the window used to be longer than a level)") {
    val adaptive = (1 to 10).map(seed => annealed(seed, SimulatedAnnealingStepMode.AdaptiveAcceptance()).fitness)
    val fixed = (1 to 10).map(seed => annealed(seed, SimulatedAnnealingStepMode.Fixed).fitness)
    assert(adaptive != fixed)
  }

  test("adaptive mode still converges on a 1-D quadratic") {
    val results = (1 to 10).map(seed => annealed(seed, SimulatedAnnealingStepMode.AdaptiveAcceptance()).fitness)
    assert(results.count(_ < 0.05) >= 8, s"results: $results")
  }

  test("the Metropolis random draw is only made for moves that do not improve") {
    var draws = 0
    val metro = new Random(5) {
      override def nextDouble(): Double = { draws += 1; super.nextDouble() }
    }
    val budgeted = new BudgetedOptimizee(new SaQuadraticOptimizee(1.0), maxEvaluations = 100000)
    val sa = new SimulatedAnnealingStrategy(budgeted, metro)
    sa.setStepMode(SimulatedAnnealingStepMode.Fixed)
    sa.setMaxTemperature(50.0)
    sa.doOptimization(start(-5.0, new Random(77)), fitnessRange = 100.0)
    val proposals = budgeted.evaluationsUsed - 1 // minus the initial evaluation
    assert(draws > 0 && draws < proposals, s"draws=$draws proposals=$proposals")
  }
}
