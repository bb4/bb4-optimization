package com.barrybecker4.optimization.strategy.genetic.optimizees

import com.barrybecker4.optimization.optimizee.Optimizee
import com.barrybecker4.optimization.parameter.ParameterArray

import scala.collection.mutable


private[strategy] final class RecordingOptimizee extends Optimizee {
  
  val evaluateFitnessCalls: mutable.Buffer[ParameterArray] = mutable.Buffer.empty

  override def getName: String = "recording"

  override def evaluateByComparison: Boolean = false

  override def evaluateFitness(params: ParameterArray): Double = {
    evaluateFitnessCalls += params
    params.get(0).getValue + params.get(1).getValue
  }

  override def compareFitness(params1: ParameterArray, params2: ParameterArray): Double =
    throw new UnsupportedOperationException

  override def getOptimalFitness: Double = 0.0
}
