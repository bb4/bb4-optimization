package com.barrybecker4.optimization.strategy.optimizees

import com.barrybecker4.optimization.optimizee.Optimizee
import com.barrybecker4.optimization.parameter.ParameterArray

/** (x² − 9)² with equal minima at x = ±3. */
final class DoubleWellOptimizee extends Optimizee {

  override def getName: String = "double-well"

  override def evaluateByComparison: Boolean = false

  override def evaluateFitness(params: ParameterArray): Double = {
    val x = params.get(0).getValue
    val d = x * x - 9.0
    d * d
  }

  override def compareFitness(params1: ParameterArray, params2: ParameterArray): Double =
    throw new UnsupportedOperationException

  override def getOptimalFitness: Double = 0.0
}
