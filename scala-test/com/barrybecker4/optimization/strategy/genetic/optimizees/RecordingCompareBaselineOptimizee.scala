package com.barrybecker4.optimization.strategy.genetic.optimizees

import com.barrybecker4.optimization.optimizee.Optimizee
import com.barrybecker4.optimization.parameter.ParameterArray

import scala.collection.mutable


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