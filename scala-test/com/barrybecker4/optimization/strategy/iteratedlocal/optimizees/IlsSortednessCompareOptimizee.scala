package com.barrybecker4.optimization.strategy.iteratedlocal.optimizees

import com.barrybecker4.optimization.optimizee.Optimizee
import com.barrybecker4.optimization.parameter.ParameterArray

/** Compare mode: the score is how far each value is from its index, and the result is a score difference. */
final class IlsSortednessCompareOptimizee extends Optimizee {

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
