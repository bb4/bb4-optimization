package com.barrybecker4.optimization.strategy.optimizees

import com.barrybecker4.optimization.optimizee.Optimizee
import com.barrybecker4.optimization.parameter.ParameterArray

final class FlatLandscapeOptimizee extends Optimizee {
  
  override def getName: String = "flat"
  override def evaluateByComparison: Boolean = false
  
  override def evaluateFitness(params: ParameterArray): Double = 1.0
  
  override def compareFitness(params1: ParameterArray, params2: ParameterArray): Double =
    throw new UnsupportedOperationException
    
  override def getOptimalFitness: Double = 0.0
}
