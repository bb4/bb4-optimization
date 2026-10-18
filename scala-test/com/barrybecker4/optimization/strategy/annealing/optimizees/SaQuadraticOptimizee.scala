package com.barrybecker4.optimization.strategy.annealing.optimizees

import com.barrybecker4.optimization.optimizee.Optimizee
import com.barrybecker4.optimization.parameter.ParameterArray


final class SaQuadraticOptimizee(target: Double) extends Optimizee {
  
  override def getName: String = "quadratic-sa-test"
  
  override def evaluateByComparison: Boolean = false
  
  override def evaluateFitness(params: ParameterArray): Double = {
    val x = params.get(0).getValue
    val d = x - target
    d * d
  }
  
  override def compareFitness(params1: ParameterArray, params2: ParameterArray): Double =
    throw new UnsupportedOperationException
    
  override def getOptimalFitness: Double = 0.0
}
