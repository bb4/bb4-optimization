package com.barrybecker4.optimization.strategy

import com.barrybecker4.optimization.optimizee.Optimizee
import com.barrybecker4.optimization.parameter.ParameterArray

/**
  * Compare-fitness landscape with optimum at the identity permutation (value i at index i).
  * Used to exercise `evaluateByComparison` paths in strategies.
  */
private[strategy] final class PermutationIdentityCompareOptimizee extends Optimizee {

  override def getName: String = "perm-compare"

  override def evaluateByComparison: Boolean = true

  override def evaluateFitness(params: ParameterArray): Double =
    throw new UnsupportedOperationException

  override def compareFitness(sample: ParameterArray, baseline: ParameterArray): Double = {
    var sum = 0.0
    for (i <- 0 until sample.size) {
      sum += math.abs(sample.get(i).getValue - i)
    }
    sum
  }

  override def getOptimalFitness: Double = 0.0
}
