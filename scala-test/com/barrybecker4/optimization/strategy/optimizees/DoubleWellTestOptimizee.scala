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

/** min((x−3)²+y², (x+3)²+y²) with minima at (3, 0) and (−3, 0). */
final class TwoMinima2DOptimizee extends Optimizee {
  override def getName: String = "two-minima-2d"
  override def evaluateByComparison: Boolean = false
  override def evaluateFitness(params: ParameterArray): Double = {
    val x = params.get(0).getValue
    val y = params.get(1).getValue
    val d1 = (x - 3.0) * (x - 3.0) + y * y
    val d2 = (x + 3.0) * (x + 3.0) + y * y
    math.min(d1, d2)
  }
  override def compareFitness(params1: ParameterArray, params2: ParameterArray): Double =
    throw new UnsupportedOperationException
  override def getOptimalFitness: Double = 0.0
}
