package com.barrybecker4.optimization.strategy

import com.barrybecker4.math.MathUtil
import com.barrybecker4.optimization.Optimizer
import com.barrybecker4.optimization.optimizee.{DiscreteStateSpace, LineGraphTestProblem, Optimizee}
import com.barrybecker4.optimization.optimizee.optimizees.DiscreteGrid2DDemoProblem
import com.barrybecker4.optimization.parameter.{NumericParameterArray, ParameterArray}
import com.barrybecker4.optimization.parameter.types.IntegerParameter
import org.scalatest.funsuite.AnyFunSuite

import scala.collection.mutable
import scala.util.Random

class StateSpaceSearchStrategySuite extends AnyFunSuite {

  test("state space search reaches optimal on small line graph") {
    val rnd = new Random(1)
    MathUtil.RANDOM.setSeed(0)
    val problem = new LineGraphTestProblem(target = 7, lo = 0, hi = 20, rnd = rnd)
    val opt = new Optimizer(problem)
    val guess = new NumericParameterArray(IndexedSeq(new IntegerParameter(0, 0, 20, "x")), rnd = rnd)
    val sol = opt.doOptimization(STATE_SPACE_SEARCH, guess, 100.0, rnd).best
    assert(sol.fitness == 0.0)
    assert(sol.pa.get(0).getValue.toInt == 7)
  }

  test("state space search reaches optimal on 2D discrete grid demo") {
    val rnd = new Random(1)
    MathUtil.RANDOM.setSeed(0)
    val problem = new DiscreteGrid2DDemoProblem()
    val opt = new Optimizer(problem)
    val guess = problem.getInitialGuess
    val sol = opt.doOptimization(STATE_SPACE_SEARCH, guess, problem.getFitnessRange, rnd).best
    assert(sol.fitness == 0.0)
    assert(sol.pa.get(0).getValue.toInt == 8)
    assert(sol.pa.get(1).getValue.toInt == 8)
  }

  test("canonical state keys prevent duplicate evaluateFitness on grid") {
    val rnd = new Random(2)
    val base = new DiscreteGrid2DDemoProblem()
    val counting = new CountingDiscreteOptimizee(base)
    val strategy = new StateSpaceSearchStrategy(counting)
    val guess = base.getInitialGuess
    val sol = strategy.doOptimization(guess, base.getFitnessRange)
    assert(sol.fitness == 0.0)
    val keys = counting.evaluatedKeys
    assert(keys.size == counting.evaluateCount,
      s"expected one eval per logical state; keys=${keys.size} evals=${counting.evaluateCount}")
    // 21x21 grid; search should not evaluate every cell, but must stay within state count
    assert(counting.evaluateCount <= 21 * 21)
  }

  test("maxExpansions cap returns best-so-far without requiring optimum") {
    val rnd = new Random(3)
    val problem = new LineGraphTestProblem(target = 0, lo = 0, hi = 500, rnd = rnd)
    val strategy = new StateSpaceSearchStrategy(problem)
    strategy.setMaxExpansions(5)
    val guess = new NumericParameterArray(IndexedSeq(new IntegerParameter(500, 0, 500, "x")), rnd = rnd)
    val initialFitness = problem.evaluateFitness(guess)
    val sol = strategy.doOptimization(guess, fitnessRange = 500.0)
    assert(strategy.lastExpansions <= 5)
    assert(sol.fitness <= initialFitness)
    assert(sol.fitness > 0.0, "with tiny expansion cap should not reach distant optimum")
  }

  test("compare-mode stored fitness matches compareFitness vs baseline") {
    val rnd = new Random(5)
    val problem = new CompareModeLineOptimizee(target = 4, lo = 0, hi = 10, rnd = rnd)
    val strategy = new StateSpaceSearchStrategy(problem)
    val baseline = new NumericParameterArray(IndexedSeq(new IntegerParameter(0, 0, 10, "x")), rnd = rnd)
    val sol = strategy.doOptimization(baseline, fitnessRange = 20.0)
    val expected = problem.compareFitness(sol.pa, baseline)
    assert(math.abs(sol.fitness - expected) < 1e-12)
    assert(sol.pa.get(0).getValue.toInt == 4)
  }

  test("AStar mode reaches optimal and does not expand more than BestFirst on line") {
    val rnd = new Random(7)
    val problem = new LineGraphTestProblem(target = 15, lo = 0, hi = 40, rnd = rnd)
    val guess = new NumericParameterArray(IndexedSeq(new IntegerParameter(0, 0, 40, "x")), rnd = rnd)

    val bestFirst = new StateSpaceSearchStrategy(problem)
    bestFirst.setConfig(StateSpaceSearchConfig(searchMode = StateSpaceSearchMode.BestFirstFitness))
    val r1 = bestFirst.doOptimization(guess, fitnessRange = 40.0)
    val expansionsBf = bestFirst.lastExpansions

    val aStar = new StateSpaceSearchStrategy(problem)
    aStar.setConfig(StateSpaceSearchConfig(searchMode = StateSpaceSearchMode.AStar))
    val r2 = aStar.doOptimization(guess, fitnessRange = 40.0)
    val expansionsA = aStar.lastExpansions

    assert(r1.fitness == 0.0)
    assert(r2.fitness == 0.0)
    assert(expansionsA <= expansionsBf,
      s"A* should not expand more; A*=$expansionsA BestFirst=$expansionsBf")
  }

  test("AStar reaches optimal on 2D grid demo") {
    val rnd = new Random(11)
    MathUtil.RANDOM.setSeed(0)
    val problem = new DiscreteGrid2DDemoProblem()
    val strategy = new StateSpaceSearchStrategy(problem)
    strategy.setConfig(StateSpaceSearchConfig(searchMode = StateSpaceSearchMode.AStar))
    val sol = strategy.doOptimization(problem.getInitialGuess, problem.getFitnessRange)
    assert(sol.fitness == 0.0)
    assert(sol.pa.get(0).getValue.toInt == 8)
    assert(sol.pa.get(1).getValue.toInt == 8)
  }

  test("factory applies StateSpaceSearchConfig defaults") {
    val rnd = new Random(13)
    val problem = new LineGraphTestProblem(target = 3, lo = 0, hi = 10, rnd = rnd)
    val strategy = STATE_SPACE_SEARCH.getStrategy(problem, 10.0, rnd).asInstanceOf[StateSpaceSearchStrategy]
    assert(strategy.getConfig.maxExpansions == StateSpaceSearchConfig.DEFAULT_MAX_EXPANSIONS)
    assert(strategy.getConfig.searchMode == StateSpaceSearchMode.BestFirstFitness)
  }
}

/** Wraps a discrete optimizee and records evaluateFitness keys / counts. */
private final class CountingDiscreteOptimizee(delegate: Optimizee & DiscreteStateSpace)
    extends Optimizee with DiscreteStateSpace {

  val evaluatedKeys: mutable.Set[String] = mutable.LinkedHashSet.empty
  var evaluateCount: Int = 0

  override def getName: String = "counting-" + delegate.getName
  override def evaluateByComparison: Boolean = delegate.evaluateByComparison
  override def getOptimalFitness: Double = delegate.getOptimalFitness

  override def evaluateFitness(params: ParameterArray): Double = {
    evaluateCount += 1
    evaluatedKeys += params.toCSVString
    delegate.evaluateFitness(params)
  }

  override def compareFitness(params1: ParameterArray, params2: ParameterArray): Double =
    delegate.compareFitness(params1, params2)

  override def successors(state: ParameterArray): Seq[ParameterArray] = delegate.successors(state)

  override def heuristic(state: ParameterArray): Double = delegate.heuristic(state)
}

/** compareFitness(sample, baseline) = score(sample) - score(baseline); score = |x - target|. */
private final class CompareModeLineOptimizee(target: Int, lo: Int, hi: Int, rnd: Random)
    extends Optimizee with DiscreteStateSpace {

  override def getName: String = "compare-line"
  override def evaluateByComparison: Boolean = true
  override def getOptimalFitness: Double = 0.0

  override def evaluateFitness(params: ParameterArray): Double =
    throw new UnsupportedOperationException

  override def compareFitness(sample: ParameterArray, baseline: ParameterArray): Double =
    score(sample) - score(baseline)

  override def successors(state: ParameterArray): Seq[ParameterArray] = {
    val v = state.get(0).getValue.toInt
    List(v - 1, v + 1).filter(x => x >= lo && x <= hi).map(npa)
  }

  override def heuristic(state: ParameterArray): Double = 0.0

  private def score(params: ParameterArray): Double =
    math.abs(params.get(0).getValue.toInt - target).toDouble

  private def npa(value: Int): NumericParameterArray =
    new NumericParameterArray(IndexedSeq(new IntegerParameter(value, lo, hi, "x")), rnd = rnd)
}
