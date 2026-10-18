package com.barrybecker4.optimization.strategy.statespace.optimizees

import com.barrybecker4.optimization.optimizee.{DiscreteStateSpace, Optimizee}
import com.barrybecker4.optimization.parameter.{NumericParameterArray, ParameterArray}
import com.barrybecker4.optimization.parameter.types.IntegerParameter

import scala.util.Random

/** compareFitness(sample, baseline) = score(sample) - score(baseline); score = |x - target|. */
final class CompareModeLineOptimizee(target: Int, lo: Int, hi: Int, rnd: Random)
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
