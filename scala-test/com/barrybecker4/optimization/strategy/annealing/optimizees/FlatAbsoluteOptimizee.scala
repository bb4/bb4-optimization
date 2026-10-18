package com.barrybecker4.optimization.strategy.annealing.optimizees

import com.barrybecker4.optimization.optimizee.Optimizee
import com.barrybecker4.optimization.parameter.ParameterArray

/** Constant positive fitness; Metropolis always accepts equal-fitness moves (exp(0)=1). */
final class FlatAbsoluteOptimizee extends Optimizee {
  
  override def getName: String = "flat-sa-test"
  
  override def evaluateByComparison: Boolean = false
  
  override def evaluateFitness(params: ParameterArray): Double = 1.0
  
  override def compareFitness(params1: ParameterArray, params2: ParameterArray): Double =
    throw new UnsupportedOperationException
    
  override def getOptimalFitness: Double = 0.0
}
