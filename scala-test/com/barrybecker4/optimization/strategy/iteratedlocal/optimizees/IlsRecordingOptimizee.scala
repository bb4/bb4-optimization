package com.barrybecker4.optimization.strategy.iteratedlocal.optimizees

import com.barrybecker4.optimization.optimizee.Optimizee
import com.barrybecker4.optimization.parameter.ParameterArray

import scala.collection.mutable.ArrayBuffer

/** Wraps a fitness function and records the values of every solution that was evaluated, in order. */
final class IlsRecordingOptimizee(fitness: ParameterArray => Double, optimal: Double = 0.0) extends Optimizee {

  val evaluated: ArrayBuffer[Vector[Double]] = ArrayBuffer.empty

  def evaluations: Int = evaluated.size

  override def getName: String = "recording"
  override def evaluateByComparison: Boolean = false

  override def evaluateFitness(params: ParameterArray): Double = {
    evaluated += Vector.tabulate(params.size)(i => params.get(i).getValue)
    fitness(params)
  }

  override def compareFitness(params1: ParameterArray, params2: ParameterArray): Double =
    throw new UnsupportedOperationException

  override def getOptimalFitness: Double = optimal
}
