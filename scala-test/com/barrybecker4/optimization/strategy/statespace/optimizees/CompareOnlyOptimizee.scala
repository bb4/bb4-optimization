package com.barrybecker4.optimization.strategy.statespace.optimizees

import com.barrybecker4.optimization.optimizee.Optimizee
import com.barrybecker4.optimization.parameter.ParameterArray

/** Absolute-fitness strategies must refuse this even when the parameters are numeric. */
final class CompareOnlyOptimizee extends Optimizee {
  override def getName: String = "compare-only"
  override def evaluateByComparison: Boolean = true
  override def evaluateFitness(params: ParameterArray): Double = 0.0
  override def compareFitness(params1: ParameterArray, params2: ParameterArray): Double = 0.0
  override def getOptimalFitness: Double = 0.0
}