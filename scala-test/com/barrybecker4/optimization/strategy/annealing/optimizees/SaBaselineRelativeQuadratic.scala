package com.barrybecker4.optimization.strategy.annealing.optimizees

import com.barrybecker4.optimization.optimizee.Optimizee
import com.barrybecker4.optimization.parameter.ParameterArray

final class SaBaselineRelativeQuadratic(target: Double) extends Optimizee {
  
  override def getName: String = "quadratic-compare-sa-test"
  
  override def evaluateByComparison: Boolean = true
  
  override def evaluateFitness(params: ParameterArray): Double =
    throw new UnsupportedOperationException
    
  override def compareFitness(sample: ParameterArray, baseline: ParameterArray): Double =
    score(sample) - score(baseline)
    
  override def getOptimalFitness: Double = 0.0

  private def score(params: ParameterArray): Double = {
    val x = params.get(0).getValue
    val d = x - target
    d * d
  }
}