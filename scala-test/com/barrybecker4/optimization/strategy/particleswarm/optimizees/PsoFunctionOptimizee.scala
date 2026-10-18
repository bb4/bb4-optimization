package com.barrybecker4.optimization.strategy.particleswarm.optimizees

import com.barrybecker4.optimization.optimizee.Optimizee
import com.barrybecker4.optimization.parameter.ParameterArray

import scala.collection.mutable.ArrayBuffer

/** Wraps a fitness function, counting evaluations and recording every position that was evaluated. */
final class PsoFunctionOptimizee(fitness: ParameterArray => Double, optimal: Double = 0.0) extends Optimizee {

  var evaluations = 0
  val seen: ArrayBuffer[Array[Double]] = ArrayBuffer.empty

  override def getName: String = "pso-test-function"
  
  override def evaluateByComparison: Boolean = false

  override def evaluateFitness(params: ParameterArray): Double = {
    evaluations += 1
    seen += Array.tabulate(params.size)(i => params.get(i).getValue)
    fitness(params)
  }

  override def compareFitness(params1: ParameterArray, params2: ParameterArray): Double =
    throw new UnsupportedOperationException

  override def getOptimalFitness: Double = optimal
}
